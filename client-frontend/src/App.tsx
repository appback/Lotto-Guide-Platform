import { ConfigProvider } from 'antd';
import koKR from 'antd/locale/ko_KR';
import { useEffect } from 'react';
import AppRouter from './router';
import './App.css';
import { adminService } from './services/adminService';

// 로컬 스토리지 키
const STORAGE_KEY_SYSTEM_OPTIONS = 'lotto_system_options';
const STORAGE_KEY_SYSTEM_OPTIONS_SERIAL = 'lotto_system_options_serial';

function App() {
  // 웹사이트 타이틀 동적 설정 (로컬 스토리지 기반, 일련번호 비교)
  useEffect(() => {
    const updateTitle = async () => {
      try {
        // 로컬 스토리지에서 옵션과 일련번호 조회
        const cachedOptionsStr = localStorage.getItem(STORAGE_KEY_SYSTEM_OPTIONS);
        const cachedSerial = localStorage.getItem(STORAGE_KEY_SYSTEM_OPTIONS_SERIAL);
        
        let options: Record<string, string> = {};
        
        if (cachedOptionsStr && cachedSerial) {
          try {
            options = JSON.parse(cachedOptionsStr);
            // 로컬 스토리지에 저장된 값으로 먼저 타이틀 설정
            if (options.site_title) {
              document.title = options.site_title;
            }
          } catch (e) {
            console.warn('로컬 스토리지 파싱 실패:', e);
          }
        }
        
        // 서버에서 옵션 조회 (일련번호 확인)
        const result = await adminService.getPublicSystemOptions();
        
        if (result.success && result.data && result.serialNumber) {
          const serverSerial = result.serialNumber;
          
          // 일련번호 비교
          if (!cachedSerial || cachedSerial !== serverSerial) {
            // 일련번호가 다르면 갱신
            options = result.data;
            localStorage.setItem(STORAGE_KEY_SYSTEM_OPTIONS, JSON.stringify(options));
            localStorage.setItem(STORAGE_KEY_SYSTEM_OPTIONS_SERIAL, serverSerial);
            
            // 타이틀 업데이트
            if (options.site_title) {
              document.title = options.site_title;
            } else {
              document.title = '로또 가이드';
            }
          }
          // 일련번호가 같으면 로컬 스토리지 값 사용 (이미 설정됨)
        } else {
          // API 실패 시 기본값 사용
          if (!options.site_title) {
            document.title = '로또 가이드';
          }
        }
      } catch (error) {
        console.warn('타이틀 조회 실패, 기본값 또는 캐시 사용:', error);
        // 로컬 스토리지에서 다시 시도
        try {
          const cachedOptionsStr = localStorage.getItem(STORAGE_KEY_SYSTEM_OPTIONS);
          if (cachedOptionsStr) {
            const options = JSON.parse(cachedOptionsStr);
            if (options.site_title) {
              document.title = options.site_title;
            } else {
              document.title = '로또 가이드';
            }
          } else {
            document.title = '로또 가이드';
          }
        } catch (e) {
          document.title = '로또 가이드';
        }
      }
    };

    updateTitle();
  }, []);

  return (
    <ConfigProvider locale={koKR}>
      <AppRouter />
    </ConfigProvider>
  );
}

export default App;
