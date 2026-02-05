package io.appback.lottoguide.infra.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 동행복권 API 응답 DTO
 * 
 * API 엔드포인트: https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo={회차번호}
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DrawApiResponse {
    
    @JsonProperty("returnValue")
    private String returnValue;
    
    @JsonProperty("drwNo")
    private Integer drwNo; // 회차 번호
    
    @JsonProperty("drwNoDate")
    private String drwNoDate; // 추첨일 (yyyy-MM-dd)
    
    @JsonProperty("drwtNo1")
    private Integer drwtNo1;
    
    @JsonProperty("drwtNo2")
    private Integer drwtNo2;
    
    @JsonProperty("drwtNo3")
    private Integer drwtNo3;
    
    @JsonProperty("drwtNo4")
    private Integer drwtNo4;
    
    @JsonProperty("drwtNo5")
    private Integer drwtNo5;
    
    @JsonProperty("drwtNo6")
    private Integer drwtNo6;
    
    @JsonProperty("bnusNo")
    private Integer bnusNo; // 보너스 번호
    
    @JsonProperty("firstWinamnt")
    private Long firstWinamnt; // 1등 당첨금 (원 단위)
    
    @JsonProperty("firstPrzwnerCo")
    private Integer firstPrzwnerCo; // 1등 당첨인원
    
    @JsonProperty("totSellamnt")
    private Long totSellamnt; // 전체 판매 금액 (원 단위)
    
    /**
     * 성공 여부 확인
     */
    public boolean isSuccess() {
        return "success".equals(returnValue) && drwNo != null;
    }
    
    /**
     * 총 당첨금 계산 (억 단위)
     * firstWinamnt * firstPrzwnerCo / 100000000
     */
    public Double getTotalPrize() {
        if (firstWinamnt != null && firstPrzwnerCo != null && firstPrzwnerCo > 0) {
            return (firstWinamnt * firstPrzwnerCo) / 100000000.0;
        }
        return null;
    }
    
    /**
     * 인당 당첨금 (억 단위)
     * firstWinamnt / 100000000
     */
    public Double getPrizePerPerson() {
        if (firstWinamnt != null) {
            return firstWinamnt / 100000000.0;
        }
        return null;
    }
    
    /**
     * 당첨인원
     */
    public Integer getWinnerCount() {
        return firstPrzwnerCo;
    }
    
    /**
     * 추첨일을 LocalDate로 변환
     */
    public LocalDate getDrawDate() {
        if (drwNoDate == null || drwNoDate.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(drwNoDate, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 번호 배열로 반환 (정렬됨)
     */
    public int[] getNumbers() {
        return new int[]{
            drwtNo1, drwtNo2, drwtNo3, drwtNo4, drwtNo5, drwtNo6
        };
    }
}
