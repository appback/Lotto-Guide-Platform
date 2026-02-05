import type { HistoryResponse, HistoryItemDto, GenerateResponse } from '@/types/api';

const STORAGE_KEY = 'lotto_history';
const MAX_ITEMS = 300;
const RETENTION_DAYS = 180;

interface StoredHistoryItem extends HistoryItemDto {
  createdAt: string; // ISO string
}

/**
 * 로컬스토리지 기반 히스토리 서비스
 * - 최대 300개 항목 저장
 * - 180일 자동 삭제
 */
export const historyService = {
  /**
   * 히스토리 조회 (로컬스토리지에서)
   */
  getHistory: async (page: number = 0, size: number = 10): Promise<HistoryResponse> => {
    try {
      const allItems = historyService.getAllHistory();
      const total = allItems.length;
      
      // 페이징 처리
      const startIndex = page * size;
      const endIndex = startIndex + size;
      const items = allItems.slice(startIndex, endIndex);
      
      return {
        items,
        total,
        page,
        size,
      };
    } catch (error) {
      console.error('히스토리 조회 실패:', error);
      return {
        items: [],
        total: 0,
        page,
        size,
      };
    }
  },

  /**
   * 모든 히스토리 조회 (정리된 상태)
   */
  getAllHistory: (): HistoryItemDto[] => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (!stored) {
        return [];
      }

      const items: StoredHistoryItem[] = JSON.parse(stored);
      const now = new Date().getTime();
      const retentionMs = RETENTION_DAYS * 24 * 60 * 60 * 1000;

      // 180일 이전 항목 필터링 및 정렬 (최신순)
      const validItems = items
        .filter(item => {
          const itemDate = new Date(item.createdAt).getTime();
          return (now - itemDate) < retentionMs;
        })
        .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
        .slice(0, MAX_ITEMS); // 최대 300개만 유지

      // 정리된 데이터를 다시 저장
      if (validItems.length !== items.length) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(validItems));
      }

      return validItems;
    } catch (error) {
      console.error('히스토리 읽기 실패:', error);
      return [];
    }
  },

  /**
   * 히스토리에 추가
   */
  addHistory: (response: GenerateResponse, strategy: string): void => {
    try {
      const allItems = historyService.getAllHistory();
      
      const newItem: StoredHistoryItem = {
        setId: response.setId || Date.now(), // setId가 없으면 타임스탬프 사용
        strategy,
        generatedCount: response.generatedSets.length,
        createdAt: new Date().toISOString(),
        numbers: response.generatedSets,
      };

      // 최신 항목을 맨 앞에 추가
      allItems.unshift(newItem);

      // 최대 300개만 유지
      const trimmedItems = allItems.slice(0, MAX_ITEMS);

      localStorage.setItem(STORAGE_KEY, JSON.stringify(trimmedItems));
    } catch (error) {
      console.error('히스토리 저장 실패:', error);
    }
  },

  /**
   * 히스토리 전체 삭제
   */
  clearHistory: (): void => {
    localStorage.removeItem(STORAGE_KEY);
  },
};
