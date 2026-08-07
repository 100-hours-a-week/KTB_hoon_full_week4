-- MySQL 8.4
-- 본문 문서 1,000,000개. 글마다 본문이 다르고, 제목의 주제와 본문 어휘가 일치한다.
--
--   topic = 1 + MOD(CRC32(CONCAT('tb', n)), 24)  <- 05_posts.sql 의 제목 활동어와 같은 식
--   홀수 슬롯은 그 주제의 어휘, 짝수 슬롯은 공통어를 뽑는다.
--   그래서 '테니스' 로 검색하면 실제로 테니스 글이 나온다.

DROP TABLE IF EXISTS bench_docs;
CREATE TABLE bench_docs (
    doc_id INT NOT NULL PRIMARY KEY,
    topic  INT NOT NULL,
    body   VARCHAR(400) NOT NULL
) ENGINE=InnoDB;

INSERT INTO bench_docs (doc_id, topic, body)
SELECT
    d.n,
    d.topic,
    CONCAT(CONCAT_WS(' ', w01.word, w02.word, w03.word, w04.word, w05.word, w06.word, w07.word, w08.word, w09.word, w10.word, w11.word, w12.word, w13.word, w14.word, w15.word, w16.word, IF(d.len_words >= 17, w17.word, NULL), IF(d.len_words >= 18, w18.word, NULL), IF(d.len_words >= 19, w19.word, NULL), IF(d.len_words >= 20, w20.word, NULL), IF(d.len_words >= 21, w21.word, NULL), IF(d.len_words >= 22, w22.word, NULL), IF(d.len_words >= 23, w23.word, NULL), IF(d.len_words >= 24, w24.word, NULL), IF(d.len_words >= 25, w25.word, NULL), IF(d.len_words >= 26, w26.word, NULL), IF(d.len_words >= 27, w27.word, NULL), IF(d.len_words >= 28, w28.word, NULL), IF(d.len_words >= 29, w29.word, NULL), IF(d.len_words >= 30, w30.word, NULL), IF(d.len_words >= 31, w31.word, NULL), IF(d.len_words >= 32, w32.word, NULL), IF(d.len_words >= 33, w33.word, NULL), IF(d.len_words >= 34, w34.word, NULL), IF(d.len_words >= 35, w35.word, NULL), IF(d.len_words >= 36, w36.word, NULL)), '.')
FROM (
    SELECT n,
           1 + MOD(CRC32(CONCAT('tb', n)), 24) AS topic,
           16 + MOD(CRC32(CONCAT('len:', n)), 21) AS len_words
    FROM numbers
    WHERE n <= 1000000
) d
JOIN bench_topic_pool_size ts ON ts.topic = d.topic
JOIN bench_topic_pool_size cs ON cs.topic = 0
JOIN bench_word_pool w01 ON w01.topic = d.topic AND w01.slot = 1 + MOD(CRC32(CONCAT(d.n, ':1')), ts.sz)
JOIN bench_word_pool w02 ON w02.topic = 0        AND w02.slot = 1 + MOD(CRC32(CONCAT(d.n, ':2')), cs.sz)
JOIN bench_word_pool w03 ON w03.topic = d.topic AND w03.slot = 1 + MOD(CRC32(CONCAT(d.n, ':3')), ts.sz)
JOIN bench_word_pool w04 ON w04.topic = 0        AND w04.slot = 1 + MOD(CRC32(CONCAT(d.n, ':4')), cs.sz)
JOIN bench_word_pool w05 ON w05.topic = d.topic AND w05.slot = 1 + MOD(CRC32(CONCAT(d.n, ':5')), ts.sz)
JOIN bench_word_pool w06 ON w06.topic = 0        AND w06.slot = 1 + MOD(CRC32(CONCAT(d.n, ':6')), cs.sz)
JOIN bench_word_pool w07 ON w07.topic = d.topic AND w07.slot = 1 + MOD(CRC32(CONCAT(d.n, ':7')), ts.sz)
JOIN bench_word_pool w08 ON w08.topic = 0        AND w08.slot = 1 + MOD(CRC32(CONCAT(d.n, ':8')), cs.sz)
JOIN bench_word_pool w09 ON w09.topic = d.topic AND w09.slot = 1 + MOD(CRC32(CONCAT(d.n, ':9')), ts.sz)
JOIN bench_word_pool w10 ON w10.topic = 0        AND w10.slot = 1 + MOD(CRC32(CONCAT(d.n, ':10')), cs.sz)
JOIN bench_word_pool w11 ON w11.topic = d.topic AND w11.slot = 1 + MOD(CRC32(CONCAT(d.n, ':11')), ts.sz)
JOIN bench_word_pool w12 ON w12.topic = 0        AND w12.slot = 1 + MOD(CRC32(CONCAT(d.n, ':12')), cs.sz)
JOIN bench_word_pool w13 ON w13.topic = d.topic AND w13.slot = 1 + MOD(CRC32(CONCAT(d.n, ':13')), ts.sz)
JOIN bench_word_pool w14 ON w14.topic = 0        AND w14.slot = 1 + MOD(CRC32(CONCAT(d.n, ':14')), cs.sz)
JOIN bench_word_pool w15 ON w15.topic = d.topic AND w15.slot = 1 + MOD(CRC32(CONCAT(d.n, ':15')), ts.sz)
JOIN bench_word_pool w16 ON w16.topic = 0        AND w16.slot = 1 + MOD(CRC32(CONCAT(d.n, ':16')), cs.sz)
JOIN bench_word_pool w17 ON w17.topic = d.topic AND w17.slot = 1 + MOD(CRC32(CONCAT(d.n, ':17')), ts.sz)
JOIN bench_word_pool w18 ON w18.topic = 0        AND w18.slot = 1 + MOD(CRC32(CONCAT(d.n, ':18')), cs.sz)
JOIN bench_word_pool w19 ON w19.topic = d.topic AND w19.slot = 1 + MOD(CRC32(CONCAT(d.n, ':19')), ts.sz)
JOIN bench_word_pool w20 ON w20.topic = 0        AND w20.slot = 1 + MOD(CRC32(CONCAT(d.n, ':20')), cs.sz)
JOIN bench_word_pool w21 ON w21.topic = d.topic AND w21.slot = 1 + MOD(CRC32(CONCAT(d.n, ':21')), ts.sz)
JOIN bench_word_pool w22 ON w22.topic = 0        AND w22.slot = 1 + MOD(CRC32(CONCAT(d.n, ':22')), cs.sz)
JOIN bench_word_pool w23 ON w23.topic = d.topic AND w23.slot = 1 + MOD(CRC32(CONCAT(d.n, ':23')), ts.sz)
JOIN bench_word_pool w24 ON w24.topic = 0        AND w24.slot = 1 + MOD(CRC32(CONCAT(d.n, ':24')), cs.sz)
JOIN bench_word_pool w25 ON w25.topic = d.topic AND w25.slot = 1 + MOD(CRC32(CONCAT(d.n, ':25')), ts.sz)
JOIN bench_word_pool w26 ON w26.topic = 0        AND w26.slot = 1 + MOD(CRC32(CONCAT(d.n, ':26')), cs.sz)
JOIN bench_word_pool w27 ON w27.topic = d.topic AND w27.slot = 1 + MOD(CRC32(CONCAT(d.n, ':27')), ts.sz)
JOIN bench_word_pool w28 ON w28.topic = 0        AND w28.slot = 1 + MOD(CRC32(CONCAT(d.n, ':28')), cs.sz)
JOIN bench_word_pool w29 ON w29.topic = d.topic AND w29.slot = 1 + MOD(CRC32(CONCAT(d.n, ':29')), ts.sz)
JOIN bench_word_pool w30 ON w30.topic = 0        AND w30.slot = 1 + MOD(CRC32(CONCAT(d.n, ':30')), cs.sz)
JOIN bench_word_pool w31 ON w31.topic = d.topic AND w31.slot = 1 + MOD(CRC32(CONCAT(d.n, ':31')), ts.sz)
JOIN bench_word_pool w32 ON w32.topic = 0        AND w32.slot = 1 + MOD(CRC32(CONCAT(d.n, ':32')), cs.sz)
JOIN bench_word_pool w33 ON w33.topic = d.topic AND w33.slot = 1 + MOD(CRC32(CONCAT(d.n, ':33')), ts.sz)
JOIN bench_word_pool w34 ON w34.topic = 0        AND w34.slot = 1 + MOD(CRC32(CONCAT(d.n, ':34')), cs.sz)
JOIN bench_word_pool w35 ON w35.topic = d.topic AND w35.slot = 1 + MOD(CRC32(CONCAT(d.n, ':35')), ts.sz)
JOIN bench_word_pool w36 ON w36.topic = 0        AND w36.slot = 1 + MOD(CRC32(CONCAT(d.n, ':36')), cs.sz);

SELECT COUNT(*) AS docs, COUNT(DISTINCT body) AS distinct_bodies,
       ROUND(AVG(CHAR_LENGTH(body))) AS avg_len FROM bench_docs;
