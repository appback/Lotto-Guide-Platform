-- 운명의 번호 추천 경고 메시지 테이블 생성
CREATE TABLE IF NOT EXISTS destiny_limit_message (
    id BIGSERIAL PRIMARY KEY,
    message TEXT NOT NULL,
    order_index INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_destiny_limit_message_order_index ON destiny_limit_message(order_index);

-- 초기 데이터 삽입 (기본 메시지 7개)
INSERT INTO destiny_limit_message (message, order_index) VALUES
('별들의 계시가 모두 전달되었습니다. 내일 다시 당신의 운명을 확인하세요.', 1),
('오늘의 운명은 이미 밝혀졌습니다. 내일 새로운 별의 인도를 받으세요.', 2),
('별자리의 메시지가 모두 전달되었습니다. 내일 다시 하늘의 계시를 받으세요.', 3),
('오늘의 운명의 번호는 이미 받으셨습니다. 내일 새로운 운명을 확인하세요.', 4),
('별들의 가르침이 모두 전해졌습니다. 내일 다시 당신의 별을 찾아보세요.', 5),
('오늘의 운명은 이미 결정되었습니다. 내일 새로운 운명의 길을 열어보세요.', 6),
('별자리의 계시가 모두 전달되었습니다. 내일 다시 당신의 운명을 만나보세요.', 7)
ON CONFLICT DO NOTHING;
