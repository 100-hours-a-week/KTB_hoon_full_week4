-- MySQL 8.4
-- posts(title, content) 에 ngram 파서 기반 FULLTEXT 인덱스를 생성한다.
-- LIKE '%keyword%' 와 MATCH...AGAINST 성능 비교 벤치마크용.
-- 선행: 05_posts (측정 대상 데이터가 이미 적재되어 있어야 의미 있는 비교가 된다)
--
-- 주의: posts 에 걸리는 첫 FULLTEXT 인덱스라 InnoDB 가 내부적으로 FTS_DOC_ID 보조 테이블을
-- 만들며 테이블을 재구성한다(ALGORITHM=INPLACE 지만 가벼운 메타데이터 변경이 아님).
-- 1,000,027건/397MB 기준 단순 인덱스보다 훨씬 오래 걸릴 수 있으니 벤치마크 실행과
-- 동시에 돌리지 않는다.

SHOW VARIABLES LIKE 'ngram_token_size';  -- 2 여야 한다(bench/my.cnf). 다르면 여기서 멈추고 확인.

ALTER TABLE posts
    ADD FULLTEXT INDEX ft_posts_title_content (title, content) WITH PARSER ngram;

ANALYZE TABLE posts;

SHOW INDEX FROM posts WHERE Key_name = 'ft_posts_title_content';
