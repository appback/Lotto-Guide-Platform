-- 미션 템플릿 테이블 생성
CREATE TABLE IF NOT EXISTS mission_templates (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(100) NOT NULL,
    theme VARCHAR(50) NOT NULL,
    tone VARCHAR(50) NOT NULL,
    place_hint VARCHAR(50),
    time_hint VARCHAR(50),
    text TEXT NOT NULL,
    weight INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- 인덱스 생성 (조회 성능 향상)
CREATE INDEX IF NOT EXISTS idx_mission_templates_category ON mission_templates(category);
CREATE INDEX IF NOT EXISTS idx_mission_templates_theme ON mission_templates(theme);
