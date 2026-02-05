package io.appback.lottoguide.infra.persistence.init;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appback.lottoguide.infra.persistence.entity.StrategyDescriptionEntity;
import io.appback.lottoguide.infra.persistence.repository.StrategyDescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 전략 설명 초기 데이터 삽입
 * 
 * 애플리케이션 시작 시 전략 설명이 없으면 기본 데이터를 삽입합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StrategyDescriptionInitializer implements CommandLineRunner {
    
    private final StrategyDescriptionRepository strategyDescriptionRepository;
    private final ObjectMapper objectMapper;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("전략 설명 초기 데이터 확인 시작");
        
        // 모든 전략 코드 목록
        List<String> allStrategyCodes = List.of(
            "FREQUENT_TOP",
            "OVERDUE_TOP",
            "BALANCED",
            "WHEELING_SYSTEM",
            "WEIGHTED_RANDOM",
            "PATTERN_MATCHER",
            "AI_SIMULATION",
            "AI_PATTERN_REASONER",
            "AI_DECISION_FILTER",
            "AI_WEIGHT_EVOLUTION"
        );
        
        // 누락된 전략 확인
        List<String> missingStrategies = new ArrayList<>();
        for (String strategyCode : allStrategyCodes) {
            if (!strategyDescriptionRepository.findByStrategyCode(strategyCode).isPresent()) {
                missingStrategies.add(strategyCode);
            }
        }
        
        if (missingStrategies.isEmpty()) {
            log.info("모든 전략 설명이 존재합니다. 초기화를 건너뜁니다.");
            return;
        }
        
        log.info("전략 설명 초기 데이터 삽입 시작 - 누락된 전략: {}개", missingStrategies.size());
        
        LocalDateTime now = LocalDateTime.now();
        
        // FREQUENT_TOP
        createStrategyDescription(
            "FREQUENT_TOP",
            "고빈도 우선",
            "고빈도 우선: 과거 당첨 번호 중 자주 나온 번호를 우선적으로 선택합니다. 통계적으로 자주 등장한 번호에 집중하는 전략입니다.",
            "과거 당첨 번호 중 자주 나온 번호를 우선적으로 선택합니다. 통계적으로 자주 등장한 번호에 집중하는 전략입니다.",
            List.of(
                "최근 N회 추첨(기본 50회)에서 고빈도 번호를 우선 선택",
                "빈도가 높을수록 높은 가중치를 부여하여 선택 확률 증가",
                "통계적 패턴을 활용한 번호 선택"
            ),
            List.of(
                "1~45번 전체 번호를 후보 풀로 생성",
                "제약 조건(포함/제외 번호) 적용",
                "메트릭 데이터 기반으로 각 번호의 빈도(frequency) 계산",
                "빈도가 높은 순서로 정렬하여 상위 6개 선택",
                "메트릭 데이터가 없는 경우 랜덤 선택"
            ),
            List.of(
                "통계적으로 검증된 번호를 선호하는 경우",
                "과거 당첨 패턴을 신뢰하는 경우"
            ),
            null
        );
        
        // OVERDUE_TOP
        createStrategyDescription(
            "OVERDUE_TOP",
            "과거 데이터 우선",
            "과거 데이터 우선: 최근에 나오지 않은 번호를 우선적으로 선택합니다. 오랫동안 나오지 않은 번호에 집중하는 전략입니다.",
            "최근에 나오지 않은 번호를 우선적으로 선택합니다. 오랫동안 나오지 않은 번호에 집중하는 전략입니다.",
            List.of(
                "최근에 나오지 않은 번호(overdue 값이 큰 번호) 우선 선택",
                "오래 안 나온 번호일수록 높은 가중치 부여",
                "\"나올 차례\"라는 가설에 기반한 선택"
            ),
            List.of(
                "1~45번 전체 번호를 후보 풀로 생성",
                "제약 조건(포함/제외 번호) 적용",
                "메트릭 데이터 기반으로 각 번호의 과거 데이터(overdue) 계산",
                "overdue 값이 큰 순서로 정렬하여 상위 6개 선택",
                "메트릭 데이터가 없는 경우 랜덤 선택"
            ),
            List.of(
                "오래 나오지 않은 번호가 곧 나올 것이라고 믿는 경우",
                "역발상 전략을 선호하는 경우"
            ),
            null
        );
        
        // BALANCED
        createStrategyDescription(
            "BALANCED",
            "균형",
            "균형: 고빈도 번호와 과거 데이터를 균형있게 조합하여 선택합니다. 다양한 번호를 포함하는 전략입니다.",
            "고빈도 번호와 과거 데이터를 균형있게 조합하여 선택합니다. 다양한 번호를 포함하는 전략입니다.",
            List.of(
                "고빈도와 과거 데이터를 모두 고려한 균형잡힌 선택",
                "제약 조건(홀수/짝수 비율, 합계 범위 등) 적용 가능",
                "가장 일반적이고 안정적인 전략"
            ),
            List.of(
                "1~45번 전체 번호를 후보 풀로 생성",
                "제약 조건(포함/제외 번호, 홀수/짝수 비율, 합계 범위) 적용",
                "제약 조건을 만족하는 조합을 랜덤으로 생성",
                "최대 1000회 시도하여 제약 조건을 만족하는 조합 선택",
                "제약 조건을 만족하지 못하면 기본 랜덤 선택"
            ),
            List.of(
                "가장 일반적인 사용 케이스",
                "특정 제약 조건을 적용하고 싶은 경우",
                "다양한 번호 조합을 원하는 경우"
            ),
            null
        );
        
        // WHEELING_SYSTEM
        createStrategyDescription(
            "WHEELING_SYSTEM",
            "추천조합1 (기본 14게임)",
            "추천조합1 (기본 14게임): 5등(번호 3개 일치) 보장 조합입니다. 통계적으로 가장 안 나온 9개 번호를 제외한 36개 번호로 조합을 생성합니다. 기본값은 14게임이며, 5등 보장을 위해 14게임을 권장합니다. 게임 개수는 변경 가능합니다.",
            "5등(번호 3개 일치) 보장 조합입니다. 통계적으로 가장 안 나온 9개 번호를 제외한 36개 번호로 조합을 생성합니다.",
            List.of(
                "기본 게임 개수: 14게임 (변경 가능, 5등 보장을 위해 권장)",
                "5등(3개 일치) 보장 알고리즘 (14게임 기준)",
                "통계적으로 가장 안 나온 9개 번호 자동 제외",
                "36개 번호를 요청한 개수만큼 조합에 균등하게 분산"
            ),
            List.of(
                "Step 1: 번호 제외 - 최근 N회 추첨(기본 50회)에서 통계적으로 가장 안 나온 9개 번호 제외",
                "Step 2: 36개 번호 선택 - 전체 45개 번호에서 제외된 9개를 뺀 36개 번호 사용",
                "Step 3: 조합 생성 - Round-Robin 방식으로 36개 번호를 요청한 개수만큼 조합에 균등하게 분산",
                "각 번호가 여러 조합에 포함되도록 구성하여 3개 일치를 보장",
                "5등 보장을 위해서는 14개 조합 권장"
            ),
            List.of(
                "5등 이상 당첨을 보장하고 싶은 경우",
                "여러 게임을 구매하여 당첨 확률을 높이고 싶은 경우",
                "통계적으로 검증된 번호 조합을 원하는 경우"
            ),
            List.of(
                "기본 게임 개수는 14개이지만 변경 가능합니다",
                "5등 보장을 위해서는 14게임을 권장합니다 (14개 미만일 경우 보장이 깨질 수 있음)",
                "5등 보장은 \"당첨 번호 6개가 36개 번호에 포함되어 있다\"는 가정 하에 성립",
                "실제 당첨 번호가 제외된 9개 번호에 포함되면 보장이 깨질 수 있음"
            )
        );
        
        // WEIGHTED_RANDOM
        createStrategyDescription(
            "WEIGHTED_RANDOM",
            "가중치 기반 생성",
            "가중치 기반 생성: 빈도와 과거 데이터를 결합한 가중치를 사용하여 번호를 랜덤하게 추출합니다. 완전한 무작위가 아니라 통계 기반 확률로 번호를 선택합니다.",
            "빈도와 과거 데이터를 결합한 가중치를 사용하여 번호를 랜덤하게 추출합니다. 완전한 무작위가 아니라 통계 기반 확률로 번호를 선택합니다.",
            List.of(
                "빈도와 과거 데이터를 50:50으로 결합한 가중치 사용",
                "가중치가 높은 번호일수록 선택 확률이 높음",
                "완전한 무작위가 아닌 통계 기반 확률 분포",
                "다양한 조합 생성 가능"
            ),
            List.of(
                "1~45번 전체 번호를 후보 풀로 생성",
                "제약 조건(포함/제외 번호) 적용",
                "메트릭 데이터 기반으로 빈도 가중치 계산",
                "메트릭 데이터 기반으로 과거 데이터(overdue) 가중치 계산",
                "빈도 가중치 50% + 과거 데이터 가중치 50%로 결합",
                "결합된 가중치를 기반으로 확률 분포 생성",
                "확률 분포에 따라 랜덤하게 번호 추출",
                "메트릭 데이터가 없는 경우 일반 랜덤 선택"
            ),
            List.of(
                "통계 기반이지만 완전히 결정적이지 않은 조합을 원하는 경우",
                "빈도와 과거 데이터를 모두 고려한 랜덤 추출을 원하는 경우",
                "다양한 조합을 생성하면서도 통계적 패턴을 반영하고 싶은 경우"
            ),
            null
        );
        
        // PATTERN_MATCHER
        createStrategyDescription(
            "PATTERN_MATCHER",
            "패턴 필터링",
            "패턴 필터링: 과거 당첨 데이터의 패턴(총합, 홀짝비, 고저비, 연속수 등)을 분석하여, 그 패턴과 일치하는 조합만 생성합니다. 말이 안 되는 조합을 걸러내는 전략입니다.",
            "과거 당첨 데이터의 패턴(총합, 홀짝비, 고저비, 연속수 등)을 분석하여, 그 패턴과 일치하는 조합만 생성합니다. 말이 안 되는 조합을 걸러내는 전략입니다.",
            List.of(
                "과거 당첨 데이터의 통계적 패턴 분석",
                "총합, 홀짝비, 고저비, 연속수 등 다차원 패턴 검증",
                "패턴 일치도 60% 이상인 조합만 선택",
                "최대 1000회 시도하여 패턴 일치 조합 생성",
                "말이 안 되는 조합 자동 필터링"
            ),
            List.of(
                "1~45번 전체 번호를 후보 풀로 생성",
                "제약 조건(포함/제외 번호) 적용",
                "과거 당첨 데이터의 패턴 통계 계산 (총합, 홀짝비, 고저비, 연속수)",
                "가중치 기반 또는 일반 랜덤으로 조합 생성",
                "생성된 조합의 패턴 분석",
                "과거 당첨 패턴과 일치도 계산 (총합 30%, 홀짝비 25%, 고저비 25%, 연속수 20%)",
                "일치도가 60% 이상이면 선택, 미만이면 재시도",
                "최대 1000회 시도하여 패턴 일치 조합 생성",
                "과거 당첨 데이터가 없으면 기본 패턴 통계 사용"
            ),
            List.of(
                "과거 당첨 번호와 유사한 패턴의 조합을 원하는 경우",
                "말이 안 되는 조합을 걸러내고 싶은 경우",
                "통계적으로 검증된 패턴을 따르는 조합을 원하는 경우",
                "AI가 800만 개의 조합 중 과거 당첨 패턴과 일치하는 조합만 엄선하고 싶은 경우"
            ),
            List.of(
                "패턴 일치도 임계값은 60%로 설정되어 있습니다",
                "과거 당첨 데이터가 많을수록 더 정확한 패턴 분석이 가능합니다",
                "최대 1000회 시도 내에 패턴 일치 조합을 찾지 못하면 일반 조합을 반환합니다"
            )
        );
        
        // AI_SIMULATION
        createStrategyDescription(
            "AI_SIMULATION",
            "AI 시뮬레이션 추천",
            "AI 시뮬레이션 추천: 시뮬레이션과 다차원 평가를 통한 최적 조합 선별입니다. 통계 기반 지능형 알고리즘으로 여러 조합을 생성하고 평가하여 최상의 조합을 선택합니다.",
            "시뮬레이션과 다차원 평가를 통한 최적 조합 선별입니다. 통계 기반 지능형 알고리즘으로 여러 조합을 생성하고 평가하여 최상의 조합을 선택합니다.",
            List.of(
                "다양한 조합을 생성하고 다차원 평가 수행",
                "통계 기반 지능형 알고리즘으로 최적 조합 선별",
                "시뮬레이션을 통한 조합 평가 및 최종 선택"
            ),
            List.of(
                "1~45번 전체 번호를 후보 풀로 생성",
                "제약 조건(포함/제외 번호) 적용",
                "여러 조합을 생성하고 각 조합을 다차원 평가",
                "평가 결과를 기반으로 최상의 조합 선별",
                "메트릭 데이터가 없는 경우 일반 랜덤 선택"
            ),
            List.of(
                "최적의 조합을 찾고 싶은 경우",
                "다차원 평가를 통한 지능형 선택을 원하는 경우",
                "시뮬레이션 기반 최적화를 원하는 경우"
            ),
            null
        );
        
        // AI_PATTERN_REASONER
        createStrategyDescription(
            "AI_PATTERN_REASONER",
            "AI 패턴 분석 추천",
            "AI 패턴 분석 추천: 과거 당첨 패턴과 유사도가 낮은 조합을 AI가 자동 제거합니다. 총합/홀짝/고저/연속수 패턴 학습 및 패턴 일치도 기반 선별입니다.",
            "과거 당첨 패턴과 유사도가 낮은 조합을 AI가 자동 제거합니다. 총합/홀짝/고저/연속수 패턴 학습 및 패턴 일치도 기반 선별입니다.",
            List.of(
                "과거 당첨 패턴 학습 및 분석",
                "총합, 홀짝비, 고저비, 연속수 등 다차원 패턴 분석",
                "패턴 유사도가 낮은 조합 자동 제거",
                "패턴 일치도 기반 선별"
            ),
            List.of(
                "1~45번 전체 번호를 후보 풀로 생성",
                "제약 조건(포함/제외 번호) 적용",
                "과거 당첨 데이터의 패턴 학습 (총합, 홀짝비, 고저비, 연속수)",
                "생성된 조합의 패턴 분석",
                "과거 당첨 패턴과 유사도 계산",
                "유사도가 낮은 조합 제거, 높은 조합 선별",
                "메트릭 데이터가 없는 경우 일반 랜덤 선택"
            ),
            List.of(
                "과거 당첨 패턴과 유사한 조합을 원하는 경우",
                "패턴 분석 기반 지능형 선택을 원하는 경우",
                "AI가 패턴을 학습하여 최적 조합을 선별하고 싶은 경우"
            ),
            null
        );
        
        // AI_DECISION_FILTER
        createStrategyDescription(
            "AI_DECISION_FILTER",
            "AI 판단 필터 추천",
            "AI 판단 필터 추천: AI가 \"이 조합은 버린다 / 남긴다\"를 판단합니다. 극단값 조합 자동 제거 및 말이 안 되는 조합 사전 차단입니다.",
            "AI가 \"이 조합은 버린다 / 남긴다\"를 판단합니다. 극단값 조합 자동 제거 및 말이 안 되는 조합 사전 차단입니다.",
            List.of(
                "AI 기반 조합 판단 및 필터링",
                "극단값 조합 자동 제거",
                "말이 안 되는 조합 사전 차단",
                "지능형 필터링을 통한 최적 조합 선별"
            ),
            List.of(
                "1~45번 전체 번호를 후보 풀로 생성",
                "제약 조건(포함/제외 번호) 적용",
                "생성된 조합을 AI 판단 기준으로 평가",
                "극단값 조합 (총합이 너무 크거나 작은 경우 등) 자동 제거",
                "말이 안 되는 조합 (비정상적인 패턴) 사전 차단",
                "AI 판단 기준을 통과한 조합만 선별",
                "메트릭 데이터가 없는 경우 일반 랜덤 선택"
            ),
            List.of(
                "AI가 조합을 판단하여 최적의 조합을 선별하고 싶은 경우",
                "극단값이나 비정상적인 조합을 제거하고 싶은 경우",
                "지능형 필터링을 통한 고품질 조합을 원하는 경우"
            ),
            null
        );
        
        // AI_WEIGHT_EVOLUTION
        createStrategyDescription(
            "AI_WEIGHT_EVOLUTION",
            "AI 가중치 진화 추천",
            "AI 가중치 진화 추천: AI가 빈도·과거 데이터 가중치를 스스로 조정합니다. 패턴 적합도에 따라 가중치가 변화하는 적응형 전략입니다.",
            "AI가 빈도·과거 데이터 가중치를 스스로 조정합니다. 패턴 적합도에 따라 가중치가 변화하는 적응형 전략입니다.",
            List.of(
                "AI 기반 가중치 자동 조정",
                "빈도와 과거 데이터 가중치를 동적으로 변화",
                "패턴 적합도에 따른 적응형 가중치 조정",
                "최적의 가중치 조합을 찾는 진화 알고리즘"
            ),
            List.of(
                "1~45번 전체 번호를 후보 풀로 생성",
                "제약 조건(포함/제외 번호) 적용",
                "메트릭 데이터 기반으로 빈도 가중치 계산",
                "메트릭 데이터 기반으로 과거 데이터(overdue) 가중치 계산",
                "AI가 패턴 적합도를 분석하여 가중치 비율 자동 조정",
                "조정된 가중치를 기반으로 확률 분포 생성",
                "확률 분포에 따라 랜덤하게 번호 추출",
                "메트릭 데이터가 없는 경우 일반 랜덤 선택"
            ),
            List.of(
                "AI가 가중치를 자동으로 조정하여 최적 조합을 찾고 싶은 경우",
                "적응형 알고리즘을 통한 지능형 선택을 원하는 경우",
                "패턴에 따라 가중치가 진화하는 전략을 원하는 경우"
            ),
            null
        );
        
        log.info("전략 설명 초기 데이터 삽입 완료 - 총 {}개 전략", allStrategyCodes.size());
    }
    
    private void createStrategyDescription(
            String strategyCode,
            String title,
            String shortDescription,
            String description,
            List<String> features,
            List<String> algorithm,
            List<String> scenarios,
            List<String> notes) {
        try {
            // 이미 존재하는지 확인
            if (strategyDescriptionRepository.findByStrategyCode(strategyCode).isPresent()) {
                log.debug("전략 설명이 이미 존재합니다: {}", strategyCode);
                return;
            }
            
            StrategyDescriptionEntity entity = StrategyDescriptionEntity.builder()
                    .strategyCode(strategyCode)
                    .title(title)
                    .shortDescription(shortDescription)
                    .description(description)
                    .features(objectMapper.writeValueAsString(features))
                    .algorithm(objectMapper.writeValueAsString(algorithm))
                    .scenarios(objectMapper.writeValueAsString(scenarios))
                    .notes(notes != null ? objectMapper.writeValueAsString(notes) : null)
                    .contentHash(generateContentHash(
                            title,
                            shortDescription,
                            description,
                            objectMapper.writeValueAsString(features),
                            objectMapper.writeValueAsString(algorithm),
                            objectMapper.writeValueAsString(scenarios),
                            notes != null ? objectMapper.writeValueAsString(notes) : ""
                    )) // 내용 기반 해시 일련번호
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            strategyDescriptionRepository.save(entity);
            log.info("전략 설명 초기화 완료: {}", strategyCode);
        } catch (Exception e) {
            log.error("전략 설명 초기화 실패: {}", strategyCode, e);
        }
    }
    
    /**
     * 전략 설명 내용을 기반으로 해시 일련번호 생성 (SHA-256)
     */
    private String generateContentHash(
            String title,
            String shortDescription,
            String description,
            String features,
            String algorithm,
            String scenarios,
            String notes) {
        try {
            // 모든 내용을 하나의 문자열로 결합
            String content = String.join("|",
                    title != null ? title : "",
                    shortDescription != null ? shortDescription : "",
                    description != null ? description : "",
                    features != null ? features : "",
                    algorithm != null ? algorithm : "",
                    scenarios != null ? scenarios : "",
                    notes != null ? notes : ""
            );
            
            // SHA-256 해시 생성
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            // 16진수 문자열로 변환 (64자)
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("해시 생성 실패", e);
            // 해시 생성 실패 시 타임스탬프 기반 일련번호 생성
            return String.valueOf(System.currentTimeMillis());
        } catch (Exception e) {
            log.error("해시 생성 중 오류 발생", e);
            return String.valueOf(System.currentTimeMillis());
        }
    }
}
