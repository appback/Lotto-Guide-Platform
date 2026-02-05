package io.appback.lottoguide.domain.mission.zodiac;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;

/**
 * 별자리 계산기
 * 생년월일을 기반으로 별자리를 계산
 * 
 * 원칙: 생년월일 정보는 이 클래스 내부에서만 사용되며 저장되지 않음
 */
@Component
public class ZodiacCalculator {
    
    /**
     * 생년월일로부터 별자리 계산
     * @param birthDate 생년월일 (YYYY-MM-DD)
     * @return 별자리 이름 (한글)
     */
    public String calculateZodiac(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        
        Month month = birthDate.getMonth();
        int day = birthDate.getDayOfMonth();
        
        // 별자리 경계일 기준으로 계산
        if ((month == Month.DECEMBER && day >= 22) || (month == Month.JANUARY && day <= 19)) {
            return "염소자리";
        } else if ((month == Month.JANUARY && day >= 20) || (month == Month.FEBRUARY && day <= 18)) {
            return "물병자리";
        } else if ((month == Month.FEBRUARY && day >= 19) || (month == Month.MARCH && day <= 20)) {
            return "물고기자리";
        } else if ((month == Month.MARCH && day >= 21) || (month == Month.APRIL && day <= 19)) {
            return "양자리";
        } else if ((month == Month.APRIL && day >= 20) || (month == Month.MAY && day <= 20)) {
            return "황소자리";
        } else if ((month == Month.MAY && day >= 21) || (month == Month.JUNE && day <= 21)) {
            return "쌍둥이자리";
        } else if ((month == Month.JUNE && day >= 22) || (month == Month.JULY && day <= 22)) {
            return "게자리";
        } else if ((month == Month.JULY && day >= 23) || (month == Month.AUGUST && day <= 22)) {
            return "사자자리";
        } else if ((month == Month.AUGUST && day >= 23) || (month == Month.SEPTEMBER && day <= 22)) {
            return "처녀자리";
        } else if ((month == Month.SEPTEMBER && day >= 23) || (month == Month.OCTOBER && day <= 22)) {
            return "천칭자리";
        } else if ((month == Month.OCTOBER && day >= 23) || (month == Month.NOVEMBER && day <= 21)) {
            return "전갈자리";
        } else { // (month == Month.NOVEMBER && day >= 22) || (month == Month.DECEMBER && day <= 21)
            return "사수자리";
        }
    }
    
    /**
     * 별자리 이름을 영문으로 변환 (필요시)
     * @param zodiacKorean 한글 별자리 이름
     * @return 영문 별자리 이름
     */
    public String toEnglish(String zodiacKorean) {
        if (zodiacKorean == null) {
            return null;
        }
        
        return switch (zodiacKorean) {
            case "염소자리" -> "Capricorn";
            case "물병자리" -> "Aquarius";
            case "물고기자리" -> "Pisces";
            case "양자리" -> "Aries";
            case "황소자리" -> "Taurus";
            case "쌍둥이자리" -> "Gemini";
            case "게자리" -> "Cancer";
            case "사자자리" -> "Leo";
            case "처녀자리" -> "Virgo";
            case "천칭자리" -> "Libra";
            case "전갈자리" -> "Scorpio";
            case "사수자리" -> "Sagittarius";
            default -> null;
        };
    }
}
