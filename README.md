## 🌱 Let's Meet!

### Back-end 소개

관심사·활동을 주제로 소모임을 만들고 모집하는 커뮤니티 프로젝트입니다.

Spring Boot로 서버를 구현하고, MySQL(RDS)을 DB로 사용했습니다.

개발은 초기 프로젝트 설정부터, DB 설계, JWT 인증, 검색·레이트리밋 같은 고도화, CI/CD 배포까지 직접 구현했습니다.

Controller-Service-Repository(포트/어댑터) 패턴으로 구현했으며, 저장소는 포트 인터페이스에 JPA/InMemory 두 구현체를 두어 프로파일별로 배선을 바꿉니다.

### 개발 인원 및 기간

- 개발기간 : 2026-06-04 ~ (진행중)
- 개발 인원 : Back-end 1명 (본인)

### 사용 기술 및 tools

- Java 17, Spring Boot 3.4.5
- Spring Data JPA, Spring Security, Spring Validation
- MySQL 8 (RDS) / H2 (test)
- JWT (jjwt 0.13.0)
- Caffeine (로컬 캐시)
- Gradle, Spotless(google-java-format aosp)
- JUnit5, AssertJ
- Docker, GitHub Actions, nginx, GHCR, EC2

### Front-end

- [Front-end Github](https://github.com/100-hours-a-week/KTB_hoon_full_week7)

### 서비스 시연 영상

- [링크](https://drive.google.com/file/d/1CeZ0o2Df8Yvj8D9igZE8vzPVAVCOf9M2/view?usp=drive_link)

### 폴더 구조

<details>
<summary>폴더 구조 보기/숨기기</summary>

```
kakao.bootcamp.fullstack
├── api
│   ├── controller                 # REST 컨트롤러
│   ├── domain                     # 엔티티 + 도메인별 에러코드
│   │   ├── auth
│   │   ├── comment
│   │   ├── common
│   │   ├── edit_revision
│   │   ├── member
│   │   ├── post
│   │   ├── post_draft
│   │   ├── report
│   │   └── search
│   ├── dto
│   │   ├── request
│   │   └── response
│   ├── repository                 # 포트 인터페이스 + jpa/inmemory 구현
│   │   ├── auth        {jpa, inmemory}
│   │   ├── comment      {jpa, inmemory}
│   │   ├── edit_revision{jpa, inmemory}
│   │   ├── member       {jpa, inmemory}
│   │   ├── post         {jpa, inmemory}
│   │   ├── post_draft   {jpa, inmemory}
│   │   ├── report       {jpa, inmemory}
│   │   └── search       {jpa, inmemory}
│   └── service
│       └── report
└── global
    ├── config                     # Security/Jwt/Cors/Jpa/Web/Scheduling
    ├── constants
    ├── exception                  # BusinessException 계층 + GlobalExceptionHandler
    ├── generator
    ├── init                       # 프로파일별 시드 러너
    ├── rate_limiter                # @RateLimited + sliding window
    ├── resolver                    # @LoginMember
    ├── response                    # ApiResponse 공통 응답
    ├── security
    │   ├── dto
    │   ├── filter                  # JwtAuthenticationFilter 등
    │   ├── hasher
    │   ├── jwt                     # Blacklist(Caffeine), JwtProvider
    │   └── token                   # RefreshToken 생성/해싱
    └── utils
```

</details>

## 서버 설계

### 인프라 구조

![Infrastructure & CI/CD Pipeline](docs/images/image.png)

- EC2 한 대에서 `docker compose`로 nginx + frontend + backend 3개 컨테이너를 함께 운영합니다. 외부에 공개되는 포트는 nginx의 `80`뿐이며, backend/frontend는 내부 네트워크에서만 접근됩니다. DB는 별도 AWS RDS(MySQL 8) 인스턴스를 사용합니다.
- `main` push 하나만 트리거로 쓰며(PR 없음), CI가 성공한 커밋만 CD로 이어져 GHCR에 이미지를 올리고 EC2의 backend 컨테이너만 교체합니다. nginx/frontend는 이 파이프라인이 건드리지 않습니다.
- 연속 push 시 이전 배포는 취소하고 최신 커밋만 배포합니다(`concurrency: cancel-in-progress`).

### 구현 기능

**Auth**
- 이메일/비밀번호 로그인, JWT 기반 무상태 인증 (AT: `Authorization` 헤더 / RT: HttpOnly 쿠키)
- RTR(Refresh Token Rotation) 기반 토큰 재발급, 로그아웃 시 세션 폐기

**Member**
- 회원가입 / 프로필 조회·수정 / 비밀번호 변경 / 회원탈퇴(소프트 삭제)

**Post / PostDraft**
- 모집글 CRUD, 모집 마감 처리, 좋아요, 조회수 집계
- 임시저장(Draft) 후 게시(publish)하는 작성 플로우

**Comment**
- 댓글 CRUD

**Search**
- 카테고리·모임형태·지역·기간·키워드 조건의 모집글 검색 + 커서 기반 페이지네이션(`GET /posts` 하나로 목록/검색 통합)

**Report / EditRevision**
- 게시글·댓글 신고, 누적 시 자동 블라인드 처리
- 수정 시마다 이전 내용을 스냅샷으로 보관(수정 이력 조회용)

**공통**
- 모든 도메인에 소프트 삭제(`@SQLDelete` + `deleted` 플래그) 정책 적용
- `{ message, code, data }` 형태로 통일된 응답 형식과 에러코드 체계

## 데이터베이스 설계

### E-R Diagram
![erd](docs/images/erd.png)

### 도메인 역할

| 도메인 | 역할 |
|---|---|
| Member | 회원 계정, 인증의 주체 |
| Post | 모집글 — 카테고리/모임형태/지역/모집상태/정원을 갖는 핵심 도메인 |
| PostDraft | 게시 전 임시 저장본. `publish` 시 `Post`로 전환 |
| Comment | 게시글에 달리는 댓글 |
| PostLike | 회원-게시글 좋아요 조인 엔티티 |
| PostViewLog | 게시글 조회 기록 로그 |
| RefreshToken | RTR용 갱신 토큰(해시 저장), family 단위로 회전·폐기 |
| Report | 게시글/댓글 신고. 누적 시 자동 블라인드 트리거 |
| EditRevision | 게시글/댓글 수정 이력 스냅샷 |

## 고도화

### 1. RTR(Refresh Token Rotation) + Caffeine Cache

단순 JWT만 쓰면 AT가 탈취돼도 만료 전까지 무효화할 방법이 없고, RT가 탈취되면 만료될 때까지 계속 재사용당할 수 있습니다. 이를 막기 위해 RTR과 2종의 블랙리스트를 조합했습니다.

- **AT/RT 분리 전달** : AT는 `Authorization: Bearer` 헤더로, RT는 `HttpOnly` 쿠키(`Path=/api/v1`)로 전달합니다. RT는 원문이 아니라 해시(`token_hash`)로만 DB에 저장합니다.
- **family 단위 회전** : 로그인 시 `family_id`(UUID)를 발급하고, `/reissue`를 호출할 때마다 기존 RT를 `revoked=true`로 바꾼 뒤 같은 `family_id`로 새 RT를 발급합니다.
- **재사용 탐지** : 이미 회전되어 폐기된(`revoked=true`) RT가 다시 들어오면 탈취·재사용으로 간주해, 그 `family_id`에 속한 RT 전체를 폐기하고 이후 요청은 세션 블랙리스트로 즉시 차단합니다.
- **로그아웃** : AT는 `jti` 기준으로 토큰 블랙리스트에 등록해 남은 만료 시간 동안 즉시 무효화하고, RT는 family 전체를 폐기합니다. 로그아웃은 공개 엔드포인트라 토큰이 없거나 무효해도 항상 200으로 멱등하게 처리합니다.
- **Caffeine 캐시** : `jti` 기준 `TokenBlacklist`와 `familyId` 기준 `SessionBlacklist` 2종을 Caffeine 로컬 캐시로 구현했습니다. value로 토큰의 만료 epoch millis를 저장하고 `Expiry`를 커스텀 구현해 엔트리별 TTL을 실제 토큰 만료 시각에 정확히 맞췄습니다(`expireAfter`). 그 덕분에 별도 정리 스케줄러 없이 각 엔트리가 자기 만료 시점에 자동으로 제거됩니다. `maximumSize`와 사용량 모니터링(`CacheUsageMonitor`)으로 무한 증가도 방지합니다.
- **보안 원칙** : 인증/토큰 관련 실패는 원인(만료/서명불일치/재사용감지 등)에 관계없이 동일한 에러코드로 응답해 공격자가 실패 사유로 내부 상태를 추측하지 못하게 하고, 실제 원인은 서버 로그로만 남깁니다.

### 2. 검색 : LIKE → FULLTEXT(ngram) 매치

기존 키워드 검색은 `LIKE '%keyword%'` 방식이었는데, 선행 와일드카드 때문에 인덱스를 타지 못하고 매번 풀스캔이 발생하는 문제가 있었습니다.

- **FULLTEXT 인덱스 도입** : `posts(title, content)`에 `WITH PARSER ngram` FULLTEXT 인덱스를 추가하고, `MATCH ... AGAINST(... IN BOOLEAN MODE)` 네이티브 쿼리로 전환했습니다.
- **BOOLEAN MODE + 구문 검색** : `AGAINST` 절에 키워드를 따옴표로 감싸 phrase search로 태워, `LIKE`의 부분 문자열 매칭 결과에 최대한 가깝게 맞췄습니다. 따옴표가 없으면 ngram 이 쪼갠 토큰들의 OR 검색이 돼(`등산모임` → `등산`/`산모`/`모임`) 결과가 크게 벌어집니다. `NATURAL LANGUAGE MODE`는 phrase 구문(따옴표)을 지원하지 않아 부분 문자열 매칭을 흉내낼 수 없고, relevance 순 정렬을 전제로 하는데 이 검색은 커서 페이지네이션이라 정렬을 고정해야 해서 배제했습니다.
- **키워드 정제** : 사용자 입력의 `"` 는 제거합니다(`sanitizeKeyword`). phrase 구문이 중간에 닫혀 쿼리 의미가 깨지는 걸 막기 위해서고, 따옴표 안에서는 `+ - * ~ ( )` 같은 boolean 연산자가 리터럴로 취급되므로 연산자 주입도 함께 차단됩니다. 다만 `ngram_token_size` 가 2 라 **1글자 키워드는 토큰이 만들어지지 않아 매치되지 않습니다** — `LIKE` 시절과 달라진 지점입니다.
- **쿼리 분기** : 키워드가 있을 때만 FULLTEXT 매치 네이티브 쿼리(`searchActivePostPage`)를 타고, 없으면 기존 JPQL 목록 쿼리(`findActivePostPage`)를 그대로 씁니다. 키워드 없는 요청까지 FULLTEXT 매치 비용을 치르지 않기 위한 분리입니다.
- **네이티브 쿼리의 제약** : 네이티브 쿼리에는 `@Enumerated(STRING)` 매핑이 적용되지 않아 enum 파라미터가 ordinal 로 바인딩되므로, `category`/`meetingType`/`recruitStatus`를 어댑터에서 문자열로 변환해 넘깁니다. `Pageable`에도 `Sort`를 싣지 않습니다 — Spring Data JPA 가 네이티브 쿼리와 동적 정렬의 조합을 허용하지 않아, 쿼리 문자열에 `ORDER BY created_at DESC, id DESC`를 직접 고정했습니다.
- **날짜 범위 검색 인덱스 실험** : 키워드 없는 날짜 범위 검색을 위해 `(created_at, deleted, blinded)` 복합 인덱스를 추가했습니다. `EXPLAIN ANALYZE`로 100만 건 기준 스캔 행 수가 777,056 → 10,218행(약 11.7배 개선)으로 줄어드는 걸 확인했지만, 넓은 range(월 단위 등)에서는 MySQL 옵티마이저가 힌트 없이 이 인덱스를 자동 채택하지 않는 사각지대도 함께 확인했습니다. `FORCE INDEX`는 프로덕션 쿼리에 넣지 않고 실험 기록으로만 남겼습니다.

**전환 후 측정에서 드러난 한계 (미해결)** — 운영 100만 건 기준으로 다시 재보니, 흔한 키워드에서는 FULLTEXT가 오히려 크게 느렸습니다.

| 키워드 | 매치 건수 | 응답 |
|---|---|---|
| `테니스` (매치 4.4%) | 41,679 | **34초** |
| `포핸드` | 3,478 | 0.15초 |
| `타이브레이크` | 99 | 0.10초 |
| 매치 없음 | 0 | 0.06초 |

원인은 FULLTEXT가 느려서가 아니라 **`ORDER BY`가 조기 종료를 막기 때문**이었습니다. `ORDER BY`만 제거하면 같은 쿼리가 21,573ms → 18.2ms(약 1,185배)로 끝납니다. FULLTEXT는 정렬 개념이 없어(`SHOW INDEX`의 `Collation`이 `NULL`) 최신순 11건을 고르려면 매치 전량을 받아 정렬해야 하고, `LIMIT`을 줄여도 이 비용은 그대로입니다.

반대로 `LIKE`는 PK 역순 스캔 방향이 정렬 방향과 같아 337행에서 조기 종료됐습니다(로컬 16ms). **두 방식의 최악 케이스가 정반대**라 — `LIKE`는 매치가 없을 때, FULLTEXT는 매치가 많을 때 무너집니다 — 한쪽으로 되돌리는 건 문제를 옮기는 것에 가깝습니다. 측정 근거와 선택지 정리는 `docs/fulltext-search-experiment.md`에 남겼고, 아직 결론을 내지 않았습니다.

### 3. 날짜 범위 검색 : 정렬 축을 인덱스에 맞추고 커서를 복합으로

운영에서 과거 구간 검색만 유독 느렸습니다. **같은 크기의 기간인데 얼마나 과거인지에 따라 50배 차이**가 났습니다.

- **원인 — 필터 축과 정렬 축의 불일치** : 필터는 `created_at`, 정렬은 `id`였습니다. MySQL은 접근 경로를 하나만 고를 수 있어 "정렬이 공짜인 `PRIMARY`"를 골랐고, 그 대가로 `created_at` 조건을 행마다 확인해야 했습니다. 11건을 반환하려고 **777,756행**을 읽고 있었습니다(`Sort` 노드는 없었습니다 — 정렬 비용이 아니라 필터 비용이었습니다).
- **인덱스는 이미 있었습니다** : `ALTER TABLE ... ADD INDEX`가 `Duplicate key name`으로 실패해 확인했습니다. 인덱스가 없어서가 아니라 **옵티마이저에게 그 인덱스를 고를 이유를 준 적이 없었던 것**입니다.
- **해결 1 — `ORDER BY`를 인덱스 순서에 맞춤** : `ORDER BY created_at DESC, id DESC`로 바꾸자 `PRIMARY`의 유일한 장점(정렬 공짜)이 사라지고, 복합 인덱스 하나가 필터와 정렬을 동시에 만족하게 됐습니다. 참고로 옵티마이저의 추정 비용은 오히려 새 계획이 더 비쌌습니다(9.83 vs 18,925) — 설득한 게 아니라 **틀린 선택지를 없앤 것**입니다.
- **새 문제 — 커서가 다시 어긋남** : 정렬이 `created_at` 기준이 되자 이번엔 `WHERE id < :cursor`가 인덱스를 타지 못했습니다. 커서 위치까지 걸어가느라 **900,040행 / 9,850ms**. 커서는 사용자 입력이라 임의 값으로 10초짜리 쿼리를 만들 수 있는 문제이기도 했습니다.
- **해결 2 — 복합 커서** : 커서도 정렬과 같은 `(created_at, id)`로 맞췄습니다. 작성일이 완전히 같은 글이 다수 존재해(벤치 데이터에 같은 마이크로초 14건) 작성일만으로는 페이지 경계를 표현할 수 없어, `id`를 동점 처리로 함께 씁니다. `nextCursor`는 불투명 base64 문자열로 내보내 프론트가 형식에 의존하지 않게 했습니다.

| 쿼리 (과거 1개월 구간) | 접근 경로 | 읽은 행 | 시간 |
|---|---|---|---|
| `ORDER BY id DESC` | `PRIMARY` 역순 | 777,756 | **4,944ms** |
| `ORDER BY created_at DESC, id DESC` | 복합 인덱스 범위 역순 | **11** | **1.24ms** |

한 줄로 줄이면 **정렬 축·필터 축·커서 축이 전부 같아야 인덱스가 일한다**는 것이었고, 세 축이 어긋날 때마다 같은 증상이 반복됐습니다. 전 과정은 `docs/date-range-search-troubleshooting.md`에 순서대로 남겼습니다.

> 정렬 옵션(인기순·조회수순)을 늘리지 않은 것도 같은 이유입니다. 커서 페이지네이션은 **정렬 키가 변하지 않는다**를 전제로 하는데, `like_count`는 페이지를 넘기는 사이에 바뀌어 글이 누락되거나 중복될 수 있습니다.

### 4. 도배 방지 Rate Limit : Fixed Window → Sliding Window

`POST /posts`, 임시글 `publish` 같은 작성형 API에 분당 요청 수를 제한해 도배(무한 게시글 등록)를 막고 있는데, 초기 구현이었던 fixed window 방식에는 구조적인 허점이 있었습니다.

- **fixed window의 문제** : 1분을 고정 구간으로 나눠 구간별로 카운트하면, 구간 경계에서 burst가 통과합니다. 예를 들어 0:59에 3건, 1:00에 다시 3건을 몰아 보내면 단 2초 사이에 6건이 통과해 "분당 3건"이라는 정책의 의도를 벗어납니다.
- **sliding window로 전환** : 회원별로 최근 요청 타임스탬프를 `Deque`에 기록합니다. 요청이 올 때마다 현재 시각 기준 `windowMinutes` 이전 타임스탬프를 큐 앞에서 모두 제거하고, 남은 개수가 `limit` 이상이면 거부, 아니면 현재 시각을 큐에 추가하고 허용합니다. 항상 "지금 시점 기준 최근 N분"을 정확히 보므로 경계 burst가 사라집니다.
- **선언적 정책 부여** : `@RateLimited(limit, windowMinutes)` 애노테이션 + `RateLimitInterceptor`로 엔드포인트마다 다른 제한치를 선언적으로 붙일 수 있습니다(기본값 1분 3건). 카운터는 회원 단위로 공유됩니다.
- **한계** : 인메모리(`synchronized` + `HashMap`) 구현이라 인스턴스를 여러 대로 늘리면 인스턴스별로 카운터가 분리됩니다. 현재는 backend 컨테이너가 단일 인스턴스로 배포돼 있어 문제가 없지만, 스케일 아웃 시에는 Redis 등 공유 저장소로 옮겨야 합니다.