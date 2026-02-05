import { apiClient } from './api';
import { API_ENDPOINTS } from '../config/environment';
import type { MissionRequest, MissionResponse } from '@/types/api';

export const missionService = {
  /**
   * 미션 생성
   */
  createMission: async (request: MissionRequest): Promise<MissionResponse> => {
    // dadp-hub 패턴: 절대 경로 직접 사용
    const response = await apiClient.post<MissionResponse>(API_ENDPOINTS.MISSION, request);
    return response;
  },
};
