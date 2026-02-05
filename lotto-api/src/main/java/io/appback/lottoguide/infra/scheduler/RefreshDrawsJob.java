package io.appback.lottoguide.infra.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 추첨 데이터 갱신 Job
 * 
 * 프로토타입에서는 기본 구조만 구현
 * 실제 추첨 데이터는 외부 API에서 가져와야 함
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshDrawsJob {
    
    /**
     * 매일 새벽 2시에 추첨 데이터 갱신
     * 프로토타입에서는 로그만 출력
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void refreshDraws() {
        log.info("RefreshDrawsJob 실행: 추첨 데이터 갱신 시작");
        
        // TODO: 실제 구현 시 외부 API에서 추첨 데이터를 가져와서 DB에 저장
        // 1. 최신 추첨 번호 조회
        // 2. 외부 API 호출하여 새로운 추첨 데이터 가져오기
        // 3. DB에 저장
        
        log.info("RefreshDrawsJob 완료: 추첨 데이터 갱신 완료");
    }
}
