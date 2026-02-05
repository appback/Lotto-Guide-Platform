import React, { useState, useEffect, useRef } from 'react';
import { Button, Card, Space, Select, InputNumber, message, Spin, Alert, Modal, Typography, Divider, Radio } from 'antd';
import { generateService, type StrategyDescription } from '@/services/generateService';
import type { GeneratedSetDto } from '@/types/api';
import { InfoCircleOutlined } from '@ant-design/icons';
import { LottoBallGroup } from '@/components/LottoBall';

const { Title, Paragraph, Text } = Typography;

const { Option } = Select;

const STORAGE_KEY_BALL_SIZE = 'lotto_ball_size';

type BallSize = 'small' | 'medium' | 'large';



type AIStrategy = 'AI_SIMULATION' | 'AI_PATTERN_REASONER' | 'AI_DECISION_FILTER' | 'AI_WEIGHT_EVOLUTION';

// AI 전략별 간단 설명 (기본값, API에서 가져오지 못할 경우 사용)
const defaultAiStrategyTips: Record<string, string> = {
  AI_SIMULATION: 'AI 시뮬레이션 추천: 수백 번의 시뮬레이션을 통해 최적의 조합을 탐색합니다. 다차원 평가를 통해 최상의 번호를 선별합니다.',
  AI_PATTERN_REASONER: 'AI 패턴 분석 추천: 과거 당첨 패턴과 유사도가 낮은 조합을 자동 제거합니다. 패턴 일치도 기반으로 엄선된 조합만 생성합니다.',
  AI_DECISION_FILTER: 'AI 판단 필터 추천: 극단값 조합을 자동 제거하고 말이 안 되는 조합을 사전 차단합니다. AI가 유효한 조합만 선별합니다.',
  AI_WEIGHT_EVOLUTION: 'AI 가중치 진화 추천: 패턴 적합도에 따라 가중치를 스스로 조정하는 적응형 전략입니다. 상황에 맞게 최적의 가중치를 자동 계산합니다.',
};

// AI 전략별 상세 알고리즘 설명 (기본값, API에서 가져오지 못할 경우 사용)
const defaultAiStrategyDetails: Record<string, {
  title: string;
  description: string;
  features: string[];
  algorithm: string[];
  scenarios: string[];
  notes?: string[];
}> = {
  AI_SIMULATION: {
    title: 'AI 시뮬레이션 추천 (AI_SIMULATION)',
    description: '수백 번의 시뮬레이션을 통해 최적의 조합을 탐색합니다. 다차원 평가를 통해 최상의 번호를 선별합니다.',
    features: [
      '500개 조합 생성 및 다차원 평가',
      '패턴 일치도, 빈도, 과거 데이터, 다양성 종합 점수',
      '상위 조합 중 최적의 번호 선별',
      '통계 기반 지능형 알고리즘'
    ],
    algorithm: [
      '1~45번 전체 번호를 후보 풀로 생성',
      '제약 조건(포함/제외 번호) 적용',
      '500개의 다양한 조합 생성 (빈도 우선, 과거 데이터 우선, 균형 가중치)',
      '각 조합에 대해 다차원 평가 점수 계산 (패턴 일치도 40%, 빈도 30%, 과거 데이터 20%, 다양성 10%)',
      '점수 기준 정렬하여 상위 50개 조합 선별',
      '상위 조합 중 랜덤 선택하여 최종 번호 결정'
    ],
    scenarios: [
      '최적의 조합을 찾고 싶은 경우',
      '다차원 평가를 통한 정교한 번호 생성이 필요한 경우',
      '통계 기반 지능형 알고리즘을 원하는 경우'
    ]
  },
  AI_PATTERN_REASONER: {
    title: 'AI 패턴 분석 추천 (AI_PATTERN_REASONER)',
    description: '과거 당첨 패턴과 유사도가 낮은 조합을 자동 제거합니다. 패턴 일치도 기반으로 엄선된 조합만 생성합니다.',
    features: [
      '패턴 일치도 65% 이상 조합만 선별',
      '총합/홀짝/고저/연속수 패턴 학습',
      '200개 후보 조합 생성 및 필터링',
      '통계 기반 패턴 분석'
    ],
    algorithm: [
      '1~45번 전체 번호를 후보 풀로 생성',
      '제약 조건(포함/제외 번호) 적용',
      '200개의 후보 조합 생성',
      '각 조합의 패턴 일치도 계산 (총합 30%, 홀짝비 25%, 고저비 25%, 연속수 20%)',
      '패턴 일치도가 65% 이상인 조합만 선택',
      '선별된 조합 중 상위 10개 중 랜덤 선택'
    ],
    scenarios: [
      '과거 당첨 패턴과 유사한 조합을 원하는 경우',
      '패턴 기반으로 엄선된 번호를 원하는 경우',
      '통계적으로 검증된 패턴을 따르는 조합을 원하는 경우'
    ]
  },
  AI_DECISION_FILTER: {
    title: 'AI 판단 필터 추천 (AI_DECISION_FILTER)',
    description: '극단값 조합을 자동 제거하고 말이 안 되는 조합을 사전 차단합니다. AI가 유효한 조합만 선별합니다.',
    features: [
      '극단값 조합 자동 제거',
      '말이 안 되는 조합 사전 차단',
      '300개 후보 조합 생성 및 필터링',
      '다차원 유효성 검증'
    ],
    algorithm: [
      '1~45번 전체 번호를 후보 풀로 생성',
      '제약 조건(포함/제외 번호) 적용',
      '300개의 후보 조합 생성',
      'AI 판단 필터링: 총합 범위 검증 (60~200)',
      'AI 판단 필터링: 연속수 검증 (4개 이상 제거)',
      'AI 판단 필터링: 번호 집중도 검증 (10개 범위에 5개 이상 제거)',
      'AI 판단 필터링: 홀짝 비율 검증 (0:6 또는 6:0 제거)',
      'AI 판단 필터링: 고저 비율 검증 (0:6 또는 6:0 제거)',
      '필터링된 조합 중 랜덤 선택'
    ],
    scenarios: [
      '말이 안 되는 조합을 걸러내고 싶은 경우',
      '극단적인 조합을 피하고 싶은 경우',
      '유효한 조합만 생성하고 싶은 경우'
    ]
  },
  AI_WEIGHT_EVOLUTION: {
    title: 'AI 가중치 진화 추천 (AI_WEIGHT_EVOLUTION)',
    description: '패턴 적합도에 따라 가중치를 스스로 조정하는 적응형 전략입니다. 상황에 맞게 최적의 가중치를 자동 계산합니다.',
    features: [
      '패턴 적합도 기반 가중치 자동 조정',
      '빈도와 과거 데이터 가중치 동적 변화',
      '400개 조합 생성 및 다차원 평가',
      '적응형 전략'
    ],
    algorithm: [
      '1~45번 전체 번호를 후보 풀로 생성',
      '제약 조건(포함/제외 번호) 적용',
      '패턴 통계 기반 최적 가중치 계산 (빈도:과거 데이터 비율 자동 조정)',
      '400개의 조합 생성 (적응형 가중치 사용)',
      '각 조합에 대해 다차원 평가 점수 계산',
      '점수 기준 정렬하여 상위 30개 조합 선별',
      '상위 조합 중 랜덤 선택하여 최종 번호 결정'
    ],
    scenarios: [
      '상황에 맞게 자동으로 최적화된 번호를 원하는 경우',
      '적응형 전략을 선호하는 경우',
      '패턴에 따라 가중치가 변화하는 전략을 원하는 경우'
    ]
  }
};

// AI 전략별 기본 게임 개수
const aiStrategyDefaultCounts: Record<string, number> = {
  AI_SIMULATION: 5,
  AI_PATTERN_REASONER: 5,
  AI_DECISION_FILTER: 5,
  AI_WEIGHT_EVOLUTION: 5,
};

export const AIGeneratePage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [generatedSets, setGeneratedSets] = useState<GeneratedSetDto[]>([]);
  const [strategy, setStrategy] = useState<AIStrategy>('AI_SIMULATION');
  const [count, setCount] = useState<number>(aiStrategyDefaultCounts['AI_SIMULATION']);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [strategyTips, setStrategyTips] = useState<Record<string, string>>(defaultAiStrategyTips);
  const [strategyDetails, setStrategyDetails] = useState<Record<string, StrategyDescription>>({});
  const windowSize = 50;
  // 마지막 생성 요청 시간 추적 (3초 딜레이)
  const lastGenerateTimeRef = useRef<number>(0);
  // 버튼 비활성화 및 카운트다운 상태
  const [buttonCooldown, setButtonCooldown] = useState<number>(0);
  
  // 볼 크기 상태 (localStorage에서 불러오기)
  const [ballSize, setBallSize] = useState<BallSize>(() => {
    const saved = localStorage.getItem(STORAGE_KEY_BALL_SIZE);
    return (saved === 'small' || saved === 'medium' || saved === 'large') ? saved : 'medium';
  });

  // AI 전략 설명 로드
  useEffect(() => {
    const loadStrategyDescriptions = async () => {
      try {
        const response = await generateService.getStrategyDescriptions();
        if (response.success && response.data) {
          // AI 전략만 필터링
          const tips: Record<string, string> = { ...defaultAiStrategyTips };
          const details: Record<string, StrategyDescription> = {};
          
          Object.entries(response.data).forEach(([code, desc]) => {
            if (code.startsWith('AI_')) {
              tips[code] = desc.shortDescription;
              details[code] = desc;
            }
          });
          
          setStrategyTips(tips);
          setStrategyDetails(details);
        }
      } catch (error) {
        console.error('AI 전략 설명 로드 실패:', error);
        // 기본값 사용
      }
    };
    
    loadStrategyDescriptions();
  }, []);



  // 버튼 쿨다운 카운트다운
  useEffect(() => {
    if (buttonCooldown > 0) {
      const timer = setInterval(() => {
        setButtonCooldown((prev) => {
          if (prev <= 0.1) {
            return 0;
          }
          return prev - 0.1;
        });
      }, 100);
      return () => clearInterval(timer);
    }
  }, [buttonCooldown]);

  // AI 전략 변경 시 해당 전략의 기본 게임 개수로 자동 설정
  const handleStrategyChange = (value: AIStrategy) => {
    setStrategy(value);
    const defaultCount = aiStrategyDefaultCounts[value] || 5;
    setCount(defaultCount);
  };

  // 볼 크기 변경 핸들러
  const handleBallSizeChange = (e: any) => {
    const newSize = e.target.value as BallSize;
    setBallSize(newSize);
    localStorage.setItem(STORAGE_KEY_BALL_SIZE, newSize);
  };

  const handleAIGenerate = async () => {
    // 3초 딜레이 체크
    const now = Date.now();
    const timeSinceLastGenerate = now - lastGenerateTimeRef.current;
    const DELAY_MS = 3000; // 3초
    
    if (timeSinceLastGenerate < DELAY_MS) {
      const remainingSeconds = (DELAY_MS - timeSinceLastGenerate) / 1000;
      setButtonCooldown(remainingSeconds);
      return;
    }
    
    lastGenerateTimeRef.current = now;
    setButtonCooldown(3); // 3초 쿨다운 시작
    setLoading(true);
    setGeneratedSets([]);
    
    try {
      // 백엔드에서 3-5초 대기를 처리하므로 프론트엔드는 바로 API 호출
      const generateResponse = await generateService.generate({
        strategy,
        count,
        windowSize,
      });
      
      setGeneratedSets(generateResponse.generatedSets);
      message.success('AI 기반 번호 생성 완료!');
    } catch (error: any) {
      console.error('번호 생성 오류:', error);
      message.error(error?.message || '번호 생성에 실패했습니다.');
      setGeneratedSets([]);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div>
      <Card title="AI 기반 추천 (유료)">
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          {strategy && (
            <Alert
              message={
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  <span>{strategyTips[strategy]}</span>
                  <Button 
                    type="link" 
                    size="small" 
                    onClick={() => setDetailModalVisible(true)}
                    style={{ padding: 0, height: 'auto', alignSelf: 'flex-start' }}
                  >
                    상세보기
                  </Button>
                </div>
              }
              type="info"
              icon={<InfoCircleOutlined />}
              showIcon
              style={{ marginTop: '8px' }}
            />
          )}
          <Space>
            <span>AI 전략:</span>
            <Select
              value={strategy}
              onChange={handleStrategyChange}
              style={{ width: 200 }}
            >
              {(['AI_SIMULATION', 'AI_PATTERN_REASONER', 'AI_DECISION_FILTER', 'AI_WEIGHT_EVOLUTION'] as const).map((code) => {
                const displayName = strategyDetails[code]?.title || defaultAiStrategyDetails[code]?.title || code;
                // title에서 괄호와 코드 제거 (예: "AI 시뮬레이션 추천 (AI_SIMULATION)" -> "AI 시뮬레이션 추천")
                const cleanName = displayName.replace(/\s*\([^)]*\)\s*$/, '').trim();
                return <Option key={code} value={code}>{cleanName}</Option>;
              })}
            </Select>
          </Space>

          <Space>
            <span>생성 개수:</span>
            <InputNumber
              min={1}
              max={20}
              value={count}
              onChange={(value) => setCount(value || 5)}
            />
          </Space>

          <Space>
            <span>볼 크기:</span>
            <Radio.Group 
              value={ballSize} 
              onChange={handleBallSizeChange}
              size="small"
              buttonStyle="solid"
            >
              <Radio.Button value="small">작게</Radio.Button>
              <Radio.Button value="medium">보통</Radio.Button>
              <Radio.Button value="large">크게</Radio.Button>
            </Radio.Group>
          </Space>

          <Button
            type="primary"
            size="large"
            onClick={handleAIGenerate}
            loading={buttonCooldown > 0 || loading}
            disabled={buttonCooldown > 0 || loading}
          >
            AI 번호 생성
          </Button>
        </Space>
      </Card>

      {loading && (
        <div style={{ textAlign: 'center', marginTop: '24px' }}>
          <Spin size="large" />
        </div>
      )}

      {!loading && generatedSets.length > 0 && (
        <div style={{ marginTop: '24px' }}>
          <Card title="생성된 번호">
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              {generatedSets.map((set, index) => (
                <Card key={index} size="small" title={`세트 ${set.index + 1}`}>
                  <LottoBallGroup 
                    numbers={set.numbers} 
                    size={ballSize}
                    animated={true}
                    gap={ballSize === 'small' ? 5 : 12}
                  />
                </Card>
              ))}
            </Space>
          </Card>
        </div>
      )}

      {/* AI 전략 상세 알고리즘 모달 */}
      <Modal
        title={strategy && strategyDetails[strategy] ? strategyDetails[strategy].title : 
               (strategy && defaultAiStrategyDetails[strategy] ? defaultAiStrategyDetails[strategy].title : 'AI 전략 상세 정보')}
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            닫기
          </Button>
        ]}
        width={700}
      >
        {strategy && (strategyDetails[strategy] || defaultAiStrategyDetails[strategy]) && (
          <div>
            {(() => {
              const detail = strategyDetails[strategy] || defaultAiStrategyDetails[strategy];
              return (
                <>
                  <Paragraph>
                    <Text strong>{detail.description}</Text>
                  </Paragraph>

                  <Divider />

                  <Title level={5}>특징</Title>
                  <ul>
                    {detail.features.map((feature, index) => (
                      <li key={index}>{feature}</li>
                    ))}
                  </ul>

                  <Divider />

                  <Title level={5}>알고리즘</Title>
                  <ol>
                    {detail.algorithm.map((step, index) => (
                      <li key={index} style={{ marginBottom: '8px' }}>{step}</li>
                    ))}
                  </ol>

                  <Divider />

                  <Title level={5}>적용 시나리오</Title>
                  <ul>
                    {detail.scenarios.map((scenario, index) => (
                      <li key={index}>{scenario}</li>
                    ))}
                  </ul>

                  {detail.notes && (
                    <>
                      <Divider />
                      <Title level={5}>주의사항</Title>
                      <ul>
                        {detail.notes.map((note, index) => (
                          <li key={index} style={{ color: '#ff4d4f' }}>{note}</li>
                        ))}
                      </ul>
                    </>
                  )}
                </>
              );
            })()}
          </div>
        )}
      </Modal>
    </div>
  );
};
