-- MySQL 8.4
-- posts(created_at, deleted, blinded) 복합 인덱스를 생성한다.
-- 날짜 범위 검색(findActivePostPage, 키워드 없는 경로)이 PRIMARY 를 id DESC 로
-- 역방향 스캔하며 created_at 을 행 단위로 걸러내던 문제를 해결한다. 오래된 날짜
-- 구간일수록 심함 — 2023-07-01~2023-08-01 기준 EXPLAIN ANALYZE 로 실측: 11건을
-- 찾으려고 777,056행을 훑음(id DESC ≈ created_at DESC 로 시딩되어 있어 최신 구간은
-- 원래도 나쁘지 않지만, 과거 구간은 테이블 상단부터 거의 끝까지 훑어야 한다).
-- 선행: 05_posts (측정 대상 데이터가 이미 적재되어 있어야 의미 있는 비교가 된다)
--
-- 컬럼 순서: created_at 이 유일한 range 프레디케이트이자 가장 선택적인 컬럼이라 선두에
-- 둔다(2023-07 구간 기준 1,000,027행 중 10,218행 = 약 1%). deleted/blinded 는 각각
-- 95.96%/98.0% 가 0 이고 둘을 합쳐도 94.04% 가 남아 선두에 둬 봐야 B-tree seek 범위를
-- 거의 못 좁힌다 — 오히려 created_at range 를 뒤로 밀어내는 손해만 있다. created_at
-- 뒤에 붙이는 건 ICP(Index Condition Pushdown) 로 인덱스 단계에서 걸러 불필요한 PK
-- 룩업만 줄이는 저비용 보너스로만 취급한다.
--
-- 주의: ORDER BY id DESC 는 이 인덱스로도 "공짜"가 되지 않는다. WHERE 의 range 가
-- id 가 아닌 created_at 에 걸려 있는 이상(B-tree 는 range 경계를 한 번만 seek 에 쓸 수
-- 있다), 인덱스 설계로 필터링과 전역 정렬을 동시에 만족시킬 방법은 없다 — 구조적 한계.
-- 이 인덱스는 필터링 비용을 "테이블 전체" 에서 "매치된 행 수 K" 로 낮출 뿐이고,
-- filesort 는 K 건에 대해 LIMIT-aware top-N 힙으로 수행된다(비용은 LIMIT 이 아니라
-- K 에 비례).

ALTER TABLE posts
    ADD INDEX idx_posts_created_at_deleted_blinded (created_at, deleted, blinded);

ANALYZE TABLE posts;

SHOW INDEX FROM posts WHERE Key_name = 'idx_posts_created_at_deleted_blinded';

-- 검증: type=range, key=idx_posts_created_at_deleted_blinded,
-- rows(실측) ≈ K(매치 건수, LIMIT 아님), Extra 에
-- "Using index condition; Using where; Using filesort" 가 뜨는지 확인.
-- EXPLAIN ANALYZE SELECT p.* FROM posts p
--     WHERE created_at >= '2023-07-01' AND created_at < '2023-08-01'
--       AND deleted = 0 AND blinded = 0
--     ORDER BY id DESC LIMIT 11;
