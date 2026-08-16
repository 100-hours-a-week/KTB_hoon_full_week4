# `created_at` 범위 검색 B-Tree 인덱스 실험

성능 비교 실험 기록이다. 프로덕션 기능 변경이 아니며, 이 문서 자체도 실험 결과 아카이빙 목적이다.

## 문제

`findActivePostPage`(키워드 없는 검색/목록 경로)의 날짜 범위 조건에 대응하는 인덱스가 없어 `PRIMARY(id)`만으로 처리되고 있었다.

```sql
SELECT p.* FROM posts p
WHERE created_at >= :from AND created_at < :to
  AND deleted = 0 AND blinded = 0
ORDER BY id DESC
LIMIT 11
```

`EXPLAIN ANALYZE`(bench-mysql, 1,000,027건, `created_at`은 `bench/sql/05_posts.sql`이 최신 편향으로 시딩):

```sql
WHERE created_at >= '2023-07-01' AND created_at < '2023-08-01'
```
→ `id DESC`로 PRIMARY를 역순 스캔하며 **777,056행**을 훑어야 11건을 찾음. 최근 구간은 id/created_at이 대략 맞물려 있어 덜 심하지만, 과거 구간일수록 악화된다.

실측 선택도:
- `created_at ∈ [2023-07-01, 2023-08-01)`: 10,218 / 1,000,027 (**약 1%**, 매우 선택적)
- `deleted=0`: 95.96%, `blinded=0`: 98.0%, 둘 다: 94.04% (**거의 안 걸러짐**)

## 인덱스 설계

```sql
ALTER TABLE posts
    ADD INDEX idx_posts_created_at_deleted_blinded (created_at, deleted, blinded);
```

- **`created_at`을 선두에**: B-tree는 range 프레디케이트 하나로만 스캔 경계를 좁힐 수 있는데, 이 쿼리의 range는 `created_at`에 걸려 있고 실측상 가장 선택적(~1%)이다.
- **`deleted`/`blinded`를 선두에 두지 않은 이유**: 동등성 프리픽스로 거의 안 좁혀진다(94% 통과). 오히려 `created_at` range를 뒤로 밀어내는 손해만 생긴다. `created_at` 뒤에 trailing 컬럼으로 붙여 ICP(Index Condition Pushdown)로 인덱스 단계에서 걸러 불필요한 PK 룩업만 줄이는 저비용 보너스로 취급했다.
- **`id`를 명시적으로 넣지 않은 이유**: InnoDB는 모든 보조 인덱스에 PK를 암묵적으로 붙인다. 명시적으로 추가해도 `ORDER BY id DESC`를 "공짜"로 만들지는 못한다(아래 참고).
- **covering 인덱스를 고려하지 않은 이유**: 쿼리가 `Post` 전체 로우(TEXT 컬럼 `content` 포함) + `JOIN FETCH member`가 필요해 커버링이 애초에 불가능하다.

**`ORDER BY id DESC`와 `created_at` range의 관계 — 구조적 한계**: WHERE의 range가 정렬 컬럼(`id`)이 아닌 다른 컬럼(`created_at`)에 걸려 있으면, "range로 좁히기"와 "정렬 순서로 걷기"를 동시에 만족하는 인덱스 순회는 없다. (A) PK 순서로 걸으며 행 단위로 필터링(과거 구간엔 거의 풀스캔) vs (B) 새 인덱스로 `created_at` range만 먼저 좁힌 뒤 매치된 K건을 `id`로 filesort — 둘 중 하나를 반드시 선택해야 한다. filesort는 회귀가 아니라 예상된 동작이며(MySQL 8 LIMIT-aware top-N 힙 정렬), 비용은 `LIMIT`이 아니라 매치 건수 K에 비례한다.

## 적용 및 검증

`Post.java`에 `@Table(indexes = {@Index(name = "idx_posts_created_at_deleted_blinded", columnList = "created_at, deleted, blinded")})` 추가(`RefreshToken.java` 패턴), `bench/sql/07_created_at_index.sql`로 bench DB에 수동 적용.

`FORCE INDEX`로 강제한 결과, 설계 예측과 정확히 일치했다:

| | PRIMARY 역순 스캔 | `idx_posts_created_at_deleted_blinded` (강제) |
|---|---|---|
| 실행 시간 | 349ms | 29.7ms (**약 11.7배**) |
| 스캔 행수(actual rows) | 777,056 | 10,218 |
| Extra | `Using where` | `Using index condition; Using where`, `Sort: p.id DESC, limit input to 11 row(s) per chunk` |

## 예상 밖 발견: 옵티마이저가 힌트 없이는 새 인덱스를 선택하지 않는다

인덱스 생성 후 **힌트 없이** 동일 쿼리를 실행하면 옵티마이저가 여전히 `PRIMARY` 역순 스캔을 선택한다:

```
-> Limit: 11 row(s)  (cost=12.9 rows=2.75) (actual time=349..349 rows=11 loops=1)
    -> Filter: (...)  (cost=12.9 rows=2.75) (actual time=349..349 rows=11 loops=1)
        -> Index scan on p using PRIMARY (reverse)  (cost=12.9 rows=534) (actual time=0.0323..309 rows=777056 loops=1)
```

원인은 `ORDER BY id DESC LIMIT 11`을 PRIMARY 역순 스캔으로 만족시킬 때 옵티마이저의 비용 추정이 실측과 크게 어긋나기 때문이다(추정 534행 vs 실측 777,056행, cost=12.9 vs 강제 인덱스 경로의 cost=24123). `ORDER BY <PK> DESC LIMIT n` + PK 외 컬럼 range 필터 조합에서 MySQL 8.4 옵티마이저가 PRIMARY 역순 스캔 비용을 구조적으로 과소평가하는 사각지대로 보인다.

range 폭을 바꿔가며 확인한 결과, 옵티마이저의 자연 선택은 range 폭에 따라 갈렸다:

| 구간 | 자연 선택 | 비고 |
|---|---|---|
| 2023-07 (1개월, 과거) | `PRIMARY` (인덱스 미사용) | 최초 보고 케이스 |
| 2025-06 (1개월, 최근) | `PRIMARY` (인덱스 미사용) | |
| 2023-01-01~08 (1주, 과거) | **새 인덱스 자연 선택** | 좁은 range에서는 비용 추정이 뒤집힘 |

즉 인덱스 설계 자체는 유효하고 강제 적용 시 확실한 개선을 보이지만, 월 단위처럼 흔히 쓰일 법한 넓은 range에서는 옵티마이저가 자동으로 채택하지 않는다.

## 운영(RDS)에서의 재현

인덱스가 적용된 운영 RDS 에서 같은 구간(`2023-07-01 ~ 2023-08-01`)을 HTTP 로 측정한 결과 **6.39초**가 나왔다.
같은 요청의 최근 구간(`2026-08-01 ~ 2026-08-15`)은 0.12초다. 인덱스가 쓰였다면 10,218행만 보면 되므로 나올 수 없는 값이며,
**옵티마이저가 힌트 없이 인덱스를 선택하지 않는 위 현상이 RDS 에서도 그대로 재현됨**을 뜻한다.
(로컬 bench 의 같은 계획이 306ms/263ms 였으므로 RDS 가 약 24배 느리다. 측정 상세는 `docs/search/fulltext-search-experiment.md` 참고.)

## 결론 및 처리 방침

이 결과는 **실험 기록으로만 남긴다.** `FORCE INDEX`나 옵티마이저 힌트는 프로덕션 쿼리(`JpaSearchRepository`)에 추가하지 않았다 — 인덱스(`Post.java`의 `@Index`, `bench/sql/07_created_at_index.sql`)만 실제 변경 사항으로 유지하고, 옵티마이저의 자연스러운 선택 여부는 향후 별도 판단이 필요한 사안으로 미해결 상태로 둔다.
