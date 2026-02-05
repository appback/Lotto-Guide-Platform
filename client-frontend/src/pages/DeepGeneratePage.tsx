import React, { useState, useEffect, useRef } from 'react';
import { Button, Card, Space, DatePicker, InputNumber, message, Spin, Typography, Select, Radio } from 'antd';
import { generateService } from '@/services/generateService';
import { missionService } from '@/services/missionService';
import { adminService } from '@/services/adminService';
import type { GeneratedSetDto, MissionResponse } from '@/types/api';
// import { useNavigate } from 'react-router-dom'; // 탭 기반으로 변경되어 제거
import type { Dayjs } from 'dayjs';
import { LottoBallGroup } from '@/components/LottoBall';

const { Paragraph } = Typography;
const { Option } = Select;

// 별자리 목록
const ZODIAC_SIGNS = [
  '염소자리',
  '물병자리',
  '물고기자리',
  '양자리',
  '황소자리',
  '쌍둥이자리',
  '게자리',
  '사자자리',
  '처녀자리',
  '천칭자리',
  '전갈자리',
  '사수자리',
] as const;

const STORAGE_KEY_LAST_ZODIAC = 'lotto_last_zodiac';
const STORAGE_KEY_BALL_SIZE = 'lotto_ball_size';
const STORAGE_KEY_DESTINY_LAST_USED = 'lotto_destiny_last_used_date';
const STORAGE_KEY_DESTINY_MESSAGES = 'lotto_destiny_messages';
const STORAGE_KEY_DESTINY_SERIAL = 'lotto_destiny_serial';

type BallSize = 'small' | 'medium' | 'large';

/**
 * 오늘 날짜를 YYYY-MM-DD 형식으로 반환
 */
const getTodayString = (): string => {
  const today = new Date();
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
};

/**
 * 오늘 사용 가능한지 확인 (자정 리셋)
 */
const canUseToday = (): boolean => {
  const lastUsed = localStorage.getItem(STORAGE_KEY_DESTINY_LAST_USED);
  const today = getTodayString();
  return lastUsed !== today;
};

/**
 * 운명의 번호 사용 완료 경고 메시지 목록 (기본값)
 */
const DEFAULT_DESTINY_LIMIT_MESSAGES = [
  '별들의 계시가 모두 전달되었습니다. 내일 다시 당신의 운명을 확인하세요.',
  '오늘의 운명은 이미 밝혀졌습니다. 내일 새로운 별의 인도를 받으세요.',
  '별자리의 메시지가 모두 전달되었습니다. 내일 다시 하늘의 계시를 받으세요.',
  '오늘의 운명의 번호는 이미 받으셨습니다. 내일 새로운 운명을 확인하세요.',
  '별들의 가르침이 모두 전해졌습니다. 내일 다시 당신의 별을 찾아보세요.',
  '오늘의 운명은 이미 결정되었습니다. 내일 새로운 운명의 길을 열어보세요.',
  '별자리의 계시가 모두 전달되었습니다. 내일 다시 당신의 운명을 만나보세요.',
];

/**
 * 기본 a 문구 목록
 */
const DEFAULT_PART_A_LIST = [
  '별들의 계시가 모두 전달되었습니다.',
  '오늘의 운명은 이미 밝혀졌습니다.',
  '별자리의 메시지가 모두 전달되었습니다.',
  '오늘의 운명의 번호는 이미 받으셨습니다.',
  '별들의 가르침이 모두 전해졌습니다.',
  '오늘의 운명은 이미 결정되었습니다.',
  '별자리의 계시가 모두 전달되었습니다.',
];

/**
 * 기본 b 문구 목록
 */
const DEFAULT_PART_B_LIST = [
  '내일 다시 당신의 운명을 확인하세요.',
  '내일 새로운 별의 인도를 받으세요.',
  '내일 다시 하늘의 계시를 받으세요.',
  '내일 새로운 운명을 확인하세요.',
  '내일 다시 당신의 별을 찾아보세요.',
  '내일 새로운 운명의 길을 열어보세요.',
  '내일 다시 당신의 운명을 만나보세요.',
];

/**
 * 로컬 스토리지에서 메시지 데이터 로드
 */
const loadDestinyMessagesFromStorage = (): { partAList: string[]; partBList: string[]; serialNumber: string } | null => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY_DESTINY_MESSAGES);
    const serial = localStorage.getItem(STORAGE_KEY_DESTINY_SERIAL);
    if (stored && serial) {
      const data = JSON.parse(stored);
      return {
        partAList: data.partAList || [],
        partBList: data.partBList || [],
        serialNumber: serial,
      };
    }
  } catch (error) {
    console.error('로컬 스토리지에서 메시지 로드 실패:', error);
  }
  return null;
};

/**
 * 로컬 스토리지에 메시지 데이터 저장
 */
const saveDestinyMessagesToStorage = (partAList: string[], partBList: string[], serialNumber: string): void => {
  try {
    localStorage.setItem(STORAGE_KEY_DESTINY_MESSAGES, JSON.stringify({ partAList, partBList }));
    localStorage.setItem(STORAGE_KEY_DESTINY_SERIAL, serialNumber);
  } catch (error) {
    console.error('로컬 스토리지에 메시지 저장 실패:', error);
  }
};

/**
 * 랜덤하게 a 문구와 b 문구를 조합하여 메시지 생성
 */
const generateRandomMessage = (partAList: string[], partBList: string[]): string => {
  if (partAList.length === 0 || partBList.length === 0) {
    // 기본값 사용
    const randomIndex = Math.floor(Math.random() * DEFAULT_DESTINY_LIMIT_MESSAGES.length);
    return DEFAULT_DESTINY_LIMIT_MESSAGES[randomIndex];
  }
  
  const randomA = partAList[Math.floor(Math.random() * partAList.length)];
  const randomB = partBList[Math.floor(Math.random() * partBList.length)];
  return `${randomA} ${randomB}`;
};

/**
 * 생년월일로부터 별자리 계산
 */
const calculateZodiacFromDate = (date: Dayjs | null): string | null => {
  if (!date) return null;
  
  const month = date.month() + 1; // dayjs는 0부터 시작
  const day = date.date();
  
  // 별자리 경계일 기준으로 계산
  if ((month === 12 && day >= 22) || (month === 1 && day <= 19)) {
    return '염소자리';
  } else if ((month === 1 && day >= 20) || (month === 2 && day <= 18)) {
    return '물병자리';
  } else if ((month === 2 && day >= 19) || (month === 3 && day <= 20)) {
    return '물고기자리';
  } else if ((month === 3 && day >= 21) || (month === 4 && day <= 19)) {
    return '양자리';
  } else if ((month === 4 && day >= 20) || (month === 5 && day <= 20)) {
    return '황소자리';
  } else if ((month === 5 && day >= 21) || (month === 6 && day <= 21)) {
    return '쌍둥이자리';
  } else if ((month === 6 && day >= 22) || (month === 7 && day <= 22)) {
    return '게자리';
  } else if ((month === 7 && day >= 23) || (month === 8 && day <= 22)) {
    return '사자자리';
  } else if ((month === 8 && day >= 23) || (month === 9 && day <= 22)) {
    return '처녀자리';
  } else if ((month === 9 && day >= 23) || (month === 10 && day <= 22)) {
    return '천칭자리';
  } else if ((month === 10 && day >= 23) || (month === 11 && day <= 21)) {
    return '전갈자리';
  } else { // (month === 11 && day >= 22) || (month === 12 && day <= 21)
    return '사수자리';
  }
};

export const DeepGeneratePage: React.FC = () => {
  // const navigate = useNavigate(); // 탭 기반으로 변경되어 제거
  const [loading, setLoading] = useState(false);
  const [generatedSets, setGeneratedSets] = useState<GeneratedSetDto[]>([]);
  const [mission, setMission] = useState<MissionResponse | null>(null);
  const [birthDate, setBirthDate] = useState<Dayjs | null>(null);
  const [zodiacSign, setZodiacSign] = useState<string | null>(null);
  // 운명의 번호 추천은 고정 5개 (변경 불가)
  const count = 5;
  // 윈도우 사이즈는 고정값 사용 (선택 불가)
  const windowSize = 50;
  // 마지막 생성 요청 시간 추적 (3초 딜레이)
  const lastGenerateTimeRef = useRef<number>(0);
  // 버튼 비활성화 및 카운트다운 상태
  const [buttonCooldown, setButtonCooldown] = useState<number>(0);
  
  // a 문구와 b 문구 목록
  const [partAList, setPartAList] = useState<string[]>(DEFAULT_PART_A_LIST);
  const [partBList, setPartBList] = useState<string[]>(DEFAULT_PART_B_LIST);
  // 일련번호는 로컬 스토리지에서 직접 비교하므로 상태로 관리하지 않음
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [_serialNumber, setSerialNumber] = useState<string>('');
  
  // 볼 크기 상태 (localStorage에서 불러오기)
  const [ballSize, setBallSize] = useState<BallSize>(() => {
    const saved = localStorage.getItem(STORAGE_KEY_BALL_SIZE);
    return (saved === 'small' || saved === 'medium' || saved === 'large') ? saved : 'medium';
  });

  // 마지막 선택한 별자리 불러오기 및 경고 메시지 로드
  useEffect(() => {
    const lastZodiac = localStorage.getItem(STORAGE_KEY_LAST_ZODIAC);
    if (lastZodiac && ZODIAC_SIGNS.includes(lastZodiac as any)) {
      setZodiacSign(lastZodiac);
    }
    
    // 경고 메시지 로드
    const loadDestinyLimitMessages = async () => {
      try {
        const response = await adminService.getDestinyLimitMessages();
        
        // 로컬 스토리지에서 기존 데이터 확인
        const storedData = loadDestinyMessagesFromStorage();
        
        if (response.success && response.partAList && response.partBList && response.serialNumber) {
          // API에서 a/b 문구와 일련번호를 받은 경우
          const apiSerialNumber = response.serialNumber;
          
          // 일련번호가 다르면 갱신
          if (!storedData || storedData.serialNumber !== apiSerialNumber) {
            setPartAList(response.partAList);
            setPartBList(response.partBList);
            setSerialNumber(apiSerialNumber);
            saveDestinyMessagesToStorage(response.partAList, response.partBList, apiSerialNumber);
          } else {
            // 일련번호가 같으면 로컬 스토리지 데이터 사용
            if (storedData) {
              setPartAList(storedData.partAList);
              setPartBList(storedData.partBList);
              setSerialNumber(storedData.serialNumber);
            }
          }
        } else if (response.success && response.data && response.data.length > 0) {
          // 기존 형식 (message 필드만 있는 경우) - 호환성 유지
          const messages = response.data.map(msg => msg.message);
          // 기존 메시지를 a/b로 분리 시도 (예: "별들의 계시가 모두 전달되었습니다. 내일 다시 당신의 운명을 확인하세요.")
          const extractedPartA: string[] = [];
          const extractedPartB: string[] = [];
          
          messages.forEach(msg => {
            // 마침표나 구분자로 분리 시도
            const parts = msg.split(/[.。]\s*/).filter(p => p.trim().length > 0);
            if (parts.length >= 2) {
              const a = parts[0].trim();
              const b = parts.slice(1).join('. ').trim();
              if (a && !extractedPartA.includes(a)) {
                extractedPartA.push(a);
              }
              if (b && !extractedPartB.includes(b)) {
                extractedPartB.push(b);
              }
            }
          });
          
          if (extractedPartA.length > 0 && extractedPartB.length > 0) {
            const newSerialNumber = String(Date.now()); // 임시 일련번호
            setPartAList(extractedPartA);
            setPartBList(extractedPartB);
            setSerialNumber(newSerialNumber);
            saveDestinyMessagesToStorage(extractedPartA, extractedPartB, newSerialNumber);
          }
        } else if (storedData) {
          // API 실패 시 로컬 스토리지 데이터 사용
          setPartAList(storedData.partAList);
          setPartBList(storedData.partBList);
          setSerialNumber(storedData.serialNumber);
        }
      } catch (error) {
        console.error('경고 메시지 로드 실패:', error);
        // 로컬 스토리지에서 로드 시도
        const storedData = loadDestinyMessagesFromStorage();
        if (storedData) {
          setPartAList(storedData.partAList);
          setPartBList(storedData.partBList);
          setSerialNumber(storedData.serialNumber);
        }
        // 기본값은 이미 useState 초기값으로 설정됨
      }
    };
    
    loadDestinyLimitMessages();
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

  // 생년월일 변경 시 별자리 자동 계산
  const handleBirthDateChange = (date: Dayjs | null) => {
    setBirthDate(date);
    if (date) {
      const calculatedZodiac = calculateZodiacFromDate(date);
      if (calculatedZodiac) {
        setZodiacSign(calculatedZodiac);
      }
    }
  };

  // 별자리 직접 선택
  const handleZodiacChange = (value: string) => {
    setZodiacSign(value);
    // 별자리 선택 시 생년월일은 유지 (초기화하지 않음)
  };

  // 볼 크기 변경 핸들러
  const handleBallSizeChange = (e: any) => {
    const newSize = e.target.value as BallSize;
    setBallSize(newSize);
    localStorage.setItem(STORAGE_KEY_BALL_SIZE, newSize);
  };

  const handleDeepGenerate = async () => {
    // 별자리만 있어도 가능 (생년월일 필수 아님)
    if (!zodiacSign) {
      message.warning('별자리를 선택해주세요.');
      return;
    }

    // 오늘 사용 가능 여부 확인 (3초 딜레이 체크보다 먼저)
    if (!canUseToday()) {
      const warningMsg = generateRandomMessage(partAList, partBList);
      message.warning(warningMsg);
      // 3초 딜레이 적용
      const now = Date.now();
      const timeSinceLastGenerate = now - lastGenerateTimeRef.current;
      const DELAY_MS = 3000; // 3초
      if (timeSinceLastGenerate < DELAY_MS) {
        const remainingSeconds = (DELAY_MS - timeSinceLastGenerate) / 1000;
        setButtonCooldown(remainingSeconds);
      } else {
        setButtonCooldown(3);
        lastGenerateTimeRef.current = now;
      }
      return;
    }

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
      // 1. 번호 생성
      const generateResponse = await generateService.generate({
        strategy: 'BALANCED',
        count,
        windowSize,
      });
      setGeneratedSets(generateResponse.generatedSets);
      
      // 마지막 선택한 별자리 저장
      if (zodiacSign) {
        localStorage.setItem(STORAGE_KEY_LAST_ZODIAC, zodiacSign);
      }
      // 오늘 사용 기록 저장
      localStorage.setItem(STORAGE_KEY_DESTINY_LAST_USED, getTodayString());

      // 2. Explain Tags 추출
      const explainTags = generateResponse.generatedSets[0]?.tags || [];

      // 3. 미션 생성 (별자리 정보 포함)
      // explainTags가 비어있어도 미션 생성 시도
      // 생년월일이 있으면 전달, 없으면 별자리만 사용
      try {
        // 별자리를 생년월일로 변환하여 전달 (서버에서 별자리 계산)
        // 또는 별자리 정보를 직접 전달할 수 있도록 API 수정 필요
        // 현재는 생년월일이 있으면 생년월일을, 없으면 임의의 날짜를 생성하여 별자리 계산
        let missionBirthDate: string | undefined = undefined;
        
        if (birthDate) {
          missionBirthDate = birthDate.format('YYYY-MM-DD');
        } else {
          // 별자리만 선택한 경우, 해당 별자리에 맞는 임의의 날짜 생성
          // 서버에서 별자리만으로도 처리할 수 있도록 별자리 정보를 전달하는 것이 좋지만
          // 현재 API는 생년월일만 받으므로, 별자리에 맞는 대표 날짜 생성
          missionBirthDate = getRepresentativeDateForZodiac(zodiacSign);
        }

        const firstSetNumbers = generateResponse.generatedSets[0]?.numbers;
        if (!firstSetNumbers || firstSetNumbers.length !== 6) {
          throw new Error('생성된 번호가 없거나 6개가 아닙니다.');
        }
        const missionResponse = await missionService.createMission({
          strategy: 'BALANCED',
          numbers: firstSetNumbers,
          explainTags: explainTags.length > 0 ? explainTags : undefined,
          tone: 'LIGHT',
          birthDate: missionBirthDate,
        });
        if (missionResponse && missionResponse.missionText) {
          setMission(missionResponse);
        } else {
          console.warn('미션 응답이 비어있습니다.');
          setMission(null);
        }
      } catch (missionError: any) {
        console.error('미션 생성 오류:', missionError);
        // 미션 생성 실패해도 번호는 표시
        let errorMsg = missionError?.message || '미션 생성에 실패했습니다.';
        if (missionError?.details?.message) {
          errorMsg = missionError.details.message;
        }
        if (missionError?.status === 500) {
          errorMsg = '서버에서 미션을 생성하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
        } else if (missionError?.status === 400) {
          errorMsg = '잘못된 요청입니다. 생년월일 또는 별자리를 확인해주세요.';
        }
        message.warning(`미션 생성 실패: ${errorMsg}. 번호는 정상적으로 생성되었습니다.`);
        setMission(null);
      }

      message.success('운명의 번호 생성 완료!');
    } catch (error: any) {
      console.error('번호 생성 오류:', error);
      message.error(error?.message || '번호 생성에 실패했습니다.');
      setGeneratedSets([]);
      setMission(null);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 별자리에 맞는 대표 날짜 생성 (별자리만 선택한 경우)
   */
  const getRepresentativeDateForZodiac = (zodiac: string): string => {
    const currentYear = new Date().getFullYear();
    const zodiacDateMap: Record<string, string> = {
      '염소자리': `${currentYear}-12-25`,
      '물병자리': `${currentYear}-02-01`,
      '물고기자리': `${currentYear}-03-10`,
      '양자리': `${currentYear}-04-01`,
      '황소자리': `${currentYear}-05-01`,
      '쌍둥이자리': `${currentYear}-06-01`,
      '게자리': `${currentYear}-07-01`,
      '사자자리': `${currentYear}-08-01`,
      '처녀자리': `${currentYear}-09-01`,
      '천칭자리': `${currentYear}-10-01`,
      '전갈자리': `${currentYear}-11-01`,
      '사수자리': `${currentYear}-12-01`,
    };
    return zodiacDateMap[zodiac] || `${currentYear}-01-01`;
  };

  return (
    <div>
      <Card
        title="운명의 번호 추천"
      >
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <Space wrap>
            <Space>
              <span>생년월일:</span>
              <DatePicker
                value={birthDate}
                onChange={handleBirthDateChange}
                format="YYYY-MM-DD"
                placeholder="생년월일 선택 (선택사항)"
                style={{ width: 200 }}
                allowClear
              />
            </Space>
            <Space>
              <span>별자리:</span>
              <Select
                value={zodiacSign}
                onChange={handleZodiacChange}
                placeholder="별자리 선택"
                style={{ width: 150 }}
                allowClear
              >
                {ZODIAC_SIGNS.map((sign) => (
                  <Option key={sign} value={sign}>
                    {sign}
                  </Option>
                ))}
              </Select>
            </Space>
          </Space>

          <Space>
            <span>생성 개수:</span>
            <InputNumber
              value={count}
              disabled
              style={{ width: 80 }}
            />
            <span style={{ color: '#666', fontSize: '12px' }}>
              (고정값, 변경 불가)
            </span>
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
            onClick={handleDeepGenerate}
            loading={buttonCooldown > 0 || loading}
            disabled={buttonCooldown > 0 || loading}
          >
            운명의 번호 받기
          </Button>
        </Space>
      </Card>

      {loading && (
        <div style={{ textAlign: 'center', marginTop: '24px' }}>
          <Spin size="large" />
        </div>
      )}

      {!loading && mission && (
        <div style={{ marginTop: '24px' }}>
          <Card
            title={
              <Space>
                <span>미션</span>
                {mission.zodiacSign && (
                  <span style={{ color: '#1890ff' }}>
                    ({mission.zodiacSign})
                  </span>
                )}
              </Space>
            }
          >
            <Paragraph style={{ fontSize: '16px', lineHeight: '1.8' }}>
              {mission.missionText}
            </Paragraph>
          </Card>
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
    </div>
  );
};
