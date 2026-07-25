# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build tool is Gradle (wrapper checked in). Java 17, Spring Boot 3.4.5.

```bash
./gradlew build                 # compile + run all tests
./gradlew test                  # run all tests
./gradlew bootRun               # run app (defaults to 'prod' profile → JPA + in-memory H2)
./gradlew bootRun --args='--spring.profiles.active=local'   # run with ConcurrentHashMap repos + seed data

# Single test class / method
./gradlew test --tests 'kakao.bootcamp.fullstack.member.MemberServiceTest'
./gradlew test --tests 'kakao.bootcamp.fullstack.member.MemberServiceTest.methodName'
```

There is no linter configured. The H2 console (prod profile) is at `/h2-console` (`jdbc:h2:mem:testdb`, user `admin`).

## Profile-driven architecture

The single most important structural fact: **which repository implementation is active depends on the Spring profile**, wired via `@Profile` on beans — no interface method changes when swapping backends.

- `prod` (default, see `application.yaml`): JPA adapters (`@Profile("prod")`, package `.../repository/<domain>/jpa/`) backed by H2 (`ddl-auto: create`). `JpaDataInitializer` seeds data, guarded by an existing-email check so it's idempotent.
- `local`: In-memory `ConcurrentHashMap` implementations (`@Profile("local")`, package `.../repository/<domain>/inmemory/`), IDs from `AtomicLongIdGenerator`. `DataInitializer` seeds data unconditionally.
- `test`: a distinct third profile used by web/integration tests (`application-test.yml`, H2 `create-drop`). Note the wiring quirk: only `JpaMemberRepositoryAdapter` is `@Profile({"prod","test"})` — the other domains' adapters are `@Profile("prod")` only, so under `test` alone they have no bean. Service/domain unit tests avoid this by using hand-written fakes instead of the container.

Every domain's persistence follows the **hexagonal port/adapter** shape:
- `repository/<domain>/XxxRepository.java` — the port (plain interface the services depend on).
- `.../jpa/JpaXxxRepository.java` — Spring Data interface; `.../jpa/JpaXxxRepositoryAdapter.java` — adapter implementing the port, `@Profile("prod")`.
- `.../inmemory/InMemoryXxxRepository.java` — a second port impl annotated `@Profile("local")`.

When adding a repository method, add it to the port interface **and** both adapters (jpa + inmemory), or the missing-profile bean will fail to satisfy the other profile.

## Package layout (`kakao.bootcamp.fullstack`)

- `api/` — the feature slice: `controller/`, `service/`, `repository/`, `domain/`, `dto/{request,response}`.
- `global/` — cross-cutting infrastructure: `config/`, `security/`, `exception/`, `rate_limiter/`, `resolver/`, `constants/`, `generator/`, `init/`.
- `practice/` — scratch/experimental code, not part of the running app’s core paths.

Domains under `api/domain/`: `member`, `post`, `comment`, `post_draft`, `report`, `edit_revision`, plus `auth` and `common` (`common` holds `BaseEntity`); `post` also has sub-entities `PostLike`/`PostViewLog`. There are 5 controllers — `Auth`, `Member`, `Post`, `PostDraft`, `Report`; comment operations are served through `PostController` (there is no `CommentController`).

## Domain model conventions

- Entities extend `BaseEntity` and use **static factory methods** (`Post.create(...)`, `Member.create(...)`) — constructors are `PROTECTED`. Business rules live on the entity (e.g. `Post.increaseReportCount()` auto-blinds at `PostConstants.BLIND_THRESHOLD`; `updatePost` sets the `edited` flag).
- **Soft delete** via Hibernate `@SQLDelete` (`UPDATE ... SET deleted = true`); repositories expose `findActiveById` / active-only queries to filter deleted rows. In-memory/fake repos replicate this filtering manually.
- The in-memory path assigns IDs through `entity.assignId(...)` (throws if already assigned); JPA uses `GenerationType.IDENTITY`.

## Error handling & API envelope

- Every response is wrapped in `ApiResponse<T>` `{ message, code, data }`. Base URL is `/api/v1`. See `docs/api-specification.md` for the full endpoint + error-code contract and `docs/postman-test-data.md` for sample payloads.
- Error codes are **enums implementing `BaseCode`** (`getHttpStatus/getCode/getMessage`), one enum per domain (`PostErrorCode`, `MemberErrorCode`, ...) plus `CommonErrorCode`/`ValidationCode`/`SuccessCode`. Throw a typed `BusinessException` subclass (`NotFoundException`, `ForbiddenException`, `ConflictException`, `UnauthorizedException`, `TooManyRequestsException`, `BadRequestException`, `InternalServerException`) carrying a `BaseCode`.
- `GlobalExceptionHandler` maps these to HTTP responses. New failure modes should be added as an enum constant, not an ad-hoc message.

## Security & request pipeline

- Stateless JWT (jjwt). `JwtAuthenticationFilter` authenticates; `SecurityExceptionFilter` sits *before* it to translate auth exceptions into the standard envelope. Public routes are the array in `PublicEndpointConstants.PUBLIC_ENDPOINTS` — add new unauthenticated endpoints there.
- Controllers get the caller via `@LoginMember AuthMember` (resolved by `LoginMemberArgumentResolver` from the security context) — do not read the principal manually.
- **Rate limiting** is annotation-driven: put `@RateLimited(limit=…, windowMinutes=…)` on a controller method; `RateLimitInterceptor` (registered in `WebConfig` for `/api/**`) enforces it per `memberId`. Default is 3 requests / 1 minute.
- Password hashing goes through the `PasswordHasher` port (impls: `SimplePasswordEncoder`, `SpringSecurityPasswordEncoderAdapter`).

## Strategy pattern for reports

`ReportService` depends on `List<ReportTargetHandler>`; each handler `supports(TargetType)` (e.g. `PostReportHandler`, `CommentReportHandler`) and applies the side effect (increment report count / blind). A `@PostConstruct` check fails fast at startup if any `TargetType` has no handler. To support a new reportable type, add the `TargetType` and a corresponding handler bean.

## Testing conventions

- Tests use `application-test.yml` (H2 `create-drop`). Auth in web tests is faked via `@WithMockAuthMember` + `WithMockAuthMemberSecurityContextFactory` (see `src/test/.../security/`).
- Service/domain unit tests use hand-written **fakes** (e.g. `FakeMemberRepository`) implementing the repository port rather than mocks — keep fakes in sync with the port interface.
- Test fixtures live under `.../<domain>/fixture/`; reuse them instead of building entities inline.
