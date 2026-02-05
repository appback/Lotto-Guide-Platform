import { config } from '@/config/environment';

const API_BASE = `${config.API_BASE_URL}${config.CONTEXT_PATH}/api/v1/admin`;

export interface CollectRangeResponse {
  success: boolean;
  message: string;
  from: number;
  to: number;
  successCount: number; // 성공한 회차 수
  skip: number;
  fail: number;
  total: number;
}

export interface ImportCsvResponse {
  success: boolean;
  message: string;
  savedCount: number;
  skippedCount: number;
  errorCount: number;
  errors?: string[];
}

export interface DrawData {
  drawNo: number;
  drawDate: string;
  numbers: number[];
  bonus: number;
  totalPrize?: number;
  winnerCount?: number;
  prizePerPerson?: number;
  createdAt?: string;
}

export interface SaveDrawRequest {
  drawNo: number;
  drawDate: string;
  numbers: number[];
  bonus: number;
  totalPrize?: number;
  winnerCount?: number;
  prizePerPerson?: number;
}

export interface SaveDrawResponse {
  success: boolean;
  message: string;
  drawNo: number;
  isUpdate?: boolean;
}

export interface GetDrawResponse {
  success: boolean;
  message?: string;
  data?: DrawData;
}

export interface GetDrawsResponse {
  success: boolean;
  message?: string;
  data?: DrawData[];
  total?: number;
  page?: number;
  size?: number;
  totalPages?: number;
}

export interface RefreshDataResponse {
  success: boolean;
  message: string;
  savedCount: number;
  failedCount: number;
  latestDrawNo: number;
}

export const adminService = {
  /**
   * CSV 파일 업로드
   */
  importCsv: async (file: File, includeHeader: boolean = true, delimiter: string = ','): Promise<ImportCsvResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('includeHeader', String(includeHeader));
    formData.append('delimiter', delimiter);

    const url = `${API_BASE}/import-csv`;
    console.log('📤 [adminService] CSV 업로드 시작:', { url, fileName: file.name, fileSize: file.size, includeHeader, delimiter });

    // FormData 사용 시 Content-Type 헤더를 명시하지 않음 (브라우저가 자동으로 multipart/form-data 설정)
    const response = await fetch(url, {
      method: 'POST',
      body: formData,
      credentials: 'same-origin',
      // Content-Type 헤더를 명시하지 않음 - 브라우저가 자동으로 boundary 포함하여 설정
    });

    console.log('📥 [adminService] CSV 업로드 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] CSV 업로드 실패:', errorData);
      } catch (e) {
        // JSON 파싱 실패 시 기본 메시지 사용
        const text = await response.text().catch(() => '');
        console.error('❌ [adminService] CSV 업로드 실패 (JSON 파싱 불가):', text);
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] CSV 업로드 성공:', result);
    return result;
  },

  /**
   * CSV 파일 다운로드
   */
  exportCsv: async (): Promise<Blob> => {
    const url = `${API_BASE}/export-csv`;
    console.log('📤 [adminService] CSV 다운로드 시작:', url);

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] CSV 다운로드 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] CSV 다운로드 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] CSV 다운로드 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const blob = await response.blob();
    console.log('✅ [adminService] CSV 다운로드 성공:', { size: blob.size, type: blob.type });
    return blob;
  },

  /**
   * 범위 수집
   */
  collectRange: async (from: number, to: number): Promise<CollectRangeResponse> => {
    const url = `${API_BASE}/collect-range?from=${from}&to=${to}`;
    console.log('📤 [adminService] 범위 수집 시작:', { url, from, to });

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 범위 수집 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 범위 수집 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 범위 수집 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 범위 수집 성공:', result);
    return result;
  },

  /**
   * 수동 저장/업데이트 (JSON 데이터 직접 전달)
   */
  saveDraw: async (request: SaveDrawRequest): Promise<SaveDrawResponse> => {
    const url = `${API_BASE}/save-draw`;
    console.log('📤 [adminService] 수동 저장 시작:', { url, drawNo: request.drawNo });

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 수동 저장 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 수동 저장 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 수동 저장 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 수동 저장 성공:', result);
    return result;
  },

  /**
   * 모든 회차 정보 가져오기 (DB 최신 회차부터 API 최신 회차까지)
   * @param forceUpdate true일 경우 모든 회차를 강제 업데이트, false일 경우 누락된 회차만 수집
   */
  refreshData: async (forceUpdate: boolean = false): Promise<RefreshDataResponse> => {
    const url = `${API_BASE}/refresh-data?forceUpdate=${forceUpdate}`;
    console.log('📤 [adminService] 모든 회차 정보 가져오기 시작:', { url, forceUpdate });

    const response = await fetch(url, {
      method: 'POST',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 모든 회차 정보 가져오기 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 모든 회차 정보 가져오기 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 모든 회차 정보 가져오기 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 모든 회차 정보 가져오기 성공:', result);
    return result;
  },

  /**
   * 수집 중단 요청
   */
  cancelRefreshData: async (): Promise<{ success: boolean; message: string }> => {
    const url = `${API_BASE}/refresh-data/cancel`;
    console.log('📤 [adminService] 수집 중단 요청 시작:', { url });

    const response = await fetch(url, {
      method: 'POST',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 수집 중단 요청 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 수집 중단 요청 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 수집 중단 요청 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 수집 중단 요청 성공:', result);
    return result;
  },

  /**
   * 공공데이터 API에서 특정 회차 조회 및 저장
   */
  fetchAndSaveDraw: async (drawNo: number): Promise<FetchAndSaveDrawResponse> => {
    const url = `${API_BASE}/fetch-and-save-draw?drawNo=${drawNo}`;
    console.log('📤 [adminService] 공공데이터 API에서 회차 조회 및 저장 시작:', { url, drawNo });

    const response = await fetch(url, {
      method: 'POST',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 공공데이터 API에서 회차 조회 및 저장 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 공공데이터 API에서 회차 조회 및 저장 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 공공데이터 API에서 회차 조회 및 저장 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 공공데이터 API에서 회차 조회 및 저장 성공:', result);
    return result;
  },

  /**
   * 단일 회차 조회
   */
  getDraw: async (drawNo: number): Promise<GetDrawResponse> => {
    const url = `${API_BASE}/draw/${drawNo}`;
    console.log('📤 [adminService] 회차 조회 시작:', { url, drawNo });

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 회차 조회 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 회차 조회 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 회차 조회 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 회차 조회 성공:', result);
    return result;
  },

  /**
   * 전체 회차 목록 조회
   */
  getDraws: async (page: number = 0, size: number = 100): Promise<GetDrawsResponse> => {
    const url = `${API_BASE}/draws?page=${page}&size=${size}`;
    console.log('📤 [adminService] 전체 회차 목록 조회 시작:', { url, page, size });

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 전체 회차 목록 조회 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 전체 회차 목록 조회 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 전체 회차 목록 조회 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 전체 회차 목록 조회 성공:', result);
    return result;
  },

  /**
   * 전략 설명 조회
   */
  getStrategyDescriptions: async (): Promise<GetStrategyDescriptionsResponse> => {
    const url = `${API_BASE}/strategy-descriptions`;
    console.log('📤 [adminService] 전략 설명 조회 시작:', url);

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 전략 설명 조회 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 전략 설명 조회 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 전략 설명 조회 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 전략 설명 조회 성공:', result);
    return result;
  },

  /**
   * 전략 설명 수정
   */
  updateStrategyDescription: async (
    strategyCode: string,
    request: UpdateStrategyDescriptionRequest
  ): Promise<UpdateStrategyDescriptionResponse> => {
    const url = `${API_BASE}/strategy-descriptions/${strategyCode}`;
    console.log('📤 [adminService] 전략 설명 수정 시작:', { url, strategyCode });

    const response = await fetch(url, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 전략 설명 수정 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 전략 설명 수정 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 전략 설명 수정 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 전략 설명 수정 성공:', result);
    return result;
  },

  /**
   * 전략 설명 CSV 다운로드
   */
  exportStrategyDescriptionsCsv: async (): Promise<Blob> => {
    const url = `${API_BASE}/strategy-descriptions/export-csv`;
    console.log('📤 [adminService] 전략 설명 CSV 다운로드 시작:', url);

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 전략 설명 CSV 다운로드 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 전략 설명 CSV 다운로드 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 전략 설명 CSV 다운로드 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const blob = await response.blob();
    console.log('✅ [adminService] 전략 설명 CSV 다운로드 성공:', { size: blob.size, type: blob.type });
    return blob;
  },

  /**
   * 전략 설명 CSV 업로드
   */
  importStrategyDescriptionsCsv: async (
    file: File,
    includeHeader: boolean = true,
    delimiter: string = ','
  ): Promise<ImportStrategyDescriptionsCsvResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('includeHeader', String(includeHeader));
    formData.append('delimiter', delimiter);

    const url = `${API_BASE}/strategy-descriptions/import-csv`;
    console.log('📤 [adminService] 전략 설명 CSV 업로드 시작:', { url, fileName: file.name, fileSize: file.size, includeHeader, delimiter });

    const response = await fetch(url, {
      method: 'POST',
      body: formData,
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 전략 설명 CSV 업로드 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 전략 설명 CSV 업로드 실패:', errorData);
      } catch (e) {
        const text = await response.text().catch(() => '');
        console.error('❌ [adminService] 전략 설명 CSV 업로드 실패 (JSON 파싱 불가):', text);
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 전략 설명 CSV 업로드 성공:', result);
    return result;
  },

  /**
   * 운명의 번호 추천 경고 메시지 조회
   */
  getDestinyLimitMessages: async (): Promise<GetDestinyLimitMessagesResponse> => {
    const url = `${API_BASE}/destiny-limit-messages`;
    console.log('📤 [adminService] 경고 메시지 조회 시작:', url);

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 경고 메시지 조회 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 경고 메시지 조회 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 경고 메시지 조회 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 경고 메시지 조회 성공:', result);
    return result;
  },

  /**
   * 운명의 번호 추천 경고 메시지 CSV 다운로드
   */
  exportDestinyLimitMessagesCsv: async (): Promise<Blob> => {
    const url = `${API_BASE}/destiny-limit-messages/export-csv`;
    console.log('📤 [adminService] 경고 메시지 CSV 다운로드 시작:', url);

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 경고 메시지 CSV 다운로드 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 경고 메시지 CSV 다운로드 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 경고 메시지 CSV 다운로드 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const blob = await response.blob();
    console.log('✅ [adminService] 경고 메시지 CSV 다운로드 성공:', { size: blob.size, type: blob.type });
    return blob;
  },

  /**
   * 운명의 번호 추천 경고 메시지 CSV 업로드
   */
  importDestinyLimitMessagesCsv: async (file: File): Promise<ImportDestinyLimitMessagesCsvResponse> => {
    const formData = new FormData();
    formData.append('file', file);

    const url = `${API_BASE}/destiny-limit-messages/import-csv`;
    console.log('📤 [adminService] 경고 메시지 CSV 업로드 시작:', { url, fileName: file.name, fileSize: file.size });

    const response = await fetch(url, {
      method: 'POST',
      body: formData,
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 경고 메시지 CSV 업로드 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 경고 메시지 CSV 업로드 실패:', errorData);
      } catch (e) {
        const text = await response.text().catch(() => '');
        console.error('❌ [adminService] 경고 메시지 CSV 업로드 실패 (JSON 파싱 불가):', text);
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 경고 메시지 CSV 업로드 성공:', result);
    return result;
  },

  /**
   * 운명의 번호 추천 미션 템플릿 조회
   */
  getMissionTemplates: async (page: number = 0, size: number = 100): Promise<GetMissionTemplatesResponse> => {
    const url = `${API_BASE}/mission-templates?page=${page}&size=${size}`;
    console.log('📤 [adminService] 미션 템플릿 조회 시작:', { url, page, size });

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 미션 템플릿 조회 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 미션 템플릿 조회 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 미션 템플릿 조회 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 미션 템플릿 조회 성공:', result);
    return result;
  },

  /**
   * 운명의 번호 추천 미션 템플릿 CSV 다운로드
   */
  exportMissionTemplatesCsv: async (): Promise<Blob> => {
    const url = `${API_BASE}/mission-templates/export-csv`;
    console.log('📤 [adminService] 미션 템플릿 CSV 다운로드 시작:', url);

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 미션 템플릿 CSV 다운로드 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 미션 템플릿 CSV 다운로드 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 미션 템플릿 CSV 다운로드 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const blob = await response.blob();
    console.log('✅ [adminService] 미션 템플릿 CSV 다운로드 성공:', { size: blob.size, type: blob.type });
    return blob;
  },

  /**
   * 운명의 번호 추천 미션 템플릿 CSV 업로드
   */
  importMissionTemplatesCsv: async (file: File): Promise<ImportMissionTemplatesCsvResponse> => {
    const formData = new FormData();
    formData.append('file', file);

    const url = `${API_BASE}/mission-templates/import-csv`;
    console.log('📤 [adminService] 미션 템플릿 CSV 업로드 시작:', { url, fileName: file.name, fileSize: file.size });

    const response = await fetch(url, {
      method: 'POST',
      body: formData,
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 미션 템플릿 CSV 업로드 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 미션 템플릿 CSV 업로드 실패:', errorData);
      } catch (e) {
        const text = await response.text().catch(() => '');
        console.error('❌ [adminService] 미션 템플릿 CSV 업로드 실패 (JSON 파싱 불가):', text);
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 미션 템플릿 CSV 업로드 성공:', result);
    return result;
  },

  /**
   * AI 로딩 메시지 조회
   */
  getAiLoadingMessages: async (): Promise<GetAiLoadingMessagesResponse> => {
    const url = `${API_BASE}/ai-loading-messages`;
    console.log('📤 [adminService] AI 로딩 메시지 조회 시작:', url);

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] AI 로딩 메시지 조회 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] AI 로딩 메시지 조회 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] AI 로딩 메시지 조회 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] AI 로딩 메시지 조회 성공:', result);
    return result;
  },

  /**
   * 시스템 옵션 조회
   */
  getSystemOptions: async (): Promise<GetSystemOptionsResponse> => {
    const url = `${API_BASE}/system-options`;
    console.log('📤 [adminService] 시스템 옵션 조회 시작:', url);

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 시스템 옵션 조회 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 시스템 옵션 조회 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 시스템 옵션 조회 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 시스템 옵션 조회 성공:', result);
    return result;
  },

  /**
   * 시스템 옵션 조회 (키별)
   */
  getSystemOption: async (key: string): Promise<GetSystemOptionResponse> => {
    const url = `${API_BASE}/system-options/${key}`;
    console.log('📤 [adminService] 시스템 옵션 조회 시작:', url);

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 시스템 옵션 조회 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 시스템 옵션 조회 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 시스템 옵션 조회 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 시스템 옵션 조회 성공:', result);
    return result;
  },

  /**
   * 시스템 옵션 수정
   */
  updateSystemOption: async (key: string, value: string, description?: string): Promise<UpdateSystemOptionResponse> => {
    const url = `${API_BASE}/system-options/${key}`;
    console.log('📤 [adminService] 시스템 옵션 수정 시작:', { url, key, value });

    const response = await fetch(url, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ value, description }),
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 시스템 옵션 수정 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 시스템 옵션 수정 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 시스템 옵션 수정 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] 시스템 옵션 수정 성공:', result);
    return result;
  },

  /**
   * 공개 시스템 옵션 조회 (인증 불필요, 일련번호 포함)
   */
  getPublicSystemOptions: async (): Promise<GetPublicSystemOptionsResponse> => {
    const { config } = await import('@/config/environment');
    const PUBLIC_API_BASE = `${config.API_BASE_URL}${config.CONTEXT_PATH}/api/v1/generate`;
    const url = `${PUBLIC_API_BASE}/system-options`;
    console.log('📤 [adminService] 공개 시스템 옵션 조회 시작:', url);

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] 공개 시스템 옵션 조회 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] 공개 시스템 옵션 조회 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] 공개 시스템 옵션 조회 실패 (JSON 파싱 불가)');
      }
      // 공개 API이므로 실패해도 기본값 사용
      return { success: false, data: {}, serialNumber: null };
    }

    const result = await response.json();
    console.log('✅ [adminService] 공개 시스템 옵션 조회 성공:', result);
    return result;
  },

  // ==================== A/B/C 멘트 관리 API ====================

  /**
   * A 멘트 목록 조회
   */
  getMissionPhraseA: async (page: number = 0, size: number = 100): Promise<GetMissionPhraseAResponse> => {
    const url = `${API_BASE}/mission-phrase-a?page=${page}&size=${size}`;
    console.log('📤 [adminService] A 멘트 조회 시작:', { url, page, size });

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] A 멘트 조회 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] A 멘트 조회 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] A 멘트 조회 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] A 멘트 조회 성공:', result);
    return result;
  },

  /**
   * A 멘트 CSV 다운로드
   */
  exportMissionPhraseACsv: async (): Promise<Blob> => {
    const url = `${API_BASE}/mission-phrase-a/export-csv`;
    console.log('📤 [adminService] A 멘트 CSV 다운로드 시작:', url);

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] A 멘트 CSV 다운로드 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] A 멘트 CSV 다운로드 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] A 멘트 CSV 다운로드 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const blob = await response.blob();
    console.log('✅ [adminService] A 멘트 CSV 다운로드 성공:', { size: blob.size, type: blob.type });
    return blob;
  },

  /**
   * A 멘트 CSV 업로드
   */
  importMissionPhraseACsv: async (file: File): Promise<ImportMissionPhraseCsvResponse> => {
    const formData = new FormData();
    formData.append('file', file);

    const url = `${API_BASE}/mission-phrase-a/import-csv`;
    console.log('📤 [adminService] A 멘트 CSV 업로드 시작:', { url, fileName: file.name, fileSize: file.size });

    const response = await fetch(url, {
      method: 'POST',
      body: formData,
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] A 멘트 CSV 업로드 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] A 멘트 CSV 업로드 실패:', errorData);
      } catch (e) {
        const text = await response.text().catch(() => '');
        console.error('❌ [adminService] A 멘트 CSV 업로드 실패 (JSON 파싱 불가):', text);
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] A 멘트 CSV 업로드 성공:', result);
    return result;
  },

  /**
   * B 멘트 목록 조회
   */
  getMissionPhraseB: async (page: number = 0, size: number = 100): Promise<GetMissionPhraseBResponse> => {
    const url = `${API_BASE}/mission-phrase-b?page=${page}&size=${size}`;
    console.log('📤 [adminService] B 멘트 조회 시작:', { url, page, size });

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] B 멘트 조회 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] B 멘트 조회 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] B 멘트 조회 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] B 멘트 조회 성공:', result);
    return result;
  },

  /**
   * B 멘트 CSV 다운로드
   */
  exportMissionPhraseBCsv: async (): Promise<Blob> => {
    const url = `${API_BASE}/mission-phrase-b/export-csv`;
    console.log('📤 [adminService] B 멘트 CSV 다운로드 시작:', url);

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] B 멘트 CSV 다운로드 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] B 멘트 CSV 다운로드 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] B 멘트 CSV 다운로드 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const blob = await response.blob();
    console.log('✅ [adminService] B 멘트 CSV 다운로드 성공:', { size: blob.size, type: blob.type });
    return blob;
  },

  /**
   * B 멘트 CSV 업로드
   */
  importMissionPhraseBCsv: async (file: File): Promise<ImportMissionPhraseCsvResponse> => {
    const formData = new FormData();
    formData.append('file', file);

    const url = `${API_BASE}/mission-phrase-b/import-csv`;
    console.log('📤 [adminService] B 멘트 CSV 업로드 시작:', { url, fileName: file.name, fileSize: file.size });

    const response = await fetch(url, {
      method: 'POST',
      body: formData,
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] B 멘트 CSV 업로드 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] B 멘트 CSV 업로드 실패:', errorData);
      } catch (e) {
        const text = await response.text().catch(() => '');
        console.error('❌ [adminService] B 멘트 CSV 업로드 실패 (JSON 파싱 불가):', text);
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] B 멘트 CSV 업로드 성공:', result);
    return result;
  },

  /**
   * C 멘트 목록 조회
   */
  getMissionPhraseC: async (page: number = 0, size: number = 100): Promise<GetMissionPhraseCResponse> => {
    const url = `${API_BASE}/mission-phrase-c?page=${page}&size=${size}`;
    console.log('📤 [adminService] C 멘트 조회 시작:', { url, page, size });

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] C 멘트 조회 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] C 멘트 조회 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] C 멘트 조회 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] C 멘트 조회 성공:', result);
    return result;
  },

  /**
   * C 멘트 CSV 다운로드
   */
  exportMissionPhraseCCsv: async (): Promise<Blob> => {
    const url = `${API_BASE}/mission-phrase-c/export-csv`;
    console.log('📤 [adminService] C 멘트 CSV 다운로드 시작:', url);

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] C 멘트 CSV 다운로드 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] C 멘트 CSV 다운로드 실패:', errorData);
      } catch (e) {
        console.error('❌ [adminService] C 멘트 CSV 다운로드 실패 (JSON 파싱 불가)');
      }
      throw new Error(errorMessage);
    }

    const blob = await response.blob();
    console.log('✅ [adminService] C 멘트 CSV 다운로드 성공:', { size: blob.size, type: blob.type });
    return blob;
  },

  /**
   * C 멘트 CSV 업로드
   */
  importMissionPhraseCCsv: async (file: File): Promise<ImportMissionPhraseCsvResponse> => {
    const formData = new FormData();
    formData.append('file', file);

    const url = `${API_BASE}/mission-phrase-c/import-csv`;
    console.log('📤 [adminService] C 멘트 CSV 업로드 시작:', { url, fileName: file.name, fileSize: file.size });

    const response = await fetch(url, {
      method: 'POST',
      body: formData,
      credentials: 'same-origin',
    });

    console.log('📥 [adminService] C 멘트 CSV 업로드 응답:', { status: response.status, ok: response.ok });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} 에러`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('❌ [adminService] C 멘트 CSV 업로드 실패:', errorData);
      } catch (e) {
        const text = await response.text().catch(() => '');
        console.error('❌ [adminService] C 멘트 CSV 업로드 실패 (JSON 파싱 불가):', text);
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log('✅ [adminService] C 멘트 CSV 업로드 성공:', result);
    return result;
  },
};

export interface ImportStrategyDescriptionsCsvResponse {
  success: boolean;
  message: string;
  savedCount: number;
  updatedCount: number;
  errorCount: number;
  errors?: string[];
}

export interface StrategyDescription {
  strategyCode: string;
  title: string;
  shortDescription: string;
  description: string;
  features: string[];
  algorithm: string[];
  scenarios: string[];
  notes?: string[];
  contentHash?: string; // 전략 설명 내용 기반 해시 일련번호
  createdAt?: string;
  updatedAt?: string;
}

export interface GetStrategyDescriptionsResponse {
  success: boolean;
  message?: string;
  data?: StrategyDescription[];
}

export interface UpdateStrategyDescriptionRequest {
  title?: string;
  shortDescription?: string;
  description?: string;
  features?: string[];
  algorithm?: string[];
  scenarios?: string[];
  notes?: string[];
}

export interface UpdateStrategyDescriptionResponse {
  success: boolean;
  message: string;
  strategyCode: string;
}

export interface DestinyLimitMessage {
  id: number;
  message: string;
  messagePartA?: string;
  messagePartB?: string;
  serialNumber?: string;
  orderIndex: number;
}

export interface GetDestinyLimitMessagesResponse {
  success: boolean;
  message?: string;
  data?: DestinyLimitMessage[];
  count?: number;
  partAList?: string[];
  partBList?: string[];
  serialNumber?: string;
}

export interface ImportDestinyLimitMessagesCsvResponse {
  success: boolean;
  message: string;
  savedCount: number;
  errorCount: number;
  errors?: string[];
}

export interface AiLoadingMessage {
  id: number;
  message: string;
  messagePartA?: string;
  messagePartB?: string;
  serialNumber?: string;
  orderIndex: number;
}

export interface GetAiLoadingMessagesResponse {
  success: boolean;
  message?: string;
  data?: AiLoadingMessage[];
  count?: number;
  partAList?: string[];
  partBList?: string[];
  serialNumber?: string;
}

export interface MissionTemplate {
  id: number;
  category: string;
  theme: string;
  tone: string;
  placeHint?: string;
  timeHint?: string;
  text: string;
  weight: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface GetMissionTemplatesResponse {
  success: boolean;
  message?: string;
  data?: MissionTemplate[];
  total?: number;
  page?: number;
  size?: number;
  totalPages?: number;
}

export interface ImportMissionTemplatesCsvResponse {
  success: boolean;
  message: string;
  savedCount: number;
  errorCount: number;
  errors?: string[];
}

export interface GetSystemOptionsResponse {
  success: boolean;
  message?: string;
  data?: Record<string, string>;
  count?: number;
  serialNumber?: string;
}

export interface GetSystemOptionResponse {
  success: boolean;
  message?: string;
  key?: string;
  value?: string;
  description?: string;
}

export interface UpdateSystemOptionResponse {
  success: boolean;
  message: string;
  key: string;
  value: string;
}

export interface GetPublicSystemOptionsResponse {
  success: boolean;
  message?: string;
  data?: Record<string, string>;
  count?: number;
  serialNumber?: string | null;
}

// A/B/C 멘트 관련 인터페이스
export interface MissionPhraseA {
  id: number;
  text: string;
  strategyTags?: string;
  comboTags?: string;
  zodiacTags?: string;
  toneTags?: string;
  weightBase: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface MissionPhraseB {
  id: number;
  text: string;
  placeHint?: string;
  colorHint?: string;
  alignTags?: string;
  avoidTags?: string;
  weightBase: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface MissionPhraseC {
  id: number;
  text: string;
  toneTags?: string;
  weightBase: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface GetMissionPhraseAResponse {
  success: boolean;
  message?: string;
  data?: MissionPhraseA[];
  total?: number;
  page?: number;
  size?: number;
  totalPages?: number;
}

export interface GetMissionPhraseBResponse {
  success: boolean;
  message?: string;
  data?: MissionPhraseB[];
  total?: number;
  page?: number;
  size?: number;
  totalPages?: number;
}

export interface GetMissionPhraseCResponse {
  success: boolean;
  message?: string;
  data?: MissionPhraseC[];
  total?: number;
  page?: number;
  size?: number;
  totalPages?: number;
}

export interface ImportMissionPhraseCsvResponse {
  success: boolean;
  message: string;
  savedCount: number;
  errorCount: number;
  errors?: string[];
}

export interface FetchAndSaveDrawResponse {
  success: boolean;
  message: string;
  drawNo: number;
  fetched: boolean;
  saved: boolean;
  isUpdate?: boolean;
  data?: {
    drawNo: number;
    drawDate: string;
    numbers: number[];
    bonus: number;
  };
  error?: string;
}
