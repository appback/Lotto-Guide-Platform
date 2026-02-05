-- ============================================
-- mission_templates 테이블 생성 스크립트
-- 프로덕션 환경에서 수동 실행 필요
-- ============================================

-- 테이블 생성
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

-- 테이블 생성 확인
SELECT COUNT(*) FROM mission_templates;
-- 예상 결과: 0 (초기 데이터는 애플리케이션 시작 시 자동 삽입)
