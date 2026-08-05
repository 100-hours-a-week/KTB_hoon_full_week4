# CI/CD 파이프라인 (Docker 기반 배포)

GitHub Actions 로 백엔드(Spring Boot)를 자동 빌드·배포하는 과정 정리.
main 브랜치 push 만 사용하며(PR 없음), 이미지는 GHCR, 배포는 EC2 의 docker compose 로 이루어진다.

## 전체 그림

```
git push (main)
      │
      ▼
┌──────────────┐   성공 시    ┌───────────────────────────────────────────┐
│   CI         │ ──────────▶ │   CD (workflow_run 으로 자동 이어짐)          │
│ ci.yml       │             │ cd.yml                                       │
│ - spotless   │             │ - checkout(CI 통과한 SHA)                     │
│ - build+test │             │ - GHCR 로그인 → 이미지 build & push           │
└──────────────┘             │ - EC2 SSH → docker compose up -d backend      │
                             └───────────────────────────────────────────┘
                                              │
                                              ▼
                    EC2 ~/app/docker-compose.yml (nginx + frontend + backend)
                    - nginx :80 → /api/* : backend:8080 / 그 외 : frontend:80
```

## 서버 구조 (EC2)

```
사용자 브라우저
      │ :80
      ▼
nginx (리버스 프록시 컨테이너)
      ├── /api/*  ──▶ backend:8080   (Spring Boot)
      └── 그 외   ──▶ frontend:80    (React 정적파일 + nginx)
```

- `~/app/docker-compose.yml` 로 **nginx + frontend + backend 3개 컨테이너**를 함께 운영.
- 외부에 공개되는 포트는 nginx 의 `80:80` 뿐. backend/frontend 는 `expose` 로 내부 네트워크에서만 접근.
- 이 파이프라인은 이 중 **backend 서비스 하나만** 새 이미지로 교체한다. nginx·frontend 는 건드리지 않음.
- frontend 이미지(`ghcr.io/100-hours-a-week/ktb_hoon_full_week7`)는 **별도 레포에서 빌드·배포**된다.

## 1. CI — `.github/workflows/ci.yml`

- **트리거**: `push` → `main`
- **스텝**
  1. checkout
  2. JDK 17 (temurin) 설정
  3. Gradle 설정 (의존성 캐시)
  4. `./gradlew spotlessCheck` — 포맷 위반이면 여기서 먼저 실패 (원인 명확화)
  5. `./gradlew build` — 컴파일 + 전체 테스트 (spotless 도 내부 포함)

CI 가 성공해야만 CD 가 이어진다.

## 2. CD — `.github/workflows/cd.yml`

- **트리거**: `workflow_run` — CI("CI") 가 `main` 에서 `conclusion == 'success'` 로 끝났을 때만 실행
  - `if: github.event.workflow_run.conclusion == 'success'` 로 CI 실패 시 스킵.
- **concurrency**: `group: cd-main`, `cancel-in-progress: true`
  - 연속 push 시 이전 배포를 취소하고 최신 커밋만 배포 (오래된 커밋 배포 방지).
- **권한**: `contents: read`, `packages: write` (GHCR push 용)
- **스텝**
  1. checkout — `ref: workflow_run.head_sha` (CI 가 통과한 그 커밋)
  2. 이미지명 계산 — `ghcr.io/<owner>/<repo>` 를 소문자로
     (GHCR 이미지명은 소문자만 허용)
  3. Buildx 설정
  4. GHCR 로그인 — `GITHUB_TOKEN` 사용 (별도 시크릿 불필요)
  5. **이미지 build & push** (`docker/build-push-action`)
     - 태그 2개: `:latest`, `:<커밋 SHA>`
     - GitHub Actions 캐시(`type=gha`) 사용
  6. **EC2 SSH 배포** (`appleboy/ssh-action`)
     ```bash
     set -e
     echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
     cd "$HOME/$APP_DIR"               # APP_DIR 은 워크플로우 env 에 정의 (현재 `app`)
     export BACKEND_TAG="$TAG"         # compose 의 image 태그를 이번 커밋 SHA 로 고정 → 불변 배포
     docker compose pull backend
     docker compose up -d backend      # backend 컨테이너만 교체
     docker image prune -f
     ```
     배포 디렉터리는 하드코딩이 아니라 워크플로우 상단 `env.APP_DIR` 로 빠져 있다.

## 3. 백엔드 Dockerfile (멀티스테이지)

```dockerfile
# 1단계: jar 빌드 (jdk)
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN chmod +x gradlew
COPY src src
RUN ./gradlew bootJar --no-daemon

# 2단계: 실행 (jre)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- **주의**: `build.gradle` 에서 `tasks.named('jar') { enabled = false }` 로 plain jar 를 껐다.
  → `build/libs/*.jar` 가 실행 jar **1개**만 매칭되어 `COPY ... app.jar` 가 안전.
- Actions runner 는 amd64 라 `--platform linux/amd64` 옵션이 필요 없다
  (로컬 Apple Silicon 에서 수동 빌드할 때만 필요했던 것).

## 4. 런타임 구성 — 프로파일 · 데이터 · 시크릿

배포된 컨테이너가 실제로 어떤 상태로 뜨는지. **CI/CD 자체보다 여기서 사고가 나기 쉽다.**

- **프로파일**: Dockerfile `ENTRYPOINT` 에 프로파일 지정이 없고 compose 에도 `environment` 가 없다.
  → `application.yaml` 의 `spring.profiles.active: prod` 가 그대로 적용된다(= `Jpa*RepositoryAdapter` + `JpaDataInitializer`).
- **⚠️ 배포할 때마다 DB 가 초기화된다.** prod 의 데이터소스는 H2 **인메모리**(`jdbc:h2:mem:testdb`)이고
  `ddl-auto: create` 다. compose 의 backend 에 volume 이 없으므로,
  **backend 컨테이너를 교체하는 모든 배포에서 기존 데이터가 전부 사라지고 시드가 재생성**된다.
  롤백·재부팅도 마찬가지. 데이터를 유지하려면 외부 DB(RDS 등)로 옮기고 접속정보를 환경변수로 주입해야 한다.
- **⚠️ `jwt.secret` 이 이미지에 평문으로 들어간다.** `application.yaml` 에 하드코딩되어 있어
  jar → 이미지에 그대로 포함된다. GHCR 이 private 이라 외부 노출은 막혀 있지만,
  이미지를 pull 할 수 있는 사람은 누구나 서명키를 얻는다. 환경변수(`JWT_SECRET`)로 빼는 것이 정석.
- **CORS**: `CorsConfig` 는 `http://localhost:5173` 만 허용한다. 현재는 nginx 가 frontend/backend 를
  같은 오리진(:80)으로 묶어주므로 배포 환경에서 CORS 자체가 발생하지 않아 문제가 없다.
  **프론트를 별도 도메인으로 분리하는 순간 깨진다.**

## 5. 이미지 레지스트리 — GHCR

- 이미지: `ghcr.io/100-hours-a-week/ktb_hoon_full_week4`
- private 패키지지만 배포 시 `GITHUB_TOKEN` 으로 로그인해 pull → **CD 파이프라인에는** 별도 시크릿 불필요.
  단 이 토큰은 워크플로우 종료 시 만료되므로 EC2 에서 수동으로 pull 할 땐 PAT 가 필요하다(§8).
- 태그 전략
  - `:latest` — 항상 최신
  - `:<커밋 SHA>` — 불변(immutable). 롤백 시 이 태그로 되돌린다.
- EC2 `docker-compose.yml` 의 backend 이미지:
  ```yaml
  backend:
    image: ghcr.io/100-hours-a-week/ktb_hoon_full_week4:${BACKEND_TAG:-latest}
  ```
  CD 가 `BACKEND_TAG=<SHA>` 를 export 후 `up -d` 하므로 매 배포가 특정 커밋으로 고정된다.
  (수동 실행/재부팅 시엔 `:latest` 로 폴백)

## 6. 필요한 GitHub Secrets / 설정

| 항목 | 값 | 비고 |
|---|---|---|
| `SSH_HOST` | EC2 IP/도메인 | 필수 |
| `SSH_USER` | `ubuntu` 등 | 필수 |
| `SSH_KEY` | pem 개인키 **전체 내용** | 필수 (`-----BEGIN...END-----`, 끝 `%` 제외) |
| `SSH_PORT` | SSH 포트 | 선택 (기본 22) |
| `GITHUB_TOKEN` | — | 자동 제공, 등록 불필요 |

추가로 repo → **Settings → Actions → Workflow permissions = Read and write** (GHCR push 용).

EC2 사전 준비:
- Docker 설치 + 사용자 `docker` 그룹 추가(`sudo usermod -aG docker $USER`)
- 보안그룹 인바운드: `80`(서비스), `22`(SSH)
- `~/app/` 에 `docker-compose.yml`, `nginx.conf` **수동 배치**
  - repo 의 `deploy/` 에 같은 내용의 사본이 있지만, **CD 는 이 파일들을 EC2 로 복사하지 않는다.**
    (워크플로우에 scp/rsync 스텝이 없다.) 서버가 실제로 쓰는 것은 EC2 의 `~/app/` 파일이다.
  - 따라서 `deploy/` 를 고쳐도 서버에는 아무 일도 일어나지 않는다. **양쪽을 수동으로 맞춰야 한다.**
  - nginx 설정을 바꿨다면 EC2 에서 `docker compose up -d nginx` 로 별도 반영.

## 7. 알아둘 동작

- **CD 상태는 커밋에 안 뜬다.** `workflow_run` 으로 트리거된 워크플로우는 커밋 SHA 에 체크로
  붙지 않고 Actions 탭에만 표시된다(GitHub 사양). 커밋 옆 체크는 CI(push)만 보인다.
- **cancelled 는 대개 정상이지만 항상은 아니다.** 연속 push 시 concurrency 설정이 이전 CD 를 취소하고
  최신만 배포한다. 다만 취소 시점이 **SSH 배포 스텝 도중**이면 `pull` 만 되고 `up -d` 는 안 된
  중간 상태로 EC2 가 남을 수 있다. 이미지 push 단계에서의 취소와는 위험도가 다르므로,
  cancelled 가 떴는데 서비스가 이상하면 EC2 에서 실제 컨테이너 상태를 확인할 것.
- **CD 워크플로우는 항상 main 의 `cd.yml` 정의로 실행된다.** `workflow_run` 트리거의 GitHub 사양이라,
  다른 브랜치에서 `cd.yml` 을 고쳐도 main 에 머지되기 전에는 반영되지 않는다.
- **배포 성공 = 서비스 정상이 아니다.** CD 는 `docker compose up -d` 의 성공만 확인하고 끝난다.
  compose 에 healthcheck 가 없어서 컨테이너가 떴다가 즉시 죽어도 워크플로우는 초록색으로 끝난다.
  배포 후에는 아래 "배포 확인" 을 수동으로 하거나, healthcheck + 배포 후 검증 스텝을 추가해야 한다.
- CI 성공 → CD 는 몇십 초 지연 후 `queued → in_progress → completed` 순으로 진행된다.

## 8. 롤백

특정 커밋 이미지로 되돌리려면 EC2 에서:
```bash
# GHCR 이 private 이므로 먼저 로그인 (아래 주의 참고)
echo "<GHCR PAT>" | docker login ghcr.io -u <github-user> --password-stdin

cd ~/app
export BACKEND_TAG=<되돌릴 커밋 SHA>
docker compose pull backend
docker compose up -d backend
```

- **⚠️ 배포 때 쓰는 `GITHUB_TOKEN` 은 워크플로우가 끝나면 만료된다.** EC2 에 남은 로그인 정보로는
  나중에 `pull` 이 401 로 실패할 수 있으므로, 수동 롤백에는 `read:packages` 권한의 PAT 가 필요하다.
- 되돌린 뒤 `BACKEND_TAG` 를 export 하지 않은 채 재부팅되면 compose 가 `:latest`(= 최신 커밋)로
  폴백하므로, 롤백 상태를 유지하려면 compose 파일의 태그를 직접 고정하거나 재배포로 해결할 것.
- 롤백해도 **DB 는 인메모리라 초기화**된다(§4). 데이터 복구 수단이 아니다.

## 9. 배포 확인

```bash
# EC2 에서
docker ps                    # backend 컨테이너가 새 이미지로 떠있는지
docker compose logs -f backend
curl -s localhost/api/v1/... # nginx 경유 백엔드 응답 확인
```

## 관련 파일

- `.github/workflows/ci.yml`, `.github/workflows/cd.yml`
- `Dockerfile`, `.dockerignore`
  - `.dockerignore` 는 `build/` 하위를 개별 제외하고 `build/libs` 는 빠져 있다.
    Dockerfile 이 `build/libs` 를 COPY 하지 않으므로 이미지에는 무해하고, 빌드 컨텍스트 전송량만 늘어난다.
- `build.gradle` (plain jar 비활성화)
- `deploy/docker-compose.yml`, `deploy/nginx.conf`
  - **배포되지 않는 사본.** EC2 `~/app/` 에 수동 배치·동기화해야 한다(§6 참고).
- `src/main/resources/application.yaml` (프로파일·DB·JWT 시크릿 — §4 참고)
