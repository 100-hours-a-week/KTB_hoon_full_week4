# 날짜 범위 검색이 느린 문제 — 고치는 과정 기록

운영에서 날짜 범위 검색이 6.4초 걸리는 걸 발견하고, 원인을 찾아 `ORDER BY` 를 바꿔봤다가
또 다른 문제를 만난 과정을 순서대로 적는다. **아직 해결 못 했고, 현재 상태까지 기록한다.**

관련 문서
- `docs/created-at-index-experiment.md` — `(created_at, deleted, blinded)` 인덱스를 만들 때의 실험
- `docs/fulltext-search-experiment.md` — 키워드 검색 34초 문제 (별개 사건)

## 측정 환경

| 항목 | 내용 |
|---|---|
| 대상 | 운영 (EC2 → AWS RDS MySQL 8) |
| 데이터 | `bench/sql` 로 만든 모집글 100만 건 규모 |
| 인덱스 | `idx_posts_created_at_deleted_blinded`, `ft_posts_title_content` 둘 다 적용됨 |
| 페이지 | `size=10` → `LIMIT 11` |

---

## 1. 문제 발견

운영에 요청을 보내보니 **과거 날짜로 검색할 때만** 느렸다.

| 요청 | 응답 시간 |
|---|---|
| `?from=2026-08-01&to=2026-08-15` (최근 2주) | **0.12초** |
| `?from=2023-07-01&to=2023-08-01` (3년 전 1개월) | **6.39초** |

같은 크기의 기간인데 50배 차이가 났다. 기간 폭이 아니라 **얼마나 과거인지**가 문제였다.

## 2. 원래 쿼리

키워드 없는 검색은 `JpaSearchRepository.findActivePostPage` (JPQL) 를 탄다.

```sql
SELECT p.* FROM posts p
JOIN members m ON m.id = p.member_id
WHERE p.created_at >= '2023-07-01' AND p.created_at < '2023-08-01'
  AND p.deleted = 0 AND p.blinded = 0
ORDER BY p.id DESC
LIMIT 11;
```

## 3. 실행 계획을 봤다

```
-> Limit: 11 row(s)  (cost=9.83) (actual time=4944..4944 rows=11)
    -> Filter: (blinded=0 and deleted=0 and created_at >= ... and created_at < ...)
                                          (actual time=4942..4942 rows=11)
        -> Index scan on p using PRIMARY (reverse)
                                          (cost=9.83) (actual time=8.31..4790 rows=777756)
```

**11건을 반환하려고 777,756행을 읽고 있었다.** 그리고 `idx_posts_created_at_deleted_blinded` 를
만들어뒀는데 안 쓰고 `PRIMARY` 를 쓰고 있었다.

## 4. 원인 — 필터 기준과 정렬 기준이 다르다

MySQL 이 고를 수 있는 접근 경로는 두 개였고, 각각 한쪽이 비쌌다.

| 접근 경로 | 정렬 (`ORDER BY id DESC`) | 필터 (`created_at` 범위) |
|---|---|---|
| **`PRIMARY` 역순** ← 선택됨 | 공짜 (PK 순서 = `id` 순서) | **행마다 확인 → 777,756행** |
| `idx_posts_created_at...` | 불가 → 정렬 필요 | 공짜 (범위만 걸으면 됨) |

인덱스는 하나만 고를 수 있다. MySQL 은 정렬을 공짜로 받는 쪽을 골랐고, 그 대가로 필터를 비싸게 치렀다.
2023년 7월 구간에 닿을 때까지 최신 글부터 계속 내려온 것이다.

**정렬 때문에 느린 게 아니라, 정렬을 공짜로 만들려다 필터가 비싸진 것**이다.
실행 계획에 `Sort` 노드가 없는 게 그 증거다.

## 5. 인덱스가 없는 줄 알았는데 있었다

처음엔 인덱스가 배포에 반영이 안 된 줄 알고 다시 걸어봤다.

```sql
ALTER TABLE posts ADD INDEX idx_posts_created_at_deleted_blinded (created_at, deleted, blinded);
-- ERROR 1061 (42000): Duplicate key name
```

이미 있었다. 즉 **인덱스가 없어서가 아니라, 있어도 옵티마이저가 안 고르는 상황**이었다.
`created-at-index-experiment.md` 에 로컬에서 관찰해뒀던 현상이 운영에서도 그대로 재현된 것이다.

## 6. `ORDER BY` 를 바꿔봤다

필터 기준(`created_at`)과 정렬 기준(`id`)이 다른 게 문제니, 정렬 기준을 필터 쪽에 맞춰봤다.

```sql
ORDER BY p.created_at DESC, p.id DESC   -- 기존: ORDER BY p.id DESC
```

```
-> Limit: 11 row(s)  (cost=18925) (actual time=1.19..1.24 rows=11)
    -> Index range scan on p using idx_posts_created_at_deleted_blinded (reverse),
       with index condition: (blinded=0 and deleted=0 and created_at >= ... and created_at < ...)
                                          (actual time=0.083..0.137 rows=11)
```

| | 접근 경로 | 읽은 행 | 시간 |
|---|---|---|---|
| 기존 | `PRIMARY` 역순 | 777,756 | **4,944ms** |
| 변경 | `idx_posts_created_at...` 범위 역순 | **11** | **1.24ms** |

**약 4,000배.** 그리고 `Filter` 노드가 사라지고 `with index condition` 이 붙었다.
`deleted`/`blinded` 검사가 행을 꺼내지 않고 인덱스 단계에서 처리된 것이다.

인덱스가 `created_at` 순으로 정렬돼 있으니, 요청 순서도 `created_at` 순이면 정렬할 필요 없이
**인덱스를 역방향으로 11칸 걷는 게 전부**가 된다.

### 뒤에 `, p.id DESC` 를 붙인 이유

`ORDER BY p.created_at DESC` 만 써도 실행 계획은 같았다(4.95ms). 그런데 데이터를 보면
작성일이 완전히 같은 글이 무더기로 있다.

```
1000026 | 2026-08-09 12:20:12.005829
1000025 | 2026-08-09 12:20:12.005829
1000024 | 2026-08-09 12:20:12.005829   ... 14건이 같은 시각
```

작성일만으로 정렬하면 이 글들의 순서가 요청마다 달라질 수 있고, 그러면 페이지를 넘길 때
어떤 글은 두 번 나오고 어떤 글은 빠진다. `, p.id DESC` 가 "같은 시각이면 번호 큰 순" 을 고정한다.
인덱스에 이미 PK 가 붙어 있어서 추가 비용은 없다.

### 옵티마이저가 똑똑해진 건 아니다

추정 비용을 보면 오히려 반대다.

| | 추정 cost | 실제 |
|---|---|---|
| 기존 (`PRIMARY` 역순) | **9.83** | 4,944ms |
| 변경 (인덱스 범위) | **18,925** | 1.24ms |

MySQL 은 새 계획이 1,900배 비싸다고 추정했다. 그런데 실제로는 4,000배 빨랐다.
즉 옵티마이저를 설득한 게 아니라 **`ORDER BY id DESC` 라는 선택지를 없애서 다른 길밖에 못 가게 만든 것**이다.
`PRIMARY` 역순 스캔을 과소평가하는 문제는 그대로 남아 있다.

## 7. 새 문제 ① — 정렬 기준을 바꾸면 결과가 달라지나?

`ORDER BY id DESC` 와 `ORDER BY created_at DESC` 가 같은 순서를 만들려면
**번호가 클수록 작성일도 뒤여야** 한다. 확인해봤다.

```sql
SELECT id, created_at FROM posts WHERE deleted=0 AND blinded=0 ORDER BY created_at DESC LIMIT 15;
```

| `created_at DESC` | `id DESC` |
|---|---|
| 1048588, 1048587 | 1048588, 1048587 |
| **26, 25, 24, 23...** | 1000026, 1000025, 1000024... |

**깨져 있었다.** `id` 14~26번 글이 `2026-08-09 18:58` 인데 벤치 데이터 100만 건의 최신은 `12:20` 이었다.
번호는 제일 작은데 작성일은 제일 최신인 글들이 있었던 것이다. 예전에 넣은 시드용 더미 데이터였다.

이대로 `ORDER BY` 만 바꾸면 페이지네이션이 깨진다. 1페이지 끝이 `id 19` 면 다음 페이지는
`WHERE id < 19` 라서 **벤치 데이터 100만 건이 통째로 안 보이게 된다.**

더미 데이터의 작성일을 과거로 옮겨서 정리했다.

```sql
UPDATE posts SET created_at = TIMESTAMP'2021-01-01 00:00:00' + INTERVAL id SECOND WHERE id <= 26;
```

정리 후 전체를 검사했다.

```sql
SELECT COUNT(*) AS 역전건수 FROM (
  SELECT created_at, LAG(created_at) OVER (ORDER BY id) AS prev FROM posts
) t WHERE created_at < prev;
-- 0
```

역전 0건. 이제 두 정렬이 같은 순서를 만든다.

## 8. 새 문제 ② — 커서가 인덱스를 못 탄다

여기가 지금 막혀 있는 지점이다.

우리 목록 조회는 커서 페이지네이션이라 `WHERE p.id < :cursor` 가 붙는다.
그런데 `ORDER BY` 를 `created_at` 기준으로 바꾸면, **커서 기준(`id`)과 정렬 기준(`created_at`)이 다시 어긋난다.**

```sql
-- 커서가 깊은 경우
WHERE deleted = 0 AND blinded = 0 AND id < 100000
ORDER BY created_at DESC, id DESC LIMIT 11;
```

| | 접근 경로 | 읽은 행 | 시간 |
|---|---|---|---|
| 기존 (`ORDER BY id DESC`) | `PRIMARY` 범위 역순 — **커서 위치로 바로 점프** | 11 | **0.82ms** |
| 변경 (`ORDER BY created_at DESC, id DESC`) | `idx_created_at` 역순 — 최신부터 훑으며 버림 | **900,040** | **9,850ms** |

**12,000배 느려졌다.** `created_at` 인덱스에는 `id` 로 점프할 방법이 없어서,
최신 글부터 걸으면서 `id < 100000` 인 글이 나올 때까지 90만 건을 버린 것이다.

조건 없는 목록 조회도 조금 느려졌다.

| | 접근 경로 | 읽은 행 | 시간 |
|---|---|---|---|
| 기존 | `PRIMARY` 역순 | 12 | **0.73ms** |
| 변경 | `idx_created_at` 역순 | 15 | **17.4ms** |

`PRIMARY` 는 행 자체가 그 순서로 저장돼 있어서 읽으면 바로 데이터가 나오는데,
보조 인덱스는 "어느 행인지"만 알려줘서 `p.*` 를 채우려면 행마다 따로 찾아가야 한다.
(cold 측정일 수 있어 재측정 필요)

### 버리는 행 수는 커서 깊이에 비례한다

| 커서 위치 | 버리는 행 | 대략 |
|---|---|---|
| 2페이지 | 10 | 무시 가능 |
| 100페이지 | 1,000 | 수 ms |
| `id < 100000` | 900,040 | **9.9초** |

정상적인 페이지 넘기기는 문제없다. **문제는 커서가 사용자 입력이라는 점**이다.
`?cursor=100000` 을 직접 넣으면 누구나 10초짜리 쿼리를 만들 수 있다.
지금 구조는 어떤 커서 값이 와도 `PRIMARY` 로 바로 점프해서 O(1) 인데, 바꾸면 그 보호가 사라진다.

## 9. 해결 — 커서도 정렬 기준에 맞춘다 (복합 커서)

원인을 한 줄로 줄이면 계속 같은 말이 나왔다.

> **정렬 기준과 커서 기준이 다르면 인덱스를 못 쓴다.**

- 처음 문제: 필터는 `created_at`, 정렬은 `id` → 어긋남
- `ORDER BY` 를 고친 뒤: 정렬은 `created_at`, 커서는 `id` → 또 어긋남

그러면 커서도 `created_at` 으로 맞추면 된다.

### `created_at` 하나로는 부족하다

같은 시각 글이 무더기로 있기 때문이다(6절 참고, 14건이 같은 마이크로초).
1페이지 마지막이 `id 1000020` 일 때 작성일만으로 커서를 주면 이렇게 된다.

| 다음 페이지 조건 | 결과 |
|---|---|
| `created_at < 커서작성일` | 같은 시각인 `1000019~1000013` **7건 누락** |
| `created_at <= 커서작성일` | 앞 페이지에서 보여준 11건 **중복** |

작성일만으로는 "같은 시각 14건 중 어디까지 봤는지" 를 표현할 수 없다. 그래서 `id` 를 같이 쓴다.

### 조건식

```sql
WHERE (:cursorCreatedAt IS NULL
       OR p.createdAt < :cursorCreatedAt
       OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId))
ORDER BY p.createdAt DESC, p.id DESC
```

> 작성일이 더 과거인 글 **이거나**, 작성일이 같고 번호가 더 작은 글

인덱스의 실제 정렬 키가 `(created_at, deleted, blinded, id)` 이고 `deleted`/`blinded` 는 등치로 고정되므로,
남는 순서가 `(created_at, id)` 다. 커서가 그 두 값을 다 주니 **인덱스에서 위치를 바로 찍을 수 있다.**

### 코드 변경

| 파일 | 내용 |
|---|---|
| `api/domain/search/SearchCursor.java` | 신규. `(createdAt, id)` 레코드 + base64url 인코딩/디코딩 |
| `api/domain/search/SearchErrorCode.java` | `INVALID_CURSOR` 추가 |
| `api/repository/search/PostSearchCond.java` | `Long cursor` → `LocalDateTime cursorCreatedAt` + `Long cursorId` |
| `.../search/jpa/JpaSearchRepository.java` | `FILTERS`·`NATIVE_FILTERS` 의 커서 조건과 `ORDER BY` |
| `.../search/jpa/JpaSearchRepositoryAdapter.java` | 파라미터 전달 |
| `.../search/inmemory/InMemorySearchRepository.java` | 정렬·커서 로직 동일하게 |
| `api/service/SearchService.java` | 커서 문자열 디코딩 |
| `api/controller/SearchController.java` | `cursor` 파라미터 `Long` → `String` |
| `api/dto/response/PostSummaryPageResDto.java` | `nextCursor` `Long` → `String`(인코딩) |
| `src/test/.../search/fake/FakeSearchRepository.java` | InMemory 와 동일하게 |

키워드 경로(`NATIVE_FILTERS`)도 같이 바꿨다. 성능 이득은 없지만
**두 경로가 서로 다른 정렬 계약을 쓰면 키워드 유무에 따라 순서가 달라질 수 있어서** 맞췄다.

### API 계약 변경

```json
"nextCursor": 1000020                              // 전
"nextCursor": "MjAyNi0wOC0wOVQxMjoyMDoxMi4wMDU4Mjlf..."   // 후
```

**불투명 문자열로 감쌌다.** 프론트가 값을 해석하지 않고 그대로 되돌려주기만 하면 되고,
나중에 커서 내용이 바뀌어도 계약이 안 깨진다. `docs/api-specification.md` 3.1 도 함께 갱신했다.

## 10. 정렬 옵션을 늘리면 (인기순·조회수순)

지금은 최신순 하나뿐이라 문제가 없지만, 정렬을 늘리면 얘기가 달라진다. 미리 정리해둔다.

**① 정렬마다 인덱스가 필요하다.** `ORDER BY like_count DESC` 로 하면 `created_at` 인덱스는
쓸모가 없다. 그리고 `view_count` 는 글을 볼 때마다 바뀌므로 그 인덱스는 갱신이 매우 잦아진다.

**② 정렬 키가 변하면 커서가 무너진다.** 이게 더 큰 문제다.
`created_at` 은 불변이라 커서가 안정적이지만 `like_count` 는 계속 변한다.

| 상황 | 결과 |
|---|---|
| 2페이지에 있던 글의 좋아요가 커서 값보다 커짐 | 조건에 안 걸림 → **영영 안 보임** |
| 1페이지에서 보여준 글의 좋아요가 떨어짐 | 2페이지 조건에 걸림 → **중복** |

**커서 페이지네이션은 "정렬 키가 변하지 않는다"를 전제로 동작한다.** 변하는 값으로 정렬하면 전제가 깨진다.

그래서 선택지는 대략 이렇게 된다 — 불변 값만 커서로 쓰거나, offset 페이지네이션(중복·누락 감수)을 쓰거나,
인기순은 "TOP N" 으로만 주고 페이지네이션을 안 하거나, 랭킹을 주기적으로 스냅샷 떠서 그걸로 페이지네이션하거나,
검색엔진에 맡기거나.

**지금은 최신순만 지원하고 판단을 미룬다.** 대신 `nextCursor` 를 불투명 문자열로 만들어뒀으므로,
나중에 어떤 방식을 택하든 API 계약은 그대로 둘 수 있다.

## 아직 확인 안 한 것

- **복합 커서가 실제로 인덱스를 타는지 미측정.** 특히 `(:cursorCreatedAt IS NULL OR ...)` 패턴이
  파라미터 바인딩 상태에서도 range scan 으로 풀리는지 확인해야 한다. 지금까지의 측정은 전부
  리터럴을 박은 SQL 이었고, 앱이 보내는 건 `?` 바인딩이라 실행 계획이 다를 수 있다.
- 코드 반영 후 HTTP 응답 시간 (지금까지 전부 SQL 측정)
- 조건 없는 목록 조회 17.4ms 가 cold 때문인지 (warm 재측정 필요)
- 벤치 데이터를 다시 적재하면 7절의 역전이 또 생긴다. 그때마다 정리해줘야 하는지, 스크립트를 고칠지
- 커서 경로를 검증하는 테스트가 없다. `FakeSearchRepository` 는 있지만 쓰는 테스트가 없다
