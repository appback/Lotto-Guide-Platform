/**
 * API 관련 타입 정의
 */

export interface GenerateRequest {
  strategy?: 'FREQUENT_TOP' | 'OVERDUE_TOP' | 'BALANCED' | 'WHEELING_SYSTEM' | 'WEIGHTED_RANDOM' | 'PATTERN_MATCHER' | 'AI_SIMULATION' | 'AI_PATTERN_REASONER' | 'AI_DECISION_FILTER' | 'AI_WEIGHT_EVOLUTION';
  constraints?: {
    includeNumbers?: number[];
    excludeNumbers?: number[];
    oddEvenRatioRange?: {
      min: number;
      max: number;
    };
    sumRange?: {
      min: number;
      max: number;
    };
    similarityThreshold?: number;
  };
  count?: number;
  windowSize?: number;
}

export interface GeneratedSetDto {
  index: number;
  numbers: number[];
  tags: string[];
}

export interface GenerateResponse {
  generatedSets: GeneratedSetDto[];
  setId?: number;
}

export interface MissionRequest {
  strategy?: string;
  numbers?: number[]; // 6개 번호 (서버 필수)
  explainTags?: string[];
  tone?: 'LIGHT';
  birthDate?: string; // YYYY-MM-DD 형식
}

export interface MissionResponse {
  missionText: string;
  tokenUsage?: number;
  costEstimate?: number;
  zodiacSign?: string;
}

export interface HistoryItemDto {
  setId: number;
  strategy: string;
  generatedCount: number;
  createdAt: string;
  numbers: GeneratedSetDto[];
}

export interface HistoryResponse {
  items: HistoryItemDto[];
  total: number;
  page: number;
  size: number;
}

export interface ErrorResponse {
  error: string;
  message: string;
  timestamp: string;
}
