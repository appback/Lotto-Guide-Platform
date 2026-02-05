-- 시스템 옵션 테이블 생성
CREATE TABLE IF NOT EXISTS system_options (
    id BIGSERIAL PRIMARY KEY,
    option_key VARCHAR(100) NOT NULL UNIQUE,
    option_value TEXT,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_system_options_key ON system_options(option_key);

-- 기본 타이틀 옵션 삽입
INSERT INTO system_options (option_key, option_value, description, created_at, updated_at)
VALUES ('site_title', '로또 가이드', '웹사이트 브라우저 타이틀', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (option_key) DO NOTHING;
