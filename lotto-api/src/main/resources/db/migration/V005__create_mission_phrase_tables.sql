-- A/B/C 멘트 조합 엔진 테이블 생성

-- A 멘트 테이블 (해석/은유)
CREATE TABLE IF NOT EXISTS mission_phrase_a (
    id BIGSERIAL PRIMARY KEY,
    text TEXT NOT NULL,
    strategy_tags TEXT,  -- JSON 배열: ["FREQUENT_TOP", "OVERDUE_TOP", ...]
    combo_tags TEXT,     -- JSON 배열: ["ODD_HEAVY", "SUM_HIGH", ...]
    zodiac_tags TEXT,    -- JSON 배열: ["ARIES", "GEMINI", ...]
    tone_tags TEXT,      -- JSON 배열: ["TAROT", "FORTUNE", ...]
    weight_base INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- B 멘트 테이블 (행동/장소/색감)
CREATE TABLE IF NOT EXISTS mission_phrase_b (
    id BIGSERIAL PRIMARY KEY,
    text TEXT NOT NULL,
    place_hint VARCHAR(50),  -- RIVER, PARK, CAFE, ...
    color_hint VARCHAR(50),   -- BLUE, RED, GREEN, ...
    align_tags TEXT,         -- JSON 배열: ["WATER", "FIRE", "LONELY", ...]
    avoid_tags TEXT,         -- JSON 배열: ["FIRE", "BOLD", ...]
    weight_base INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- C 멘트 테이블 (추천도/마무리)
CREATE TABLE IF NOT EXISTS mission_phrase_c (
    id BIGSERIAL PRIMARY KEY,
    text TEXT NOT NULL,
    tone_tags TEXT,      -- JSON 배열: ["TAROT", "FORTUNE", ...] (선택적)
    weight_base INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 히스토리는 프론트엔드 로컬 스토리지에서 관리하므로
-- DB 테이블 생성 불필요
