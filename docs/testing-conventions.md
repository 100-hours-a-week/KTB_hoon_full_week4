# 테스트 컨벤션

이 프로젝트에서 테스트를 작성할 때 따르는 규칙과 패턴을 정리한다.

## 실행 명령

```bash
./gradlew test                  # 전체 테스트
./gradlew build                 # 컴파일 + 전체 테스트

# 단일 클래스 / 메서드
./gradlew test --tests 'kakao.bootcamp.fullstack.auth.AuthServiceTest'
./gradlew test --tests 'kakao.bootcamp.fullstack.auth.AuthServiceTest.rotatesSuccessfully'
```

테스트는 `application-test.yml`(H2 `create-drop`)을 사용한다.

## 프레임워크 · 스타일

- **JUnit5 + AssertJ**. Mockito도 사용 가능.
- 테스트 클래스: `public class XxxTest`.
- **`@Nested` + `@DisplayName`**: 대상 메서드가 여럿인 테스트(서비스·컨트롤러 등)는 메서드별로 `@Nested` 클래스를 두고 한글 `@DisplayName`을 단다. 단일 대상·단순 케이스 위주인 리포지토리(`@DataJpaTest`)·검증(`@ValidXxx`) 테스트는 `@Nested` 없이 평면 구조로 둔다.
- 테스트 메서드명은 영어 camelCase, 본문은 `// given / // when / // then` 주석 블록으로 구분한다. **설명성 주석은 두지 않는다**(비자명한 "왜"만 예외로 허용).
- 예외 검증은 아래 관용구를 쓴다 (에러코드까지 확인):

```java
assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> authService.reissue(rt))
        .extracting(BusinessException::getCode)
        .isEqualTo(AuthErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
```

## 무엇에 단언하는가 — 단언 대상 선택 (핵심)

테스트 더블을 **어떻게 조립하나**는 아래에서 다룬다. 그 전에 **무엇을 검증 대상으로 삼는가**를 정한다. 이 기준이 흔들리면 "돌아가긴 하는데 아무것도 증명하지 못하는" 테스트가 나온다.

**대원칙: 계약(무엇을 하기로 했는가)을 증명하는 가장 작은 관찰가능값에 assert한다. 상호작용(어떻게 했는가)이 아니라 상태·결과를 본다.** 상호작용 검증은 구현 방식에 결합돼 리팩터링에 부서지고, 상태·반환 검증은 계약에 결합돼 살아남는다.

### 관찰가능값 우선순위 사다리

위에서부터 가능한 것을 쓴다. 아래로 내려갈수록 최후의 수단이다.

1. **반환값** — 메서드가 의미 있는 값을 반환하면 **그 규칙을 담은 필드**에 assert한다. 예: `AuthServiceTest`가 `result.accessToken()`/`refreshTokenMaxAgeSeconds()`를 확인.
2. **포트에 남은 결과 상태** — 부작용이 저장소 등 포트의 상태 변화라면 **fake를 조회해 그 상태**에 assert한다. `verify(repo).save(...)`가 아니라 `storedTokenOf(rt).isRevoked()`처럼 **결과를 되읽는다**. (fake는 실제 상태를 들고 있으므로 이게 가능하다.)
3. **던진 예외** — 타입 + 에러코드까지 확인한다(위 "프레임워크 · 스타일"의 예외 관용구).
4. **반환도 상태도 없는 순수 부수효과** — 로그·이벤트뿐인 컴포넌트는 **그 효과를 캡처해 assert**한다. 이때만 "행위 검증"이 1순위가 되며, 이는 되읽을 상태가 정말로 없기 때문이다. 표준 예시: `CacheUsageMonitorTest`가 logback `ListAppender`로 로그를 캡처해 레벨·상태전이를 단언한다.

### 상호작용 검증(Mockito `verify`)은 원칙적으로 쓰지 않는다

- 유닛 테스트는 **fake의 결과 상태를 조회해 단언**한다. `verify(...)`/`times(...)`로 "호출됐는지"를 보지 않는다.
- 예외는 **되읽을 상태도 반환값도 없고 "호출 사실 자체가 계약"인 순수 외부 경계**뿐이다. 그마저도 `verify` 대신 **호출을 기록하는 hand-written fake**(호출 인자·횟수를 필드로 축적)로 바꿔, 기록된 상태에 assert하는 쪽을 택한다.
- 근거: `verify`는 테스트 대상이 협력자를 *어떻게* 부르는지에 결합한다. 같은 계약을 다른 방식으로 구현하면 깨지므로, 리팩터링을 막는 테스트가 된다.

### 안티패턴 (에이전트가 자주 저지르는 것)

- **과소 단언**: 관찰가능한 결과가 있는데 `assertThatCode(...).doesNotThrowAnyException()` 하나로 끝낸다. "안 터졌다"는 계약이 아니다 — 반환/상태/예외 중 실제 규칙을 짚어라.
- **과다 단언**: 규칙과 무관한 부수 필드까지 전부 assert하거나, 계약이 약속하지 않은 호출 순서·횟수를 검증한다. 규칙을 담은 필드만 짚는다.
- **더블 내부 배선에 단언**: 테스트 대상의 관찰가능 행위가 아니라 fake/목의 내부 구현이 특정 값이 됐는지를 본다.
- **상태로 볼 수 있는 걸 상호작용으로 검증**: `verify(repo).save(x)` 대신 `storedTokenOf(...)`로 결과를 되읽는다.

## 서비스 · 도메인 유닛 테스트 — hand-written fakes

**서비스/도메인 유닛 테스트는 Mockito 목이 아니라 손으로 쓴 fake로 포트를 구현한다**. 컨테이너 없이 `new XxxService(...)`로 조립한다.

- fake는 `.../<domain>/fake/`에 두고, 리포지토리 **포트 인터페이스를 직접 구현**한다.
- **소프트삭제/active 필터링을 fake 안에서 재현**한다 (`.filter(e -> !e.isDeleted())`). 프로덕션 인메모리 구현(`InMemoryXxxRepository`)이 그대로 참고 원본이 된다.
- 상태를 재사용한다면 `clear()` 헬퍼를 제공한다. (예: `FakeMemberRepository`)
- **순수 로직 협력자(암호화·해싱·생성기 등)는 fake 대신 실제 인스턴스를 쓴다.** 예: `RefreshTokenGenerator`, `RefreshTokenHasher`는 `new`로 실제 객체를 넣어 raw↔hash 왕복이 실제로 맞물리게 한다.
- `record` 설정 객체는 직접 생성한다. 예: `new JwtProperties("secret", 600, 1209600)`.

참고 구현:
- `src/test/.../member/fake/FakeMemberRepository.java`
- `src/test/.../auth/fake/` (`FakeRefreshTokenRepository`, `FakePasswordHasher`, `FakeJwtProvider`, `FakeTokenBlacklist`, `FakeSessionBlacklist`)
- `src/test/.../auth/AuthServiceTest.java` — fakes로 조립한 서비스 테스트의 표준 예시.

> 주의: `fake`는 포트가 바뀌면 같이 갱신해야 한다(컴파일러가 강제). 포트에 메서드를 추가하면 fake·프로덕션 어댑터(jpa/inmemory)도 함께 수정한다.

> 서비스 유닛 테스트는 모두 fake 기반이다(`AuthServiceTest`, `MemberServiceTest`). Mockito `@Mock`/`verify`로 서비스를 조립하지 않는다.

## 픽스처

**테스트에서 도메인 객체(엔티티 등)를 직접 `new`/`create` 하지 말고 픽스처를 거친다.** 생성 방식이 한 곳에 모여야 필드가 바뀌어도 흔들리지 않는다.

- 엔티티 픽스처는 `.../<domain>/fixture/`, 요청 DTO 픽스처는 `.../<domain>/fixture/dto/`에 둔다. 인스턴스화 불가한 홀더 클래스에 **static 팩토리**를 두고, 빌더 라이브러리는 쓰지 않는다.
- **기준값(canonical)은 한 곳에서만 정의한다.** 기준 인자는 픽스처 클래스의 `public static final` 상수로 선언하고, no-arg 팩토리가 그 상수로 기준 인스턴스를 만든다. 파라미터 오버로드는 no-arg를 재사용하거나(예: id만 추가) 공유 상수를 재사용해 기준 리터럴을 반복하지 않는다.
  예: `MemberFixture.EMAIL`/`ENCODED_PASSWORD`/`NICKNAME`/`PROFILE_IMG_URL` 상수 + `activeMember()`(기준값) / `activeMember(Long id)`(→ no-arg + `assignId`) / `activeMember(String email, String encodedPassword)`.
- 상수를 `public`으로 두면 라운드트립을 검증하는 테스트가 리터럴을 하드코딩하지 않고 픽스처 상수를 참조할 수 있다(예: `MemberRepositoryTest`가 저장 후 필드를 `MemberFixture.EMAIL` 등과 비교).
- **id 유무를 흐름에 맞춘다.** 리포지토리 저장으로 id가 부여되는 흐름(`@DataJpaTest`)에는 **id 미할당** 팩토리를, id가 미리 필요한 서비스 유닛 등에는 `assignId(...)`된 팩토리를 쓴다.
- **상태별 팩토리로 경계·의미를 명확히 한다.** 유효/만료처럼 갈리는 경우 이름 있는 팩토리로 나눠 오해와 경계 누락을 막는다. 예: `RefreshTokenFixture.active(...)` / `expired(...)`.
- 엔티티는 정적 팩토리(`Xxx.create(...)`)로 만들고 `assignId(...)`로 id를 부여한다.
- 예외: 도메인 객체 **자체를 검증하는 단위 테스트**(예: `MemberTest`)는 대상 생성 로직을 직접 짚는 게 자연스러우니 픽스처를 강제하지 않는다.

참고: `src/test/.../member/fixture/MemberFixture.java`, `.../auth/fixture/RefreshTokenFixture.java`, `.../fixture/dto/SignupReqDtoFixture.java`.

## 상수 · 매직값

**여러 번 쓰이는 리터럴/매직값은 반드시 이름으로 뺀다.** 같은 값을 두 곳 이상에서 쓰면 상수로 올려 한 곳에서만 바꾸게 한다.

- **여러 테스트 클래스가 공유**하면 **상수 홀더 클래스**로 뺀다. 프로덕션 constants 스타일(`@NoArgsConstructor(access = AccessLevel.PRIVATE)`)을 따르고 `static import`로 참조한다.
  예: `TokenExpireTestConstants.ACCESS_TOKEN_EXPIRE_SECONDS` / `REFRESH_TOKEN_EXPIRE_SECONDS` — `AuthServiceTest`·`AuthControllerTest`가 공유.
- **한 테스트 클래스 안에서만 반복**되면 그 클래스의 `private static final` 필드로 둔다. 구조가 반복되는 요청 바디 등은 작은 헬퍼로 묶어 값만 넘긴다.
  예: `AuthControllerTest`의 `EMAIL`/`PASSWORD`/`WRONG_PASSWORD` 필드 + `loginBody(email, password)` 헬퍼.
- 경계 검증에서 길이 같은 값은 **문자열 변수 + `hasSize(N)`로 먼저 못박아** 값이 어긋나면 검증 단언 자체가 깨지게 한다(예: `ValidPasswordTest`의 8/20/21자 경계).

참고: `src/test/.../auth/TokenExpireTestConstants.java`, `.../auth/AuthControllerTest.java`, `.../member/validation/ValidPasswordTest.java`.

## 리포지토리 테스트 — `@DataJpaTest` 슬라이스

JPA 쿼리·소프트삭제 동작은 H2에 대고 슬라이스 테스트로 검증한다.

```java
@DataJpaTest
@ActiveProfiles("test")
@Import({JpaRefreshTokenRepositoryAdapter.class, JpaConfig.class})
public class RefreshTokenRepositoryTest {
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    // ...
}
```

- 필요한 **JPA 어댑터와 `JpaConfig`만 명시적으로 `@Import`**한다.
- 소프트삭제는 `entity.delete()` 후 `save()`로 재현하고, `findActive*`가 걸러내는지 확인한다.
- `revoked`(상태 플래그)와 `deleted`(소프트삭제)는 별개다 — revoke돼도 삭제되지 않았으면 여전히 조회되어야 한다.

참고: `src/test/.../member/MemberRepositoryTest.java`, `src/test/.../auth/RefreshTokenRepositoryTest.java`.

## 프로파일 빈 배선 주의 (중요)

`test` 프로파일에는 일부 빈이 **존재하지 않는다.** 리포지토리 어댑터 중 `@Profile({"prod","test"})`인 것만 test에서 살아있고(`JpaMemberRepositoryAdapter`, `JpaRefreshTokenRepositoryAdapter`), 나머지 도메인 어댑터와 **`InMemoryTokenBlacklist`·`InMemorySessionBlacklist`(`@Profile({"local","prod"})`)는 test 프로파일에 빈이 없다.**

결과:
- **`@SpringBootTest(@ActiveProfiles("test"))`로는 `AuthService`를 배선할 수 없다** — `TokenBlacklist`/`SessionBlacklist` 빈이 없어 컨텍스트 로딩이 실패한다.
- 그래서 서비스 로직은 **fake 기반 유닛 테스트**로 검증한다(위 참조).
- 컨트롤러 레이어는 아래 "웹 · 시큐리티 테스트"처럼 **`@WebMvcTest` 슬라이스**로 간다(협력자는 `@MockitoBean`으로 채운다).

## 웹 · 시큐리티 테스트

컨트롤러 테스트는 **`@WebMvcTest`(대상 컨트롤러) + `@Import`(시큐리티 설정) + `@MockitoBean`(협력자)** 슬라이스를 쓴다. 전체 컨텍스트(JPA·DataSource·시딩)를 띄우지 않아 가볍다.

```java
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class AuthControllerTest {
    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuthService authService;       // 대상 서비스
    @MockitoBean private JwtProvider jwtProvider;        // 필터 협력자
    @MockitoBean private TokenBlacklist tokenBlacklist;
    @MockitoBean private SessionBlacklist sessionBlacklist;
    @MockitoBean private RateLimiter rateLimiter;        // 인터셉터 협력자
}
```

배선 규칙 두 가지가 핵심이다:

- **`@Import`로 시큐리티 설정을 명시한다.** `@WebMvcTest`는 우리 `SecurityConfig`(@Configuration)를 자동 적용하지 않는다 — 빼면 Boot 기본 시큐리티가 먹어 public 엔드포인트도 403이 난다. `SecurityConfig`와, 그것이 요구하지만 슬라이스가 스캔하지 않는 핸들러 빈(`JwtAuthenticationEntryPoint`·`JwtAccessDeniedHandler`, 이 둘은 `HandlerExceptionResolver`만 의존 → MVC 오토컨피그가 공급)을 함께 `@Import`한다.
- **웹 컴포넌트의 슬라이스-밖 협력자를 `@MockitoBean`으로 채운다.** `@WebMvcTest`는 `@Component` 필터·인터셉터(`JwtAuthenticationFilter`·`SecurityExceptionFilter`·`RateLimitInterceptor`)를 웹 컴포넌트로 끌어오므로, 그 의존(`JwtProvider`·`TokenBlacklist`·`SessionBlacklist`·`RateLimiter`)이 없으면 컨텍스트가 안 뜬다. public 엔드포인트는 필터가 `shouldNotFilter`로 skip되고 `@RateLimited` 없는 핸들러는 인터셉터가 조기 return하므로, 컨텍스트 로딩용으로만 목을 두면 되고 스터빙은 불필요하다.

그 위에서:

- 대상 서비스만 `@MockitoBean`으로 대체하면 나머지(실제 시큐리티 필터 체인·`GlobalExceptionHandler`)는 그대로 탄다. 예외→envelope 매핑, `@CookieValue` 바인딩, `Set-Cookie` 속성까지 실제 파이프라인으로 검증된다.
- 인증이 필요한 엔드포인트는 로그인 사용자를 **`@WithMockAuthMember`**로 시큐리티 컨텍스트에 주입한다(`WithMockAuthMemberSecurityContextFactory`가 `AuthMember` 프린시펄을 구성). 컨트롤러는 `@LoginMember AuthMember`로 이를 받는다. (public 엔드포인트는 필터를 통과하므로 불필요)
- 응답 envelope는 `{ message, code, data }`이므로 `jsonPath("$.code")`, `jsonPath("$.data.xxx")`로 검증한다.

> 주의: `@Import` 목록과 `@MockitoBean` 협력자는 시큐리티/웹 배선을 따라간다. `SecurityConfig`에 의존이 늘거나 새 `@Component` 필터·인터셉터가 추가되면 이 목록도 갱신해야 한다(컨텍스트 로딩 실패로 바로 드러난다).

참고: `src/test/.../auth/AuthControllerTest.java`(`/reissue` 쿠키 테스트), `src/test/.../security/` (`WithMockAuthMember`, `WithMockAuthMemberSecurityContextFactory`).

## 요약 결정 트리

**무엇에 단언할지** (레이어 무관, 위에서부터):
- 반환값 있음 → **반환 DTO의 규칙 필드**에 assert.
- 포트 상태 변화 → **fake 조회로 결과 상태**에 assert (`verify` 아님).
- 예외 → **타입 + 에러코드**.
- 순수 부수효과(로그·이벤트)뿐 → **효과를 캡처해 assert** (`ListAppender` 등).
- 상호작용 검증(`verify`)은 원칙적으로 안 씀 — 호출 사실 자체가 계약인 순수 외부 경계에서만, 그마저 호출 기록 fake 우선.

**어떤 토폴로지로** (레이어별):
- 서비스/도메인 로직 → **fake 유닛 테스트** (`new Service(fakes...)`).
- JPA 쿼리/소프트삭제 → **`@DataJpaTest`** 슬라이스 (`@ActiveProfiles("test")` + `@Import`).
- 컨트롤러/시큐리티 → **`@WebMvcTest`(대상 컨트롤러) + `@Import`(`SecurityConfig` + 핸들러) + `@MockitoBean`(대상 서비스 및 필터·인터셉터 협력자)** (+ 인증 필요 시 `@WithMockAuthMember`).
- 트랜잭션 시맨틱(`noRollbackFor` 등) 실증 → `prod` 프로파일 `@SpringBootTest` (fake 유닛 테스트로는 행위 수준까지만 검증됨).
