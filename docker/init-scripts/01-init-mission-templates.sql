-- Database initialization script
-- Executed automatically when PostgreSQL container starts for the first time

-- 1. Mission Templates Table
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

CREATE INDEX IF NOT EXISTS idx_mission_templates_category ON mission_templates(category);
CREATE INDEX IF NOT EXISTS idx_mission_templates_theme ON mission_templates(theme);

-- 2. Lotto Draw Table
CREATE TABLE IF NOT EXISTS lotto_draw (
    draw_no INTEGER PRIMARY KEY,
    draw_date DATE NOT NULL,
    n1 INTEGER NOT NULL,
    n2 INTEGER NOT NULL,
    n3 INTEGER NOT NULL,
    n4 INTEGER NOT NULL,
    n5 INTEGER NOT NULL,
    n6 INTEGER NOT NULL,
    bonus INTEGER NOT NULL,
    total_prize DOUBLE PRECISION,
    winner_count INTEGER,
    prize_per_person DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_lotto_draw_draw_date ON lotto_draw(draw_date);

-- 3. Lotto Number Metrics Table
CREATE TABLE IF NOT EXISTS lotto_number_metrics (
    id BIGSERIAL PRIMARY KEY,
    window_size INTEGER NOT NULL,
    number INTEGER NOT NULL,
    freq INTEGER NOT NULL,
    overdue INTEGER NOT NULL,
    last_seen_draw_no INTEGER,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(window_size, number)
);

CREATE INDEX IF NOT EXISTS idx_lotto_number_metrics_window_size ON lotto_number_metrics(window_size);
CREATE INDEX IF NOT EXISTS idx_lotto_number_metrics_number ON lotto_number_metrics(number);

-- 4. Lotto Data State Table
CREATE TABLE IF NOT EXISTS lotto_data_state (
    id INTEGER PRIMARY KEY DEFAULT 1,
    as_of_draw_no INTEGER,
    refreshed_at TIMESTAMP,
    refreshing BOOLEAN NOT NULL DEFAULT FALSE,
    refresh_started_at TIMESTAMP,
    refresh_lock_until TIMESTAMP,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT lotto_data_state_single_row CHECK (id = 1)
);

-- 5. Mission Log Table
CREATE TABLE IF NOT EXISTS mission_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    anon_id VARCHAR(100),
    tone VARCHAR(50) NOT NULL,
    input_tags_json TEXT,
    mission_text TEXT NOT NULL,
    token_usage INTEGER,
    cost_estimate NUMERIC(10, 6),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mission_log_user_id ON mission_log(user_id);
CREATE INDEX IF NOT EXISTS idx_mission_log_created_at ON mission_log(created_at);

-- 6. Generated Set Table
CREATE TABLE IF NOT EXISTS generated_set (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    strategy_code VARCHAR(50) NOT NULL,
    strategy_params_json TEXT,
    constraints_json TEXT,
    generated_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_generated_set_user_id ON generated_set(user_id);
CREATE INDEX IF NOT EXISTS idx_generated_set_created_at ON generated_set(created_at);

-- 7. Generated Numbers Table
CREATE TABLE IF NOT EXISTS generated_numbers (
    id BIGSERIAL PRIMARY KEY,
    generated_set_id BIGINT NOT NULL,
    idx INTEGER NOT NULL,
    n1 INTEGER NOT NULL,
    n2 INTEGER NOT NULL,
    n3 INTEGER NOT NULL,
    n4 INTEGER NOT NULL,
    n5 INTEGER NOT NULL,
    n6 INTEGER NOT NULL,
    tags_json TEXT,
    FOREIGN KEY (generated_set_id) REFERENCES generated_set(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_generated_numbers_generated_set_id ON generated_numbers(generated_set_id);
