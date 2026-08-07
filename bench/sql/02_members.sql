-- MySQL 8.4
-- 벤치마크 회원 10,000명 추가. email/nickname 기준으로 재실행 가능하다.
-- 실제 AUTO_INCREMENT 값을 bench_member_ids에 매핑하여 posts FK에 사용한다.

CREATE TABLE IF NOT EXISTS bench_member_ids (
    seq INT NOT NULL,
    member_id BIGINT NOT NULL,
    PRIMARY KEY (seq),
    UNIQUE KEY uk_bench_member_ids_member_id (member_id),
    CONSTRAINT fk_bench_member_ids_member
        FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE=InnoDB;

INSERT IGNORE INTO members (
    deleted,
    created_at,
    deleted_at,
    updated_at,
    email,
    encoded_password,
    nickname,
    profile_img_url,
    role
)
SELECT
    b'0',
    TIMESTAMP('2021-01-01 00:00:00') + INTERVAL MOD(n * 7919, 157766400) SECOND,
    NULL,
    TIMESTAMP('2021-01-01 00:00:00') + INTERVAL MOD(n * 7919, 157766400) SECOND,
    CONCAT('bench-user-', LPAD(n, 5, '0'), '@example.com'),
    '$2a$10$7EqJtq98hPqEX7fNZaFWoO5cHj9f8hG6nq0QFQdYjGvXK5mP2uQ6a',
    CONCAT('벤치회원', LPAD(n, 5, '0')),
    CONCAT('https://example.com/profiles/', LPAD(n, 5, '0'), '.png'),
    CASE WHEN n % 1000 = 0 THEN 'ROLE_ADMIN' ELSE 'ROLE_USER' END
FROM numbers
WHERE n <= 10000;

DELETE FROM bench_member_ids;

INSERT INTO bench_member_ids (seq, member_id)
SELECT
    CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(email, '@', 1), '-', -1) AS UNSIGNED) AS seq,
    id
FROM members
WHERE email LIKE 'bench-user-%@example.com'
  AND CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(email, '@', 1), '-', -1) AS UNSIGNED)
      BETWEEN 1 AND 10000;

SELECT COUNT(*) AS bench_members_count,
       MIN(member_id) AS min_member_id,
       MAX(member_id) AS max_member_id
FROM bench_member_ids;
