// 환경변수 기반 설정 (dadp-hub 패턴 적용)
interface EnvironmentConfig {
  API_BASE_URL: string;
  CONTEXT_PATH: string;
}

const getEnvironmentConfig = (): EnvironmentConfig => {
  // 현재 호스트 기반 환경 감지
  const currentHost = window.location.hostname;
  const currentPort = window.location.port;
  
  // AWS 환경 감지
  const isAwsEnvironment = currentHost.includes('ec2') || 
                          currentHost.includes('amazonaws.com') || 
                          /^\d+\.\d+\.\d+\.\d+$/.test(currentHost);
  
  const isLocalEnvironment = currentHost.includes('localhost') || currentHost.includes('127.0.0.1');
  
  // 환경별 기본 URL 설정
  let apiBaseUrl = '';
  
  // 환경별 설정
  if (isAwsEnvironment) {
    // AWS 환경: 상대 경로 사용
    apiBaseUrl = '';
  } else {
    // 로컬 개발 환경: 절대 URL 사용 (dadp-hub 패턴)
    apiBaseUrl = 'http://localhost:8083';
  }
  
  const contextPath = '/lotto';
  
  const config = {
    API_BASE_URL: apiBaseUrl,
    CONTEXT_PATH: contextPath,
  };
  
  // 환경 정보 로깅
  console.log('🔍 [environment.ts] 환경 설정:', {
    currentHost,
    currentPort,
    isAwsEnvironment,
    isLocalEnvironment,
    apiBaseUrl,
    contextPath
  });
  
  return config;
};

export const config = getEnvironmentConfig();

// API 엔드포인트 - Context Path 포함
export const API_ENDPOINTS = {
  API_BASE_URL: config.API_BASE_URL,
  
  GENERATE: `${config.API_BASE_URL}${config.CONTEXT_PATH}/api/v1/generate`,
  MISSION: `${config.API_BASE_URL}${config.CONTEXT_PATH}/api/v1/mission`,
  HISTORY: `${config.API_BASE_URL}${config.CONTEXT_PATH}/api/v1/history`,
  STRATEGY_DESCRIPTIONS: `${config.API_BASE_URL}${config.CONTEXT_PATH}/api/v1/generate/strategy-descriptions`,
} as const;
