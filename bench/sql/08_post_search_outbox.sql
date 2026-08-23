-- 색인 동기화 아웃박스 테이블. ddl-auto=none 이라 JPA 가 만들어주지 않는다 —
-- 로컬 bench MySQL 과 운영 RDS 에 직접 적용할 것. (test 프로파일의 H2 만 create-drop 자동 생성)
CREATE TABLE post_search_outbox (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    post_id         BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    retry_count     INT          NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6)  NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    deleted         BIT(1)       NOT NULL DEFAULT 0,
    deleted_at      DATETIME(6)  NULL,
    INDEX idx_post_search_outbox_status_next_attempt_at (status, next_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
