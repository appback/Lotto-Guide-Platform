import React, { useState, useEffect, useRef } from 'react';
import { Button, Card, Space, Select, InputNumber, message, Spin, Alert, Modal, Typography, Divider, Radio } from 'antd';
import { generateService, type StrategyDescription } from '@/services/generateService';
import type { GeneratedSetDto } from '@/types/api';
// import { useNavigate } from 'react-router-dom'; // 탭 기반으로 변경되어 제거
import { InfoCircleOutlined } from '@ant-design/icons';
import { LottoBallGroup } from '@/components/LottoBall';

const { Title, Paragraph, Text } = Typography;

const { Option } = Select;

const STORAGE_KEY_BALL_SIZE = 'lotto_ball_size';
type BallSize = 'small' | 'medium' | 'large';

// 전략별 간단 설명 (기본값, API에서 가져오지 못할 경우 사용)
const defaultStrategyTips: Record<string, string> = {
  FREQUENT_TOP: '고빈도 우선: 과거 당첨 번호 중 자주 나온 번호를 우선적으로 선택합니다. 통계적으로 자주 등장한 번호에 집중하는 전략입니다.',
  OVERDUE_TOP: '과거 데이터 우선: 최근에 나오지 않은 번호를 우선적으로 선택합니다. 오랫동안 나오지 않은 번호에 집중하는 전략입니다.',
  BALANCED: '균형: 고빈도 번호와 과거 데이터를 균형있게 조합하여 선택합니다. 다양한 번호를 포함하는 전략입니다.',
  WHEELING_SYSTEM: '추천조합1 (기본 14게임): 5등(번호 3개 일치) 보장 조합입니다. 통계적으로 가장 안 나온 9개 번호를 제외한 36개 번호로 조합을 생성합니다. 기본값은 14게임이며, 5등 보장을 위해 14게임을 권장합니다. 게임 개수는 변경 가능합니다.',
  WEIGHTED_RANDOM: '가중치 기반 생성: 빈도와 과거 데이터를 결합한 가중치를 사용하여 번호를 랜덤하게 추출합니다. 완전한 무작위가 아니라 통계 기반 확률로 번호를 선택합니다.',
  PATTERN_MATCHER: '패턴 필터링: 과거 당첨 데이터의 패턴(총합, 홀짝비, 고저비, 연속수 등)을 분석하여, 그 패턴과 일치하는 조합만 생성합니다. 말이 안 되는 조합을 걸러내는 전략입니다.',
};

// 전략별 상세 알고리즘 설명 (기본값, API에서 가져오지 못할 경우 사용)
const defaultStrategyDetails: Record<string, {
  title: string;
  description: string;
  features: string[];
  algorithm: string[];
  scenarios: string[];
  notes?: string[];
}> = {
  FREQUENT_TOP: {
    title: '고빈도 우선 (FREQUENT_TOP)',
    description: '과거 당첨 번호 중 자주 나온 번호를 우선적으로 선택합니다. 통계적으로 자주 등장한 번호에 집중하는 전략입니다.',
    features: [
      '최근 N회 추첨(기본 50회)에서 고빈도 번호를 우선 선택',
      '빈도가 높을수록 높은 가중치를 부여하여 선택 확률 증가',
      '통계적 패턴을 활용한 번호 선택'
    ],
    algorithm: [
      '1~45번 전체 번호를 후보 풀로 생성',
      '제약 조건(포함/제외 번호) 적용',
      '메트릭 데이터 기반으로 각 번호의 빈도(frequency) 계산',
      '빈도가 높은 순서로 정렬하여 상위 6개 선택',
      '메트릭 데이터가 없는 경우 랜덤 선택'
    ],
    scenarios: [
      '통계적으로 검증된 번호를 선호하는 경우',
      '과거 당첨 패턴을 신뢰하는 경우'
    ]
  },
  OVERDUE_TOP: {
    title: '과거 데이터 우선 (OVERDUE_TOP)',
    description: '최근에 나오지 않은 번호를 우선적으로 선택합니다. 오랫동안 나오지 않은 번호에 집중하는 전략입니다.',
    features: [
      '최근에 나오지 않은 번호(overdue 값이 큰 번호) 우선 선택',
      '오래 안 나온 번호일수록 높은 가중치 부여',
      '"나올 차례"라는 가설에 기반한 선택'
    ],
    algorithm: [
      '1~45번 전체 번호를 후보 풀로 생성',
      '제약 조건(포함/제외 번호) 적용',
      '메트릭 데이터 기반으로 각 번호의 과거 데이터(overdue) 계산',
      'overdue 값이 큰 순서로 정렬하여 상위 6개 선택',
      '메트릭 데이터가 없는 경우 랜덤 선택'
    ],
    scenarios: [
      '오래 나오지 않은 번호가 곧 나올 것이라고 믿는 경우',
      '역발상 전략을 선호하는 경우'
    ]
  },
  BALANCED: {
    title: '균형 (BALANCED)',
    description: '고빈도 번호와 과거 데이터를 균형있게 조합하여 선택합니다. 다양한 번호를 포함하는 전략입니다.',
    features: [
      '고빈도와 과거 데이터를 모두 고려한 균형잡힌 선택',
      '제약 조건(홀수/짝수 비율, 합계 범위 등) 적용 가능',
      '가장 일반적이고 안정적인 전략'
    ],
    algorithm: [
      '1~45번 전체 번호를 후보 풀로 생성',
      '제약 조건(포함/제외 번호, 홀수/짝수 비율, 합계 범위) 적용',
      '제약 조건을 만족하는 조합을 랜덤으로 생성',
      '최대 1000회 시도하여 제약 조건을 만족하는 조합 선택',
      '제약 조건을 만족하지 못하면 기본 랜덤 선택'
    ],
    scenarios: [
      '가장 일반적인 사용 케이스',
      '특정 제약 조건을 적용하고 싶은 경우',
      '다양한 번호 조합을 원하는 경우'
    ]
  },
  WHEELING_SYSTEM: {
    title: '추천조합1 (기본 14게임) (WHEELING_SYSTEM)',
    description: '5등(번호 3개 일치) 보장 조합입니다. 통계적으로 가장 안 나온 9개 번호를 제외한 36개 번호로 조합을 생성합니다.',
    features: [
      '기본 게임 개수: 14게임 (변경 가능, 5등 보장을 위해 권장)',
      '5등(3개 일치) 보장 알고리즘 (14게임 기준)',
      '통계적으로 가장 안 나온 9개 번호 자동 제외',
      '36개 번호를 요청한 개수만큼 조합에 균등하게 분산'
    ],
    algorithm: [
      'Step 1: 번호 제외 - 최근 N회 추첨(기본 50회)에서 통계적으로 가장 안 나온 9개 번호 제외',
      'Step 2: 36개 번호 선택 - 전체 45개 번호에서 제외된 9개를 뺀 36개 번호 사용',
      'Step 3: 조합 생성 - Round-Robin 방식으로 36개 번호를 요청한 개수만큼 조합에 균등하게 분산',
      '각 번호가 여러 조합에 포함되도록 구성하여 3개 일치를 보장',
      '5등 보장을 위해서는 14개 조합 권장'
    ],
    scenarios: [
      '5등 이상 당첨을 보장하고 싶은 경우',
      '여러 게임을 구매하여 당첨 확률을 높이고 싶은 경우',
      '통계적으로 검증된 번호 조합을 원하는 경우'
    ],
    notes: [
      '기본 게임 개수는 14개이지만 변경 가능합니다',
      '5등 보장을 위해서는 14게임을 권장합니다 (14개 미만일 경우 보장이 깨질 수 있음)',
      '5등 보장은 "당첨 번호 6개가 36개 번호에 포함되어 있다"는 가정 하에 성립',
      '실제 당첨 번호가 제외된 9개 번호에 포함되면 보장이 깨질 수 있음'
    ]
  },
  WEIGHTED_RANDOM: {
    title: '가중치 기반 생성 (WEIGHTED_RANDOM)',
    description: '빈도와 과거 데이터를 결합한 가중치를 사용하여 번호를 랜덤하게 추출합니다. 완전한 무작위가 아니라 통계 기반 확률로 번호를 선택합니다.',
    features: [
      '빈도와 과거 데이터를 50:50으로 결합한 가중치 사용',
      '가중치가 높은 번호일수록 선택 확률이 높음',
      '완전한 무작위가 아닌 통계 기반 확률 분포',
      '다양한 조합 생성 가능'
    ],
    algorithm: [
      '1~45번 전체 번호를 후보 풀로 생성',
      '제약 조건(포함/제외 번호) 적용',
      '메트릭 데이터 기반으로 빈도 가중치 계산',
      '메트릭 데이터 기반으로 과거 데이터(overdue) 가중치 계산',
      '빈도 가중치 50% + 과거 데이터 가중치 50%로 결합',
      '결합된 가중치를 기반으로 확률 분포 생성',
      '확률 분포에 따라 랜덤하게 번호 추출',
      '메트릭 데이터가 없는 경우 일반 랜덤 선택'
    ],
    scenarios: [
      '통계 기반이지만 완전히 결정적이지 않은 조합을 원하는 경우',
      '빈도와 과거 데이터를 모두 고려한 랜덤 추출을 원하는 경우',
      '다양한 조합을 생성하면서도 통계적 패턴을 반영하고 싶은 경우'
    ]
  },
  PATTERN_MATCHER: {
    title: '패턴 필터링 (PATTERN_MATCHER)',
    description: '과거 당첨 데이터의 패턴(총합, 홀짝비, 고저비, 연속수 등)을 분석하여, 그 패턴과 일치하는 조합만 생성합니다. 말이 안 되는 조합을 걸러내는 전략입니다.',
    features: [
      '과거 당첨 데이터의 통계적 패턴 분석',
      '총합, 홀짝비, 고저비, 연속수 등 다차원 패턴 검증',
      '패턴 일치도 60% 이상인 조합만 선택',
      '최대 1000회 시도하여 패턴 일치 조합 생성',
      '말이 안 되는 조합 자동 필터링'
    ],
    algorithm: [
      '1~45번 전체 번호를 후보 풀로 생성',
      '제약 조건(포함/제외 번호) 적용',
      '과거 당첨 데이터의 패턴 통계 계산 (총합, 홀짝비, 고저비, 연속수)',
      '가중치 기반 또는 일반 랜덤으로 조합 생성',
      '생성된 조합의 패턴 분석',
      '과거 당첨 패턴과 일치도 계산 (총합 30%, 홀짝비 25%, 고저비 25%, 연속수 20%)',
      '일치도가 60% 이상이면 선택, 미만이면 재시도',
      '최대 1000회 시도하여 패턴 일치 조합 생성',
      '과거 당첨 데이터가 없으면 기본 패턴 통계 사용'
    ],
    scenarios: [
      '과거 당첨 번호와 유사한 패턴의 조합을 원하는 경우',
      '말이 안 되는 조합을 걸러내고 싶은 경우',
      '통계적으로 검증된 패턴을 따르는 조합을 원하는 경우',
      'AI가 800만 개의 조합 중 과거 당첨 패턴과 일치하는 조합만 엄선하고 싶은 경우'
    ],
    notes: [
      '패턴 일치도 임계값은 60%로 설정되어 있습니다',
      '과거 당첨 데이터가 많을수록 더 정확한 패턴 분석이 가능합니다',
      '최대 1000회 시도 내에 패턴 일치 조합을 찾지 못하면 일반 조합을 반환합니다'
    ]
  }
};

// 전략별 기본 게임 개수
const strategyDefaultCounts: Record<string, number> = {
  FREQUENT_TOP: 5,
  OVERDUE_TOP: 5,
  BALANCED: 5,
  WHEELING_SYSTEM: 14,
  WEIGHTED_RANDOM: 5,
  PATTERN_MATCHER: 5,
};

export const GeneratePage: React.FC = () => {
  // const navigate = useNavigate(); // 탭 기반으로 변경되어 제거
  const [loading, setLoading] = useState(false);
  const [generatedSets, setGeneratedSets] = useState<GeneratedSetDto[]>([]);
  const [strategy, setStrategy] = useState<'FREQUENT_TOP' | 'OVERDUE_TOP' | 'BALANCED' | 'WHEELING_SYSTEM' | 'WEIGHTED_RANDOM' | 'PATTERN_MATCHER'>('BALANCED');
  const [count, setCount] = useState<number>(strategyDefaultCounts['BALANCED']);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [strategyTips, setStrategyTips] = useState<Record<string, string>>(defaultStrategyTips);
  const [strategyDetails, setStrategyDetails] = useState<Record<string, StrategyDescription>>({});
  // 윈도우 사이즈는 고정값 사용 (선택 불가)
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

  // 전략 설명 로드
  useEffect(() => {
    const loadStrategyDescriptions = async () => {
      try {
        const response = await generateService.getStrategyDescriptions();
        if (response.success && response.data) {
          // 간단 설명 업데이트
          const tips: Record<string, string> = { ...defaultStrategyTips };
          const details: Record<string, StrategyDescription> = {};
          
          Object.entries(response.data).forEach(([code, desc]) => {
            tips[code] = desc.shortDescription;
            details[code] = desc;
          });
          
          setStrategyTips(tips);
          setStrategyDetails(details);
        }
      } catch (error) {
        console.error('전략 설명 로드 실패:', error);
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

  // 전략 변경 시 해당 전략의 기본 게임 개수로 자동 설정
  const handleStrategyChange = (value: 'FREQUENT_TOP' | 'OVERDUE_TOP' | 'BALANCED' | 'WHEELING_SYSTEM' | 'WEIGHTED_RANDOM' | 'PATTERN_MATCHER') => {
    setStrategy(value);
    const defaultCount = strategyDefaultCounts[value] || 5;
    setCount(defaultCount);
  };

  // 볼 크기 변경 핸들러
  const handleBallSizeChange = (e: any) => {
    const newSize = e.target.value as BallSize;
    setBallSize(newSize);
    localStorage.setItem(STORAGE_KEY_BALL_SIZE, newSize);
  };

  const handleGenerate = async () => {
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
    try {
      const response = await generateService.generate({
        strategy,
        count,
        windowSize,
      });
      setGeneratedSets(response.generatedSets);
      message.success('번호 생성 완료!');
    } catch (error: any) {
      message.error(error.response?.data?.message || '번호 생성에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <Card title="로또 번호 추천">
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
        <span>전략:</span>
          <Select
              value={strategy}
              onChange={handleStrategyChange}
              style={{ width: 200 }}
            >
              {(['FREQUENT_TOP', 'OVERDUE_TOP', 'BALANCED', 'WHEELING_SYSTEM', 'WEIGHTED_RANDOM', 'PATTERN_MATCHER'] as const).map((code) => {
                const displayName = strategyDetails[code]?.title || defaultStrategyDetails[code]?.title || code;
                // title에서 괄호와 코드 제거 (예: "고빈도 우선 (FREQUENT_TOP)" -> "고빈도 우선")
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
            {strategy === 'WHEELING_SYSTEM' && (
              <span style={{ color: '#666', fontSize: '12px' }}>
                (기본 추천: 14게임, 5등 보장을 위해 권장)
              </span>
            )}
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
            onClick={handleGenerate}
            loading={buttonCooldown > 0 || loading}
            disabled={buttonCooldown > 0 || loading}
          >
            번호 생성
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

      {/* 전략 상세 알고리즘 모달 */}
      <Modal
        title={strategy && strategyDetails[strategy] ? strategyDetails[strategy].title : 
               (strategy && defaultStrategyDetails[strategy] ? defaultStrategyDetails[strategy].title : '전략 상세 정보')}
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            닫기
          </Button>
        ]}
        width={700}
      >
        {strategy && (strategyDetails[strategy] || defaultStrategyDetails[strategy]) && (
          <div>
            {(() => {
              const detail = strategyDetails[strategy] || defaultStrategyDetails[strategy];
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
