-- MySQL 8.4
-- 1 ~ 1,000,000 정수 생성. 재실행 시 테이블을 다시 만든다.

DROP TABLE IF EXISTS numbers;

CREATE TABLE numbers (
    n INT NOT NULL,
    PRIMARY KEY (n)
) ENGINE=InnoDB;

INSERT INTO numbers (n) VALUES (1);

INSERT INTO numbers (n) SELECT n + 1      FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 2      FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 4      FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 8      FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 16     FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 32     FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 64     FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 128    FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 256    FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 512    FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 1024   FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 2048   FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 4096   FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 8192   FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 16384  FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 32768  FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 65536  FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 131072 FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n) SELECT n + 262144 FROM (SELECT n FROM numbers) t;
INSERT INTO numbers (n)
SELECT n + 524288
FROM (SELECT n FROM numbers) t
WHERE n + 524288 <= 1000000;

SELECT COUNT(*) AS numbers_count, MIN(n) AS min_n, MAX(n) AS max_n
FROM numbers;
