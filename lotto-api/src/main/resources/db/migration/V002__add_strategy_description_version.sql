-- 전략 설명 테이블에 내용 기반 해시 일련번호 컬럼 추가
ALTER TABLE strategy_description 
ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64);

-- 기존 데이터의 해시는 애플리케이션에서 자동 생성됨
-- (내용이 변경되지 않았다면 해시가 동일하게 유지됨)
