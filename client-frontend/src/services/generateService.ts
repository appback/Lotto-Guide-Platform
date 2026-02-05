import { apiClient } from './api';
import { API_ENDPOINTS } from '../config/environment';
import type { GenerateRequest, GenerateResponse } from '@/types/api';
import { historyService } from './historyService';

export interface StrategyDescription {
  title: string;
  shortDescription: string;
  description: string;
  features: string[];
  algorithm: string[];
  scenarios: string[];
  notes?: string[];
}

export interface GetStrategyDescriptionsResponse {
  success: boolean;
  message?: string;
  data?: Record<string, StrategyDescription>;
}

export const generateService = {
  /**
   * 번호 생성
   */
  generate: async (request: GenerateRequest): Promise<GenerateResponse> => {
    // dadp-hub 패턴: 절대 경로 직접 사용
    const response = await apiClient.post<GenerateResponse>(API_ENDPOINTS.GENERATE, request);
    
    // 히스토리에 저장 (로컬스토리지)
    if (response.generatedSets && response.generatedSets.length > 0) {
      const strategy = request.strategy || 'BALANCED';
      historyService.addHistory(response, strategy);
    }
    
    return response;
  },

  /**
   * 전략 설명 조회
   */
  getStrategyDescriptions: async (): Promise<GetStrategyDescriptionsResponse> => {
    const response = await apiClient.get<GetStrategyDescriptionsResponse>(
      API_ENDPOINTS.STRATEGY_DESCRIPTIONS
    );
    return response;
  },
};
