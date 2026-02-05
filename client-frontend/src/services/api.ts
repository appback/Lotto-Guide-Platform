// dadp-hub 패턴: fetch + 절대 경로 사용
export const apiClient = {
  // GET 요청
  get: async <T>(endpoint: string, options?: { params?: Record<string, any> }): Promise<T> => {
    try {
      // 쿼리 파라미터 추가
      let url = endpoint;
      if (options?.params) {
        const params = new URLSearchParams();
        Object.entries(options.params).forEach(([key, value]) => {
          if (value !== undefined && value !== null) {
            params.append(key, String(value));
          }
        });
        const queryString = params.toString();
        if (queryString) {
          url += `?${queryString}`;
        }
      }

      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'same-origin',
      });

      // 401 에러 처리
      if (response.status === 401) {
        window.location.href = '/lotto/login';
        throw new Error('인증이 필요합니다.');
      }

      if (!response.ok) {
        let errorMessage = `HTTP ${response.status} 에러`;
        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorData.error || errorMessage;
        } catch {
          // JSON 파싱 실패 시 기본 메시지 사용
          if (response.status === 500) {
            errorMessage = '서버 내부 오류가 발생했습니다.';
          } else if (response.status === 404) {
            errorMessage = '요청한 리소스를 찾을 수 없습니다.';
          } else if (response.status === 401) {
            errorMessage = '인증이 필요합니다.';
          }
        }
        throw new Error(errorMessage);
      }

      const data = await response.json();
      return data;
    } catch (error) {
      console.error('apiClient.get 에러:', error);
      throw error;
    }
  },

  // POST 요청
  post: async <T>(endpoint: string, body: unknown): Promise<T> => {
    try {
      // X-User-Id 헤더 추가
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      };
      const userId = localStorage.getItem('userId');
      if (userId) {
        headers['X-User-Id'] = userId;
      }

      const response = await fetch(endpoint, {
        method: 'POST',
        headers,
        body: JSON.stringify(body || {}),
        credentials: 'same-origin',
      });

      // 401 에러 처리
      if (response.status === 401) {
        window.location.href = '/lotto/login';
        throw new Error('인증이 필요합니다.');
      }

      if (!response.ok) {
        let errorMessage = `HTTP ${response.status} 에러`;
        let errorDetails: any = null;
        try {
          const errorData = await response.json();
          errorDetails = errorData;
          errorMessage = errorData.message || errorData.error || errorMessage;
        } catch {
          // JSON 파싱 실패 시 기본 메시지 사용
          if (response.status === 500) {
            errorMessage = '서버 내부 오류가 발생했습니다.';
          } else if (response.status === 404) {
            errorMessage = '요청한 리소스를 찾을 수 없습니다.';
          } else if (response.status === 401) {
            errorMessage = '인증이 필요합니다.';
          } else if (response.status === 400) {
            errorMessage = '잘못된 요청입니다.';
          }
        }
        const error = new Error(errorMessage);
        (error as any).details = errorDetails;
        (error as any).status = response.status;
        throw error;
      }

      const data = await response.json();
      return data;
    } catch (error) {
      console.error('apiClient.post 에러:', error);
      throw error;
    }
  },
};
