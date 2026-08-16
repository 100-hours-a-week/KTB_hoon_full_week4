# Caffeine 블랙리스트 — 만료가 동작하는 원리

`CaffeineTokenBlacklist`(jti 기준)와 `CaffeineSessionBlacklist`(familyId 기준)가 어떻게 엔트리를 만료시키는지 정리한 문서다. 두 클래스는 키 이름과 로그 문구만 다르고 구조가 같다.

## 왜 직접 만료 정책을 구현했나

블랙리스트에 올라간 항목은 **원래 토큰이 만료되는 시각까지만** 막으면 된다. 그 시각이 지나면 토큰 자체가 무효라 블랙리스트에 남겨둘 이유가 없다. 그런데 항목마다 남은 수명이 제각각이다 — 방금 발급된 AT는 600초, 9분 전에 발급된 AT는 60초.

그래서 모든 엔트리에 같은 기간을 주는 `expireAfterWrite(10분)`을 못 쓰고, 엔트리별 TTL을 계산하는 `Expiry`를 직접 구현했다. 캐시 타입이 `Cache<String, Long>`인데 이 **값(`Long`)이 곧 만료 시각(epoch millis)**이라, 값만 보면 수명을 계산할 수 있다.

정리 스케줄러(`@Scheduled`)를 따로 두지 않는 이유도 이것이다. 각 엔트리가 자기 만료 시각에 스스로 빠진다.

## Ticker — 캐시가 시간을 읽는 통로

Caffeine은 만료를 판단할 때 `System.nanoTime()`을 직접 부르지 않고 항상 `ticker.read()`를 거친다. 기본값은 `Ticker.systemTicker()`다.

돌려주는 나노초 값은 **기준점이 임의라 절대 시각이 아니다.** "몇 시 몇 분"으로 환산할 수 없고 두 값의 차이(경과 시간)만 의미가 있다. 손목시계가 아니라 스톱워치에 가깝다.

`nanoTime` 계열을 쓰는 이유는 벽시계(`currentTimeMillis`)가 NTP 동기화나 서머타임으로 **뒤로 갈 수 있어서** 경과 측정에 쓸 수 없기 때문이다.

## 시계가 두 개인 이유

`ttlNanos`는 벽시계를 보고, 만료 판단은 ticker가 한다. 섞여 있는 게 아니라 각자 할 수 있는 일만 한다.
벽시계는 `System.currentTimeMillis()`를 직접 부르지 않고 주입받은 `Clock` 빈(`ClockConfig`)을 쓴다.

```
등록 순간 : 벽시계로  expiresAt - clock.millis() = 남은 기간
           → 나노초로 변환해 Caffeine 에 반환
그 이후   : Caffeine 이 (ticker 현재 눈금 + 기간) 을 만료 눈금으로 기억
           이후 판단은 전부 ticker 눈금과의 비교
```

| 필요한 것 | 쓰는 시계 | 이유 |
|---|---|---|
| 절대 시각 비교 (JWT `exp`가 언제인가) | 벽시계 | 단조 시계는 기준점이 임의라 비교 불가 |
| 경과 측정 (600초 지났나) | ticker | 벽시계는 뒤로 갈 수 있음 |

지켜야 할 선은 하나다 — **두 시계의 값을 직접 비교하지 않는다.** ticker 나노초와 epoch millis를 빼면 무의미한 숫자가 나온다. 현재 코드는 벽시계 쪽에서 **기간**만 만들어 넘기고, 기간은 두 시계 모두가 이해하는 공통 단위라 안전하게 건너간다.

그래서 벽시계는 **등록 순간 딱 한 번** 쓰이고 그 뒤로 등장하지 않는다. 등록 이후 서버 시각이 틀어져도 이미 잡힌 만료는 흔들리지 않는다.

콜백의 `currentTime` 파라미터가 ticker 값인데, 위 이유로 쓸모가 없어 무시한다.

## Expiry의 세 콜백

| 콜백 | 호출 시점 | 반환값 | 효과 |
|---|---|---|---|
| `expireAfterCreate` | 새로 등록 | `ttlNanos(expiresAt)` | 만료 시점 설정 |
| `expireAfterUpdate` | 같은 키 재등록 | `ttlNanos(expiresAt)` | 값이 바뀌었으므로 만료 시점 갱신 |
| `expireAfterRead` | 조회 | `currentDuration` 그대로 | **연장하지 않음** |

`expireAfterRead`가 의도가 담긴 자리다. 여기서 `ttlNanos`를 다시 계산하면 조회할 때마다 수명이 되살아나, **자주 확인되는 세션일수록 더 오래 차단되는** 이상한 동작이 된다. Caffeine이 알려주는 남은 기간(`currentDuration`)을 그대로 돌려줘 "등록 시점 기준으로만 만료"를 지킨다.

`ttlNanos`의 `Math.max(0, remain)`은 이미 지난 시각으로 등록하는 경우를 처리한다. TTL 0이면 곧바로 만료된다.

밀리초를 나노초로 바꾸는 것은 ticker 단위에 맞추기 위해서다. 밀리초 그대로 넘기면 600초가 아니라 0.0006초 뒤에 만료된다.

## 언제 실제로 제거되나

Caffeine은 **별도 정리 스레드가 없다.** 만료 시각은 타이머 휠에 넣어두고, 읽기·쓰기 연산 뒤에 유지보수 작업을 태워 보내면서 그때 휠을 굴려 만료된 엔트리를 걷어낸다. 게다가 매 연산마다 도는 것도 아니다 — 링 버퍼에 기록만 남기고 조건이 맞을 때 한 스레드가 몰아서 처리한다.

즉 **누가 캐시를 건드려야만 정리된다.** 블랙리스트는 트래픽이 없으면 아무도 건드리지 않으므로, 로그아웃이 뜸한 시간대에는 만료된 엔트리가 몇 시간이고 힙에 남는다.

그래서 `Scheduler.systemScheduler()`를 켜서 정리할 계기를 트래픽 말고 하나 더 만들었다.

### 역할 분담

| 역할 | 누가 | 아는 것 |
|---|---|---|
| 시간표 | 타이머 휠 (캐시마다 따로) | 모든 엔트리의 만료 시각 |
| 알람 | JDK 공용 데몬 스레드 (`CompletableFutureDelayScheduler`) | `(언제, 실행할 작업)`뿐. 어떤 엔트리인지 모름 |
| 정리 | `ForkJoinPool.commonPool` 워커 | 지시받은 뒤 휠을 뒤져 치울 것을 정함 |

### 순서

1. 유지보수가 끝날 때마다 휠에 "다음 만료까지 얼마 남았나"를 묻는다 (`getExpirationDelay()`)
2. 그 값으로 알람을 건다 — **우리 캐시 몫은 항상 하나**(가장 이른 것)
3. 시간이 되면 데몬 스레드가 깨어나 워커에 유지보수 작업(`drainBuffersTask`)을 던진다
4. 워커가 그 시점까지 만료된 엔트리를 **한꺼번에** 제거하고 `removalListener`를 호출한다
5. 다시 1번으로. 남은 엔트리가 없으면 알람을 해제한다

`Scheduler.systemScheduler()`는 `CompletableFuture.delayedExecutor(...)`에 위임하므로 **JVM 전역에 데몬 스레드 하나**를 쓴다. 두 블랙리스트가 공유하고, 캐시를 늘려도 스레드는 늘지 않는다. 데몬은 던지기만 하고 실제 정리는 다른 스레드에서 돌기 때문에, 우리 로그 출력이 JVM 전체의 지연 작업을 밀지 않는다. 전용 스레드로 격리하려면 `Scheduler.forScheduledExecutorService(...)`로 바꾸면 된다.

내부적으로 `Pacer`가 최소 1.07초 간격으로 조절하고 알람을 하나만 유지한다. 그래서 "만료 즉시"가 아니라 **만료 시점 근처로 수렴**한다.

### 정확성 문제가 아니다

Scheduler가 없어도 만료된 엔트리를 조회하면 여전히 `null`이 나온다. 만료된 토큰이 통과하는 구멍은 생기지 않는다. 얻는 것은 두 가지다:

- **메모리** — 상한이 100,000이라 만료된 엔트리가 자리를 차지하면 아직 유효한 엔트리가 사이즈 축출로 밀려날 수 있다. `CacheUsageMonitor`의 "포화: 유효 엔트리 축출 위험" 경고가 가리키는 상황이다.
- **관측** — 만료 로그와 사용량 샘플링이 제때 찍힌다. 없으면 "누가 조회했을 때" 몰려 찍혀 실제 시각과 어긋난다.

한 줄로는 **예측 가능한 제거**다.

## 사용량 샘플링

`CacheUsageMonitor`는 캐시를 스스로 들여다보지 않는다. `check(currentSize)`로 알려줄 때만 등급(정상/경고/포화)을 다시 매기고, 등급이 바뀔 때만 로그를 남기는 엣지 트리거 방식이다.

그래서 **언제 알려주느냐가 곧 경보의 정확도**가 된다. 등록 시점(`add`)에만 알려주면 캐시가 차오를 때는 제때 올라가지만, TTL로 줄어들 때는 `add`가 불리지 않아 **다음 등록이 들어올 때까지 포화 상태에 머문다.** 이걸 알림으로 확장하면 이미 해소된 상태가 경보로 남는다.

그래서 `removalListener`에서도 크기를 잰다(`sampleUsage()`). 만료·사이즈 축출·명시적 제거를 가리지 않고 샘플링한다.

## 시간 기반 만료를 테스트하는 방법

`Thread.sleep(600_000)` 없이 검증하려면 **캐시가 시간을 읽는 통로인 `Ticker`를 갈아끼운다.** 두 클래스 모두 캐시 빌드를 생성자로 빼고 ticker와 상한을 받는 시드 생성자를 열어뒀다(package-private).

```java
@Autowired
public CaffeineSessionBlacklist(Clock clock) {
    this(clock, Ticker.systemTicker(), MAX_SIZE);
}

CaffeineSessionBlacklist(Clock clock, Ticker ticker, long maxSize) { ... }
```

`FakeTicker`는 숫자 하나를 들고 있다가 물어보면 돌려주는 가짜 시계이고, `advance(Duration)`는 그 숫자를 더한다.

```java
clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
ticker = new FakeTicker();
sessionBlacklist = new CaffeineSessionBlacklist(clock, ticker, MAX_SIZE);

sessionBlacklist.add("family-1", clock.millis() + 600_000);

ticker.advance(Duration.ofSeconds(599));   // 만료 직전
assertThat(sessionBlacklist.exists("family-1")).isTrue();
ticker.advance(Duration.ofSeconds(2));     // 만료 직후
assertThat(sessionBlacklist.exists("family-1")).isFalse();
```

- **벽시계는 등록 순간 한 번만** 쓰이므로 **만료 재현 자체는 ticker 교체만으로 된다.** `Clock`은 만료를 앞당기는 용도가 아니라,
  등록 시점의 기준 시각을 고정해 테스트를 결정적으로 만드는 용도다(`clock.millis() + TTL`).
- **경계에서 `advance(TTL)`로 딱 맞추지 않는다.** 등록과 `advance` 사이의 실제 경과(수 ms)가 TTL에서 이미 빠져 있어 플래키해진다. 앞뒤로 1초씩 여유를 둔다.
- `FakeTicker`로 눈금만 밀었을 때 Scheduler는 실제 시간 기준으로 돌기 때문에 도움이 되지 않는다. **조회가 만료를 발견하는 계기**가 된다.
- `removalListener`는 비동기(`ForkJoinPool.commonPool`)라, 만료 로그나 사용량 경보를 검증할 때는 Awaitility로 기다린다.
- 사용량 경보를 검증하려면 상한을 채워야 하므로 시드 생성자로 `maxSize`를 낮춘다. 실제 값은 100,000이다.

참고: `src/test/.../global/security/jwt/CaffeineSessionBlacklistTest.java`, `CaffeineTokenBlacklistTest.java`, 공용 더블은 `.../global/fake/` (`FakeTicker`, `MutableClock`)

## 한계

프로세스 로컬 캐시라 **재기동하면 전량 소실**되고 스케일 아웃 시 인스턴스별로 분리된다.

- 재기동하면 로그아웃 처리된 AT가 남은 수명(prod 최대 10분) 동안 다시 유효해진다. CD가 `main` push마다 컨테이너를 교체하므로 배포할 때마다 실제로 열리는 창이다.
- 스케일 아웃 시 A에서 로그아웃해도 B는 그 AT를 받아준다.

해소하려면 Redis 등 공유·영속 저장소로 옮겨야 한다. 레이트 리미터(`InMemoryPostRateLimiter`)가 같은 사유를 안고 있어 함께 옮기는 게 자연스럽다. 미해결이다.
