import React from 'react';
import './LottoBall.css';

interface LottoBallProps {
  number: number;
  size?: 'small' | 'medium' | 'large';
  animated?: boolean;
}

/**
 * 로또 번호를 원형 볼 스타일로 표시하는 컴포넌트
 * 
 * @param number - 표시할 로또 번호 (1-45)
 * @param size - 볼 크기 (small: 40px, medium: 50px, large: 60px)
 * @param animated - 생성 시 애니메이션 효과 적용 여부
 */
export const LottoBall: React.FC<LottoBallProps> = ({ 
  number, 
  size = 'medium',
  animated = false 
}) => {
  // 번호 범위별 색상 결정 (실제 로또와 유사한 색상)
  const getBallColor = (num: number): string => {
    if (num >= 1 && num <= 10) return 'yellow';    // 노랑
    if (num >= 11 && num <= 20) return 'blue';     // 파랑
    if (num >= 21 && num <= 30) return 'red';      // 빨강
    if (num >= 31 && num <= 40) return 'gray';     // 회색
    if (num >= 41 && num <= 45) return 'green';    // 초록
    return 'default';
  };

  const colorClass = getBallColor(number);
  const sizeClass = `lotto-ball-${size}`;
  const animationClass = animated ? 'lotto-ball-animated' : '';

  return (
    <div 
      className={`lotto-ball ${colorClass} ${sizeClass} ${animationClass}`}
      style={{ animationDelay: animated ? `${Math.random() * 0.3}s` : '0s' }}
    >
      <span className="lotto-ball-number">{number}</span>
    </div>
  );
};

interface LottoBallGroupProps {
  numbers: number[];
  size?: 'small' | 'medium' | 'large';
  animated?: boolean;
  gap?: number;
}

/**
 * 여러 로또 번호를 볼 그룹으로 표시하는 컴포넌트
 */
export const LottoBallGroup: React.FC<LottoBallGroupProps> = ({ 
  numbers, 
  size = 'medium',
  animated = false,
  gap
}) => {
  // gap이 명시되지 않았을 때 size에 따라 기본값 설정
  // 작게일 때는 간격을 더 작게 (5px), 나머지는 기본값 (12px)
  const defaultGap = gap !== undefined ? gap : (size === 'small' ? 5 : 12);
  
  return (
    <div 
      className="lotto-ball-group" 
      style={{ gap: `${defaultGap}px` }}
    >
      {numbers.map((num, index) => (
        <LottoBall 
          key={`${num}-${index}`}
          number={num} 
          size={size}
          animated={animated}
        />
      ))}
    </div>
  );
};
