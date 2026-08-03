# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Spring Boot 3.4.5 / Java 17 기반의 커뮤니티(당근 "모집글" 성격) API 서버.

## 명령어

```bash
./gradlew build                 # 컴파일 + 전체 테스트
./gradlew test                  # 전체 테스트
./gradlew spotlessApply         # 포맷 적용 (커밋/빌드 전 필수)
./gradlew bootRun               # 서버 실행 (기본 prod 프로파일, H2 인메모리)

# 단일 테스트
./gradlew test --tests 'kakao.bootcamp.fullstack.auth.AuthServiceTest'
./gradlew test --tests 'kakao.bootcamp.fullstack.auth.AuthServiceTest.rotatesSuccessfully'

# 실행 시 설정 덮어쓰기 (예: 스케줄러/토큰 만료 짧게 두고 관찰)
./gradlew bootRun --args='--jwt.access-token-expire-seconds=2 --security.blacklist.cleanup-interval-ms=3000'
```

- **포맷**: spotless `googleJavaFormat().aosp()` + `removeUnusedImports`. 포맷 어긋나면 `build`가 실패하므로 항상 `spotlessApply` 후 빌드.
- **스키마**: `ddl-auto: create` (운영/로컬), 테스트는 `create-drop`. **마이그레이션 도구 없음** — 엔티티 변경이 곧 스키마. 부팅 시 시드 데이터가 재생성된다.

## 프로파일 = 아키텍처의 핵심 축

이 프로젝트의 가장 중요한 구조. 프로파일에 따라 저장소 구현과 시드 러너가 통째로 바뀐다.

- **`prod`** (기본 active): `Jpa*RepositoryAdapter`(`@Profile("prod")`) + `JpaDataInitializer` 시드. DB는 H2 인메모리.
- **`local`**: `InMemory*Repository`(`@Profile("local")`) + `DataInitializer` 시드. DB 없이 `ConcurrentHashMap`.
- **`test`**: `src/test/resources/application-test.yml`, H2 `create-drop`.

### 저장소 포트 + 2중 구현 패턴 (필수 인지)
모든 저장소는 **포트 인터페이스**(`api/repository/<domain>/XxxRepository`)이고 구현이 **두 벌**이다:
- `.../jpa/JpaXxxRepositoryAdapter` (`@Profile("prod")`, 내부에 Spring Data `JpaXxxRepository` 위임)
- `.../inmemory/InMemoryXxxRepository` (`@Profile("local")`)

**저장소 인터페이스에 메서드를 추가하면 JPA·InMemory 두 구현 모두**에 반영해야 하고, 테스트 fake에도 반영해야 컴파일된다. InMemory 구현은 소프트삭제 필터링(`!isDeleted()`)까지 재현하므로 테스트 fake의 참고 원본이 된다.

## 도메인 모델 규약

- **공통 조상 `global/BaseEntity`**: `createdAt/updatedAt`(JPA Auditing), `deleted/deletedAt` + `delete()/restore()`.
- **소프트삭제**: 모든 엔티티가 `@SQLDelete`로 물리삭제 대신 `deleted=true` UPDATE. 조회 쿼리는 항상 `deleted = false` 조건을 건다. (상태 플래그와 공존시키며 `@SQLDelete`는 유지)
- 엔티티는 정적 팩토리(`Xxx.create(...)`) + `@NoArgsConstructor(PROTECTED)`. 상태 변경은 도메인 메서드로.
- 도메인은 `api/domain/<name>` 아래: auth, member, post, post_draft, comment, report, edit_revision, common.

## 응답 · 에러 코드 체계

- **`global/response/ApiResponse<T>`**: 모든 응답 공통 봉투(`message`, `code`, `data`).
- **`BaseCode` 인터페이스**를 도메인별 에러 enum(`XxxErrorCode`)과 `CommonErrorCode`, `SuccessCode`가 구현. 각 코드는 `(HttpStatus, code, message)`.
- **예외 계층**: `BusinessException` ← `BadRequest/Unauthorized/Forbidden/NotFound/Conflict/TooManyRequests`. 각 생성자는 `BaseCode`를 받는다. `GlobalExceptionHandler`가 처리.
- **Bean Validation 연동(비자명)**: DTO의 `@NotBlank(message = ValidationCode.XXX)`에 쓰는 메시지는 `ValidationCode`의 상수 문자열이고, 핸들러가 `ErrorCodeMapper.from(code)`로 **그 문자열과 `getCode()`가 일치하는 `BaseCode`를 찾아** 응답을 만든다. 따라서 검증 메시지를 추가할 땐 **`ValidationCode` 상수 + 대응하는 `XxxErrorCode` 항목을 둘 다** 만들어야 한다.
- **보안 예외 원칙**: 인증/토큰 실패는 응답 코드를 하나로 통일해 추측을 막고, 실제 사유는 서버 로그로만 남긴다(예: RT 재사용도 `INVALID_REFRESH_TOKEN`으로 응답).

## 인증 / JWT

- 무상태 JWT. **AT는 `Authorization: Bearer`**, **RT는 HttpOnly 쿠키**. 로그인/재발급 응답 body에 AT.
- **RTR(Refresh Token Rotation)** + RT family. 이미 회전된 RT 재사용 감지 시 family 전체 폐기.
- **블랙리스트 2종**(`global/security/jwt/`): `TokenBlacklist`(jti 기준), `SessionBlacklist`(familyId 기준). 값은 만료 epoch millis. `exists()`는 조회 시 만료분을 걸러내고(lazy-expiry), `BlacklistCleanupScheduler`(`@Scheduled`, `SchedulingConfig`의 `@EnableScheduling`)가 주기적으로 만료분을 벌크 제거. 주기는 `security.blacklist.cleanup-interval-ms`.
- `JwtAuthenticationFilter`가 AT 검증→블랙리스트 확인. 컨트롤러는 `@LoginMember AuthMember`(`LoginMemberArgumentResolver`)로 인증 회원 주입.
- 설정은 `jwt.*`(yaml) → `JwtProperties`.

## 기타 횡단 관심사

- **레이트리밋**: `@RateLimited` + `RateLimitInterceptor`(인메모리 fixed window, 1분 3건). 예: `POST /posts`.
- **커서 기반 페이지네이션**: 목록 조회는 `id < cursor ... ORDER BY id DESC` 방식(offset 아님).
- 설정 클래스는 `global/config/`(Security/Jwt/Cors/Jpa/Web/Scheduling).

## 테스트 컨벤션

상세는 `docs/testing-conventions.md`. 핵심:
- **서비스/도메인 유닛 테스트는 Mockito 목이 아니라 손으로 쓴 fake**로 포트를 구현하고 `new XxxService(...)`로 조립. fake는 `.../<domain>/fake/`에 두고 소프트삭제 필터링을 재현. 순수 로직 협력자(해셔·생성기 등)는 fake 대신 실제 인스턴스 사용.
- JUnit5 + AssertJ. 대상 메서드가 여럿이면 `@Nested` + 한글 `@DisplayName`, 리포지토리(`@DataJpaTest`)·검증 테스트는 평면 구조.
- 예외 검증은 에러코드까지 확인(`extracting(BusinessException::getCode)`).

## 커밋 메시지

기존 이력 컨벤션: `type : 한글 설명` (`feat`/`refactor`/`test`/`docs`/`style`/`perf`). 예: `refactor : findActiveByTokenHash → findNotDeletedByTokenHash 리네임`.
