package io.appback.lottoguide.infra.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appback.lottoguide.infra.external.dto.DrawApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 동행복권 API Client
 * 
 * API 엔드포인트: https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo={회차번호}
 * 
 * 주의사항:
 * 1. 이 API는 비공식 엔드포인트일 수 있으며, 동행복권의 정책 변경 시 동작하지 않을 수 있습니다.
 * 2. API가 HTML을 반환하는 경우, 엔드포인트가 변경되었거나 접근이 차단된 것일 수 있습니다.
 * 3. 모든 API 호출은 실패 가능성을 고려하여 Optional로 반환하며, 예외는 내부에서 처리합니다.
 * 4. 외부 API 실패 시 랜덤 생성 모드로 폴백됩니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DonghaengLottoApiClient {
    
    // 동행복권 API 엔드포인트
    // HAR 분석 결과: 브라우저는 selectPstLt645Info.do를 사용하여 정상적으로 JSON을 받음
    // 기존 common.do 엔드포인트는 HTML을 반환할 수 있으므로, 브라우저가 사용하는 엔드포인트로 변경
    private static final String BASE_URL = "https://www.dhlottery.co.kr/common.do"; // 기존 엔드포인트 (개별 회차 조회용)
    private static final String LIST_API_URL = "https://www.dhlottery.co.kr/lt645/selectPstLt645Info.do"; // 전체 리스트 조회용 (HAR에서 확인된 엔드포인트)
    private static final String MAIN_PAGE_URL = "https://www.dhlottery.co.kr/";
    private static final String GAME_RESULT_PAGE_URL = "https://www.dhlottery.co.kr/lt645/result"; // HAR에서 확인된 실제 Referer
    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1초
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // 쿠키 저장 (세션 관리용)
    private String sessionCookies = null;
    
    // 기본 엔드포인트 차단 여부 (한 번 차단되면 바로 폴백으로 이동)
    private volatile boolean isBaseEndpointBlocked = false;
    
    /**
     * 세션 쿠키 획득 (WAF/봇 방어 우회를 위해 실제 당첨 번호 조회 페이지 먼저 접속)
     * 
     * 중요: 단순히 메인 페이지만 접속하면 API 권한이 활성화되지 않을 수 있습니다.
     * 실제 당첨 번호 조회 페이지로 접속하여 세션을 활성화해야 합니다.
     */
    private void ensureSessionCookies() {
        if (sessionCookies != null) {
            return; // 이미 쿠키가 있으면 재사용
        }
        
        try {
            log.debug("동행복권 당첨 번호 조회 페이지 접속하여 세션 쿠키 획득 시도");
            
            // 1단계: 메인 페이지 접속 (첫 방문자처럼)
            HttpHeaders mainHeaders = createBrowserHeaders();
            ResponseEntity<String> mainResponse = restTemplate.exchange(
                URI.create(MAIN_PAGE_URL),
                HttpMethod.GET,
                new HttpEntity<>(mainHeaders),
                String.class
            );
            
            // 메인 페이지에서 쿠키 획득
            List<String> mainCookies = extractCookies(mainResponse.getHeaders().get("Set-Cookie"));
            if (!mainCookies.isEmpty()) {
                sessionCookies = String.join("; ", mainCookies);
                log.debug("메인 페이지에서 쿠키 획득: {}", sessionCookies.length() > 50 
                    ? sessionCookies.substring(0, 50) + "..." 
                    : sessionCookies);
            }
            
            Thread.sleep(300); // 자연스러운 브라우저 동작 모방
            
            // 2단계: 실제 당첨 번호 조회 페이지 접속 시도 (HAR 분석 결과: lt645/result 사용)
            // 참고: 이 페이지가 302 리다이렉트를 반환할 수 있으므로, 리다이렉트 후 응답도 확인
            try {
                HttpHeaders resultHeaders = createBrowserHeaders();
                resultHeaders.set("Referer", MAIN_PAGE_URL); // 메인 페이지에서 온 것처럼
                
                ResponseEntity<String> resultResponse = restTemplate.exchange(
                    URI.create(GAME_RESULT_PAGE_URL),
                    HttpMethod.GET,
                    new HttpEntity<>(resultHeaders),
                    String.class
                );
                
                // HTTP 상태 코드 확인 (302 리다이렉트인 경우도 처리)
                if (resultResponse.getStatusCode().is3xxRedirection()) {
                    String location = resultResponse.getHeaders().getFirst("Location");
                    log.debug("당첨 번호 조회 페이지가 리다이렉트됨: Location={}", location);
                }
                
                // 당첨 번호 조회 페이지에서 추가 쿠키 획득 (리다이렉트 후에도 쿠키는 있을 수 있음)
                List<String> resultCookies = extractCookies(resultResponse.getHeaders().get("Set-Cookie"));
                if (!resultCookies.isEmpty()) {
                    // 기존 쿠키와 병합
                    List<String> allCookies = new ArrayList<>(mainCookies);
                    for (String cookie : resultCookies) {
                        // 중복 제거 (같은 이름의 쿠키는 최신 것으로 교체)
                        String cookieName = cookie.split("=")[0];
                        allCookies.removeIf(c -> c.startsWith(cookieName + "="));
                        allCookies.add(cookie);
                    }
                    sessionCookies = String.join("; ", allCookies);
                    log.info("세션 쿠키 획득 성공 (당첨 번호 조회 페이지): {}", sessionCookies.length() > 100 
                        ? sessionCookies.substring(0, 100) + "..." 
                        : sessionCookies);
                } else if (sessionCookies != null) {
                    log.info("세션 쿠키 유지 (당첨 번호 조회 페이지에서 추가 쿠키 없음): {}", sessionCookies.length() > 100 
                        ? sessionCookies.substring(0, 100) + "..." 
                        : sessionCookies);
                }
            } catch (Exception e) {
                log.debug("당첨 번호 조회 페이지 접속 실패 (리다이렉트 또는 기타 오류), 메인 페이지 쿠키만 사용: {}", e.getMessage());
                // 메인 페이지 쿠키만으로도 시도 가능
            }
            
            // 쿠키 획득 후 잠시 대기 (자연스러운 브라우저 동작 모방)
            Thread.sleep(500);
            
        } catch (Exception e) {
            log.warn("세션 쿠키 획득 실패, 쿠키 없이 시도: {}", e.getMessage());
        }
    }
    
    /**
     * Set-Cookie 헤더에서 쿠키 이름=값만 추출
     */
    private List<String> extractCookies(List<String> setCookieHeaders) {
        List<String> cookieValues = new ArrayList<>();
        if (setCookieHeaders != null && !setCookieHeaders.isEmpty()) {
            for (String setCookie : setCookieHeaders) {
                // Set-Cookie 형식: "JSESSIONID=xxx; Path=/; HttpOnly" -> "JSESSIONID=xxx"만 추출
                String cookieValue = setCookie.split(";")[0].trim();
                if (!cookieValue.isEmpty()) {
                    cookieValues.add(cookieValue);
                }
            }
        }
        return cookieValues;
    }
    
    /**
     * 브라우저처럼 보이는 HTTP 헤더 생성
     */
    private HttpHeaders createBrowserHeaders() {
        HttpHeaders headers = new HttpHeaders();
        
        // 최신 Chrome User-Agent
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
        
        // Accept 헤더를 브라우저처럼 설정
        headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        
        // Accept-Language
        headers.set("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        
        // Accept-Encoding
        headers.set("Accept-Encoding", "gzip, deflate, br, zstd");
        
        // Connection
        headers.set("Connection", "keep-alive");
        
        // Cache-Control
        headers.set("Cache-Control", "max-age=0");
        
        // Sec-Fetch 헤더 (최신 브라우저)
        headers.set("Sec-Fetch-Dest", "document");
        headers.set("Sec-Fetch-Mode", "navigate");
        headers.set("Sec-Fetch-Site", "none");
        headers.set("Sec-Fetch-User", "?1");
        
        // Upgrade-Insecure-Requests
        headers.set("Upgrade-Insecure-Requests", "1");
        
        // 쿠키가 있으면 추가
        if (sessionCookies != null) {
            headers.set("Cookie", sessionCookies);
        }
        
        return headers;
    }
    
    /**
     * API 호출용 헤더 생성 (JSON 요청)
     * 
     * 중요: User-Agent와 Referer가 없으면 서버가 즉시 HTML로 리다이렉트합니다.
     * Referer는 정밀 일치가 필요합니다.
     */
    private HttpHeaders createApiHeaders() {
        HttpHeaders headers = new HttpHeaders();
        
        // 1. User-Agent (필수) - 실제 최신 브라우저 정보
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
        
        // 2. Referer (필수) - 정밀 일치 필요: 실제 브라우저가 사용하는 정확한 주소
        // HAR 분석 결과: 브라우저는 https://www.dhlottery.co.kr/lt645/result를 Referer로 사용
        // 이 값이 없거나 틀리면 서버가 HTML을 반환할 수 있습니다
        headers.set("Referer", "https://www.dhlottery.co.kr/lt645/result");
        
        // 3. Host (추가) - 서버가 요청을 정확히 라우팅하도록
        headers.set("Host", "www.dhlottery.co.kr");
        
        // 4. Accept (권장 형식) - 동행복권 API 특성상 이 형식이 필요
        headers.set("Accept", "application/json, text/javascript, */*; q=0.01");
        
        // 5. X-Requested-With (AJAX 요청임을 명시)
        headers.set("X-Requested-With", "XMLHttpRequest");
        
        // 6. Accept-Language (HAR 분석 결과: 브라우저는 ko를 우선순위로 사용)
        headers.set("Accept-Language", "ko,en-US;q=0.9,en;q=0.8,ko-KR;q=0.7");
        
        // 7. Accept-Encoding (HAR 분석 결과: 브라우저는 gzip, deflate, br 사용)
        // 주의: Java에서 br(Brotli) 지원은 추가 라이브러리 필요, zstd는 제거
        headers.set("Accept-Encoding", "gzip, deflate, br");
        
        // 8. Connection
        headers.set("Connection", "keep-alive");
        
        // 9. Origin
        headers.set("Origin", "https://www.dhlottery.co.kr");
        
        // 10. Sec-Fetch 헤더 (최신 브라우저)
        headers.set("Sec-Fetch-Dest", "empty");
        headers.set("Sec-Fetch-Mode", "cors");
        headers.set("Sec-Fetch-Site", "same-origin");
        
        // 11. 쿠키 (세션 쿠키) - DHJSESSIONID 등
        // 중요: 쿠키 값이 "DHJSESSIONID=..." 형태여야 함
        if (sessionCookies != null && !sessionCookies.isEmpty()) {
            headers.set("Cookie", sessionCookies);
            log.debug("API 호출에 쿠키 포함: {}", sessionCookies.length() > 50 
                ? sessionCookies.substring(0, 50) + "..." 
                : sessionCookies);
        } else {
            log.warn("쿠키가 없습니다. API 호출이 실패할 수 있습니다.");
        }
        
        return headers;
    }
    
    /**
     * 특정 회차의 추첨 결과 조회
     * 
     * HAR 분석 결과 반영:
     * - 브라우저는 Referer를 https://www.dhlottery.co.kr/lt645/result로 사용
     * - Accept-Language는 ko를 우선순위로 사용
     * - 기존 common.do 엔드포인트가 HTML을 반환하면, 전체 리스트 조회로 폴백 시도
     * 
     * 실패 시나리오:
     * 1. 네트워크 오류 -> 재시도 후 실패 시 Optional.empty()
     * 2. HTML 응답 반환 -> 전체 리스트 조회로 폴백 시도 -> 실패 시 Optional.empty()
     * 3. JSON 파싱 실패 -> 응답 형식 변경 -> Optional.empty()
     * 4. returnValue != "success" -> API 오류 -> Optional.empty()
     * 
     * @param drawNo 회차 번호
     * @return 추첨 결과 (Optional) - 실패 시 empty 반환
     */
    public Optional<DrawApiResponse> fetchDraw(int drawNo) {
        if (drawNo < 1) {
            log.warn("잘못된 회차 번호: {}", drawNo);
            return Optional.empty();
        }
        
        // 세션 쿠키 획득 (처음 호출 시 또는 쿠키가 없을 때만)
        ensureSessionCookies();
        
        // 기본 엔드포인트가 차단되어 있으면 바로 폴백으로 이동
        if (isBaseEndpointBlocked) {
            log.debug("기본 엔드포인트 차단됨, 바로 폴백 엔드포인트로 이동: drawNo={}", drawNo);
            return fetchDrawFromListApi(drawNo);
        }
        
        String url = String.format("%s?method=getLottoNumber&drwNo=%d", BASE_URL, drawNo);
        
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                // 매 시도마다 헤더를 새로 생성 (쿠키는 재사용)
                HttpHeaders headers = createApiHeaders();
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                log.debug("동행복권 API 호출 시도 {}/{}: drawNo={}, url={}", attempt, MAX_RETRY, drawNo, url);
                
                // 디버깅: 실제 전송되는 헤더 로그 출력
                log.info("동행복권 API 호출 헤더 (시도 {}): User-Agent={}, Referer={}, Accept={}, Origin={}, Cookie={}", 
                    attempt,
                    headers.getFirst("User-Agent") != null ? headers.getFirst("User-Agent").substring(0, Math.min(50, headers.getFirst("User-Agent").length())) + "..." : "없음",
                    headers.getFirst("Referer"),
                    headers.getFirst("Accept"),
                    headers.getFirst("Origin"),
                    headers.getFirst("Cookie") != null ? (headers.getFirst("Cookie").length() > 50 
                        ? headers.getFirst("Cookie").substring(0, 50) + "..." 
                        : headers.getFirst("Cookie")) : "없음"
                );
                
                ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(url), 
                    HttpMethod.GET, 
                    entity, 
                    String.class
                );
                
                // HTTP 상태 코드 확인
                if (response.getStatusCode() != HttpStatus.OK) {
                    log.warn("동행복권 API HTTP 상태 코드 오류: status={}, drawNo={}, attempt={}/{}", 
                        response.getStatusCode(), drawNo, attempt, MAX_RETRY);
                    if (attempt < MAX_RETRY) {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                        continue;
                    }
                    return Optional.empty();
                }
                
                // 응답 헤더 확인 (리다이렉트 여부 확인)
                String contentType = response.getHeaders().getFirst("Content-Type");
                String location = response.getHeaders().getFirst("Location");
                List<String> setCookies = response.getHeaders().get("Set-Cookie");
                log.info("동행복권 API 응답 헤더: Status={}, Content-Type={}, Location={}, Set-Cookie={}", 
                    response.getStatusCode(),
                    contentType,
                    location != null ? location : "없음",
                    setCookies != null && !setCookies.isEmpty() ? setCookies.size() + "개" : "없음"
                );
                
                // Set-Cookie가 있으면 쿠키 업데이트
                if (setCookies != null && !setCookies.isEmpty()) {
                    List<String> newCookies = extractCookies(setCookies);
                    if (!newCookies.isEmpty()) {
                        // 기존 쿠키와 병합
                        List<String> allCookies = new ArrayList<>();
                        if (sessionCookies != null && !sessionCookies.isEmpty()) {
                            String[] existingCookies = sessionCookies.split("; ");
                            for (String cookie : existingCookies) {
                                allCookies.add(cookie.trim());
                            }
                        }
                        for (String cookie : newCookies) {
                            // 중복 제거
                            String cookieName = cookie.split("=")[0];
                            allCookies.removeIf(c -> c.startsWith(cookieName + "="));
                            allCookies.add(cookie);
                        }
                        sessionCookies = String.join("; ", allCookies);
                        log.info("응답에서 새 쿠키 획득 및 업데이트: {}", sessionCookies.length() > 100 
                            ? sessionCookies.substring(0, 100) + "..." 
                            : sessionCookies);
                    }
                }
                
                String responseBody = response.getBody();
                
                // 응답 본문 확인
                if (responseBody == null || responseBody.trim().isEmpty()) {
                    log.warn("동행복권 API 응답 본문이 비어있음: drawNo={}, attempt={}/{}", drawNo, attempt, MAX_RETRY);
                    if (attempt < MAX_RETRY) {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                        continue;
                    }
                    return Optional.empty();
                }
                
                // HTML 응답 체크 (JSON이 아닌 경우)
                // 동행복권 API가 HTML을 반환하는 경우: API 변경, 회차 없음, 접근 차단 등
                if (responseBody.trim().startsWith("<")) {
                    // 기본 엔드포인트가 차단되었음을 표시 (이후 호출 시 바로 폴백으로 이동)
                    isBaseEndpointBlocked = true;
                    
                    // HTML 응답을 받은 경우: HAR 분석 결과, selectPstLt645Info.do 엔드포인트로 폴백 시도
                    // 이 엔드포인트는 전체 리스트를 반환하므로, 특정 회차를 필터링하여 찾음
                    // 주의: selectPstLt645Info.do는 당첨금 정보를 포함하지 않을 수 있음
                    log.debug("기본 엔드포인트 HTML 응답, 폴백 엔드포인트로 전환: drawNo={}", drawNo);
                    Optional<DrawApiResponse> fallbackResult = fetchDrawFromListApi(drawNo);
                    if (fallbackResult.isPresent()) {
                        // 폴백 API는 당첨금 정보가 없을 수 있으므로 확인
                        DrawApiResponse result = fallbackResult.get();
                        if (result.getFirstWinamnt() == null && result.getFirstPrzwnerCo() == null) {
                            log.warn("폴백 엔드포인트에서 당첨금 정보 없음: drawNo={} (selectPstLt645Info.do는 당첨금 정보를 포함하지 않을 수 있음)", drawNo);
                        }
                        log.debug("폴백 엔드포인트에서 회차 조회 성공: drawNo={}", drawNo);
                        return fallbackResult;
                    }
                    log.warn("폴백 엔드포인트에서도 회차를 찾을 수 없음: drawNo={}", drawNo);
                    return Optional.empty();
                }
                
                // JSON 형식 확인
                String trimmedBody = responseBody.trim();
                if (!trimmedBody.startsWith("{") && !trimmedBody.startsWith("[")) {
                    log.warn("동행복권 API 응답이 JSON 형식이 아님: drawNo={}, responsePreview={}, attempt={}/{}", 
                        drawNo, 
                        trimmedBody.length() > 200 ? trimmedBody.substring(0, 200) : trimmedBody,
                        attempt, MAX_RETRY);
                    return Optional.empty();
                }
                
                // JSON 파싱 시도
                DrawApiResponse apiResponse;
                try {
                    apiResponse = objectMapper.readValue(responseBody, DrawApiResponse.class);
                } catch (com.fasterxml.jackson.core.JsonParseException e) {
                    log.warn("동행복권 API JSON 파싱 실패: drawNo={}, error={}, responsePreview={}, attempt={}/{}", 
                        drawNo, e.getMessage(),
                        responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody,
                        attempt, MAX_RETRY);
                    return Optional.empty();
                } catch (Exception e) {
                    log.warn("동행복권 API 응답 파싱 중 예외: drawNo={}, error={}, attempt={}/{}", 
                        drawNo, e.getMessage(), attempt, MAX_RETRY);
                    return Optional.empty();
                }
                
                // API 응답 검증
                if (apiResponse == null) {
                    log.warn("동행복권 API 응답이 null: drawNo={}, attempt={}/{}", drawNo, attempt, MAX_RETRY);
                    return Optional.empty();
                }
                
                // returnValue 확인
                if (!apiResponse.isSuccess()) {
                    log.warn("동행복권 API 응답 실패: returnValue={}, drawNo={}, attempt={}/{}", 
                        apiResponse.getReturnValue(), drawNo, attempt, MAX_RETRY);
                    return Optional.empty();
                }
                
                // 회차 번호 일치 확인
                if (apiResponse.getDrwNo() == null || !apiResponse.getDrwNo().equals(drawNo)) {
                    log.warn("동행복권 API 응답 회차 번호 불일치: 요청={}, 응답={}, attempt={}/{}", 
                        drawNo, apiResponse.getDrwNo(), attempt, MAX_RETRY);
                    return Optional.empty();
                }
                
                log.info("동행복권 API 호출 성공: drawNo={}, drawDate={}", 
                    apiResponse.getDrwNo(), apiResponse.getDrwNoDate());
                
                return Optional.of(apiResponse);
                
            } catch (RestClientException e) {
                // 네트워크 오류: 재시도 가능
                log.warn("동행복권 API 호출 중 네트워크 오류 (시도 {}/{}): drawNo={}, error={}", 
                    attempt, MAX_RETRY, drawNo, e.getMessage());
                
                if (attempt < MAX_RETRY) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("동행복권 API 재시도 중단: drawNo={}", drawNo);
                        return Optional.empty();
                    }
                    continue;
                }
                return Optional.empty();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("동행복권 API 호출 중 인터럽트: drawNo={}", drawNo);
                return Optional.empty();
                
            } catch (Exception e) {
                // 예상치 못한 예외: 로깅 후 실패 처리 (스택 트레이스 없이 메시지만)
                log.error("동행복권 API 호출 중 예상치 못한 예외 (시도 {}/{}): drawNo={}, error={}", 
                    attempt, MAX_RETRY, drawNo, e.getMessage());
                
                // 예상치 못한 예외는 재시도하지 않음
                return Optional.empty();
            }
        }
        
        // 모든 재시도 실패
        log.error("동행복권 API 호출 최종 실패: drawNo={}, 최대 재시도 횟수 초과", drawNo);
        return Optional.empty();
    }
    
    /**
     * HAR 분석 결과 확인된 엔드포인트(selectPstLt645Info.do)를 사용하여 전체 리스트에서 특정 회차 조회
     * 
     * 이 메서드는 common.do 엔드포인트가 HTML을 반환할 때 폴백으로 사용됩니다.
     * 
     * @param drawNo 회차 번호
     * @return 추첨 결과 (Optional) - 실패 시 empty 반환
     */
    private Optional<DrawApiResponse> fetchDrawFromListApi(int drawNo) {
        try {
            log.debug("전체 리스트 API로 회차 조회 시도: drawNo={}", drawNo);
            
            String url = String.format("%s?srchLtEpsd=all", LIST_API_URL);
            HttpHeaders headers = createApiHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                URI.create(url),
                HttpMethod.GET,
                entity,
                String.class
            );
            
            if (response.getStatusCode() != HttpStatus.OK) {
                log.warn("전체 리스트 API HTTP 상태 코드 오류: status={}, drawNo={}", 
                    response.getStatusCode(), drawNo);
                return Optional.empty();
            }
            
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.warn("전체 리스트 API 응답 본문이 비어있음: drawNo={}", drawNo);
                return Optional.empty();
            }
            
            // JSON 파싱
            try {
                // JSON 구조: {"resultCode":null,"resultMessage":null,"data":{"list":[...]}}
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> jsonMap = objectMapper.readValue(responseBody, java.util.Map.class);
                
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> data = (java.util.Map<String, Object>) jsonMap.get("data");
                if (data == null) {
                    log.warn("전체 리스트 API 응답에 data 필드가 없음: drawNo={}", drawNo);
                    return Optional.empty();
                }
                
                @SuppressWarnings("unchecked")
                java.util.List<java.util.Map<String, Object>> list = 
                    (java.util.List<java.util.Map<String, Object>>) data.get("list");
                if (list == null || list.isEmpty()) {
                    log.warn("전체 리스트 API 응답에 list가 비어있음: drawNo={}", drawNo);
                    return Optional.empty();
                }
                
                // 특정 회차 찾기
                // 필드명: ltEpsd (회차), tm1WnNo~tm6WnNo (당첨번호), bnsWnNo (보너스), ltRflYmd (추첨일)
                for (java.util.Map<String, Object> item : list) {
                    Object ltEpsdObj = item.get("ltEpsd");
                    if (ltEpsdObj != null) {
                        int itemDrawNo = ((Number) ltEpsdObj).intValue();
                        if (itemDrawNo == drawNo) {
                            // 날짜 형식 변환: "20021207" -> "2002-12-07"
                            String ltRflYmd = (String) item.get("ltRflYmd");
                            String formattedDate = formatDate(ltRflYmd);
                            
                            // API 응답 필드 확인 (디버깅용)
                            log.debug("API 응답 필드 확인 (drawNo={}): {}", drawNo, item.keySet());
                            
                            // DrawApiResponse로 변환
                            // selectPstLt645Info.do API 필드명: rnk1WnAmt (1등 당첨금액), rnk1WnNope (1등 당첨인원)
                            Long firstWinamnt = getLongValue(item, "rnk1WnAmt");
                            Integer firstPrzwnerCo = getIntValue(item, "rnk1WnNope");
                            Long totSellamnt = getLongValue(item, "rlvtEpsdSumNtslAmt"); // 해당 회차 판매금액
                            
                            log.debug("당첨금 필드 확인 (drawNo={}): rnk1WnAmt={}, rnk1WnNope={}, rlvtEpsdSumNtslAmt={}", 
                                drawNo, firstWinamnt, firstPrzwnerCo, totSellamnt);
                            
                            DrawApiResponse apiResponse = DrawApiResponse.builder()
                                .returnValue("success")
                                .drwNo(itemDrawNo)
                                .drwNoDate(formattedDate)
                                .drwtNo1(getIntValue(item, "tm1WnNo"))
                                .drwtNo2(getIntValue(item, "tm2WnNo"))
                                .drwtNo3(getIntValue(item, "tm3WnNo"))
                                .drwtNo4(getIntValue(item, "tm4WnNo"))
                                .drwtNo5(getIntValue(item, "tm5WnNo"))
                                .drwtNo6(getIntValue(item, "tm6WnNo"))
                                .bnusNo(getIntValue(item, "bnsWnNo"))
                                .firstWinamnt(firstWinamnt)
                                .firstPrzwnerCo(firstPrzwnerCo)
                                .totSellamnt(totSellamnt)
                                .build();
                            
                            log.info("전체 리스트 API에서 회차 조회 성공: drawNo={}, drawDate={}, firstWinamnt={}, firstPrzwnerCo={}, totalPrize={}, prizePerPerson={}", 
                                apiResponse.getDrwNo(), apiResponse.getDrwNoDate(), 
                                firstWinamnt, firstPrzwnerCo, 
                                apiResponse.getTotalPrize(), apiResponse.getPrizePerPerson());
                            return Optional.of(apiResponse);
                        }
                    }
                }
                
                log.warn("전체 리스트에서 회차를 찾을 수 없음: drawNo={}", drawNo);
                return Optional.empty();
                
            } catch (Exception e) {
                log.warn("전체 리스트 API JSON 파싱 실패: drawNo={}, error={}", drawNo, e.getMessage());
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.warn("전체 리스트 API 호출 실패: drawNo={}, error={}", drawNo, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Map에서 Integer 값 추출 (null 안전)
     */
    private Integer getIntValue(java.util.Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Map에서 Long 값 추출 (null 안전)
     */
    private Long getLongValue(java.util.Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * 날짜 형식 변환: "20021207" -> "2002-12-07"
     * 
     * @param dateStr "yyyyMMdd" 형식의 날짜 문자열
     * @return "yyyy-MM-dd" 형식의 날짜 문자열, 변환 실패 시 null
     */
    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.length() != 8) {
            return null;
        }
        try {
            // "20021207" -> "2002-12-07"
            return dateStr.substring(0, 4) + "-" + dateStr.substring(4, 6) + "-" + dateStr.substring(6, 8);
        } catch (Exception e) {
            log.warn("날짜 형식 변환 실패: dateStr={}, error={}", dateStr, e.getMessage());
            return null;
        }
    }
    
    /**
     * 최신 회차부터 역순으로 여러 회차 조회
     * 
     * 실패 처리:
     * - 개별 회차 조회 실패 시 해당 회차만 건너뛰고 계속 진행
     * - 연속 실패가 발생하면 중단 (데이터 불일치 방지)
     * 
     * @param startDrawNo 시작 회차 번호 (최신 회차)
     * @param count 조회할 회차 수
     * @return 추첨 결과 리스트 (실패한 회차는 제외)
     */
    public List<DrawApiResponse> fetchDraws(int startDrawNo, int count) {
        if (startDrawNo < 1 || count < 1) {
            log.warn("잘못된 파라미터: startDrawNo={}, count={}", startDrawNo, count);
            return List.of();
        }
        
        List<DrawApiResponse> results = new ArrayList<>();
        int consecutiveFailures = 0; // 연속 실패 횟수
        final int MAX_CONSECUTIVE_FAILURES = 3; // 최대 연속 실패 허용 횟수
        
        for (int i = 0; i < count; i++) {
            int drawNo = startDrawNo - i;
            if (drawNo < 1) {
                log.debug("회차 번호가 1보다 작음, 중단: drawNo={}", drawNo);
                break;
            }
            
            Optional<DrawApiResponse> response = fetchDraw(drawNo);
            if (response.isPresent()) {
                results.add(response.get());
                consecutiveFailures = 0; // 성공 시 연속 실패 카운터 리셋
            } else {
                consecutiveFailures++;
                log.warn("회차 {} 조회 실패 (연속 실패: {}/{})", drawNo, consecutiveFailures, MAX_CONSECUTIVE_FAILURES);
                
                // 연속 실패가 너무 많으면 중단 (데이터 불일치 방지)
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    log.warn("연속 실패 횟수 초과, 조회 중단: drawNo={}, 연속실패={}", drawNo, consecutiveFailures);
                    break;
                }
            }
            
            // API 부하 방지를 위한 대기 (차단 방지)
            try {
                Thread.sleep(1500); // 1.5초 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("fetchDraws 중단: drawNo={}", drawNo);
                break;
            }
        }
        
        log.info("회차 조회 완료: 요청={}개, 성공={}개, 실패={}개", 
            count, results.size(), count - results.size());
        
        return results;
    }
    
    /**
     * 최신 회차 번호 조회 (날짜 기반 계산)
     * 
     * 로또 추첨 정보:
     * - 첫 회차: 2002년 12월 7일 (토요일)
     * - 매주 토요일 추첨 (추첨 후 일요일부터 다음 회차 발표)
     * - 오늘 날짜까지 진행된 회차 수를 계산
     * 
     * 계산 방식:
     * 1. 첫 회차 날짜와 오늘 날짜 사이의 경과 주 수 계산
     * 2. 회차 = 1 + 경과 주 수
     * 3. 오늘이 일요일~토요일이면 지난 토요일 회차까지 포함
     *    (일요일부터 다음 토요일까지는 같은 회차로 간주)
     * 
     * 예시:
     * - 2026-01-19 (일요일) 호출 -> 지난 토요일(2026-01-18) 회차까지
     * - 2026-01-20 (월요일) 호출 -> 지난 토요일(2026-01-18) 회차까지
     * - 2026-01-25 (토요일) 호출 -> 오늘(2026-01-25) 회차까지
     * 
     * @param estimatedLatestDrawNo 예상 최신 회차 번호 (무시됨, 날짜 기반으로 계산)
     * @return 최신 회차 번호 (Optional) - 항상 성공
     */
    public Optional<Integer> findLatestDrawNo(int estimatedLatestDrawNo) {
        log.info("최신 회차 번호 조회 시작 (날짜 기반 계산)");
        
        // 첫 회차 날짜: 2002년 12월 7일 (토요일)
        LocalDate firstDrawDate = LocalDate.of(2002, 12, 7);
        
        // 오늘 날짜
        LocalDate today = LocalDate.now();
        
        // 마지막 추첨일 계산
        // DayOfWeek: 1=월요일, 2=화요일, ..., 6=토요일, 7=일요일
        LocalDate lastDrawDate = today;
        int dayOfWeek = today.getDayOfWeek().getValue();
        
        if (dayOfWeek == 7) {
            // 일요일: 지난 토요일까지 (어제)
            lastDrawDate = today.minusDays(1);
        } else if (dayOfWeek < 6) {
            // 월~금: 지난 토요일까지
            // 월요일(1) -> 2일 전, 화요일(2) -> 3일 전, ..., 금요일(5) -> 1일 전
            lastDrawDate = today.minusDays(dayOfWeek + 1);
        }
        // 토요일(6)이면 오늘 포함
        
        // 첫 회차 날짜와 마지막 추첨일 사이의 경과 주 수 계산
        long weeksBetween = ChronoUnit.WEEKS.between(firstDrawDate, lastDrawDate);
        
        // 회차 = 1 + 경과 주 수
        int latestDrawNo = 1 + (int) weeksBetween;
        
        log.info("날짜 기반 최신 회차 계산: 첫 회차={}, 오늘={}, 마지막 추첨일={}, 경과 주 수={}, 최신 회차={}", 
            firstDrawDate, today, lastDrawDate, weeksBetween, latestDrawNo);
        
        return Optional.of(latestDrawNo);
    }
}
