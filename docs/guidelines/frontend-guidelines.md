# 프론트엔드 개발 가이드

> **프로젝트**: Lotto Guide Platform  
> **기술 스택**: React + TypeScript + Vite + Ant Design  
> **참조**: dadp-hub의 client-frontend 구조를 따름

## 개요

Lotto Guide Platform 프론트엔드는 dadp-hub의 프론트엔드 방식을 따라 React 18.3+, TypeScript 5.8+, Vite 6.3+, Ant Design 5.19+를 기반으로 구축됩니다.

### 주요 특징
- **타입 안정성**: TypeScript를 통한 컴파일 타임 타입 검증
- **빠른 개발**: Vite의 빠른 HMR과 빌드 성능
- **일관된 UI**: Ant Design을 통한 통일된 디자인 시스템
- **상태 관리**: Zustand를 통한 효율적인 상태 관리
- **실시간 통신**: WebSocket을 통한 실시간 데이터 업데이트

## 기술 스택

### 핵심 프레임워크
- **React**: 18.3+ (메인 프레임워크)
- **TypeScript**: 5.8+ (타입 안정성)
- **Vite**: 6.3+ (빌드 도구 및 개발 서버)

### UI 라이브러리
- **Ant Design**: 5.19+ (UI 컴포넌트 라이브러리)
- **Ant Design Icons**: 5.3+ (아이콘 라이브러리)
- **Chart.js**: 4.5+ (차트 라이브러리)
- **React Chart.js 2**: 5.3+ (React 차트 래퍼)

### 상태 관리
- **Zustand**: 5.0+ (상태 관리 라이브러리)
- **React Hook Form**: 7.58+ (폼 관리)

### API 통신
- **Axios**: 1.10+ (HTTP 클라이언트)
- **STOMP.js**: 7.2+ (WebSocket 통신)
- **Zod**: 3.25+ (스키마 검증)

## 프로젝트 구조

### 표준 디렉토리 구조 (dadp-hub 방식)
```
client-frontend/
├── src/
│   ├── components/                    # 공통 컴포넌트
│   │   ├── common/                    # 공통 UI 컴포넌트
│   │   │   ├── Header.tsx            # 헤더 컴포넌트
│   │   │   ├── Sidebar.tsx           # 사이드바 컴포넌트
│   │   │   ├── Layout.tsx            # 레이아웃 컴포넌트
│   │   │   ├── Loading.tsx           # 로딩 컴포넌트
│   │   │   ├── ErrorBoundary.tsx     # 에러 바운더리
│   │   │   └── ProtectedRoute.tsx    # 보호된 라우트
│   │   └── features/                  # 기능별 컴포넌트
│   ├── pages/                         # 페이지 컴포넌트
│   │   ├── Dashboard.tsx             # 대시보드 페이지
│   │   ├── Login.tsx                 # 로그인 페이지
│   │   └── features/                  # 기능별 페이지
│   ├── services/                      # API 서비스
│   │   ├── api.ts                    # API 클라이언트 설정
│   │   └── *.ts                      # 기능별 서비스
│   ├── stores/                        # 상태 관리 (Zustand)
│   ├── types/                         # TypeScript 타입 정의
│   ├── utils/                         # 유틸리티 함수
│   ├── hooks/                         # 커스텀 훅
│   ├── config/                        # 설정 파일
│   ├── contexts/                      # React Context
│   ├── styles/                        # 스타일 파일
│   ├── App.tsx                       # 메인 앱 컴포넌트
│   ├── main.tsx                      # 앱 진입점
│   └── router.tsx                    # 라우터 설정
├── public/                            # 정적 파일
├── scripts/                           # 빌드 스크립트
│   ├── generate-build-info.js        # 빌드 정보 생성
│   └── copy-build-info.js            # 빌드 정보 복사
├── package.json
├── vite.config.ts
└── tsconfig.json
```

## 개발 환경 설정

### 1. 필수 요구사항
```powershell
# Node.js 18+ 설치 확인
node --version

# npm 9+ 설치 확인
npm --version
```

### 2. 프로젝트 생성
```powershell
# Vite를 사용한 React + TypeScript 프로젝트 생성
npm create vite@latest client-frontend -- --template react-ts

# 프로젝트 디렉토리로 이동
cd client-frontend

# 의존성 설치
npm install
```

### 3. 필수 패키지 설치
```powershell
# UI 라이브러리
npm install antd @ant-design/icons

# 상태 관리
npm install zustand

# 라우팅
npm install react-router-dom

# API 통신
npm install axios

# 폼 관리
npm install react-hook-form @hookform/resolvers

# 실시간 통신
npm install @stomp/stompjs sockjs-client

# 스키마 검증
npm install zod

# 차트 라이브러리
npm install chart.js react-chartjs-2

# 유틸리티
npm install uuid qs
npm install --save-dev @types/uuid @types/qs

# 개발 도구
npm install -D @types/node rimraf
```

### 4. Vite 설정
```typescript
// vite.config.ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  base: '/lotto/', // 컨텍스트 패스 설정
  server: {
    host: '127.0.0.1',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
})
```

### 5. TypeScript 설정
```json
// tsconfig.json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

## 빌드 및 배포

### 1. 개발 서버 실행
```powershell
# 프로젝트 루트에서 실행
cd C:\Projects\Lotto-Guide-Platform
npm --prefix client-frontend run dev
```

### 2. 프로덕션 빌드
```powershell
# 프로젝트 루트에서 실행
cd C:\Projects\Lotto-Guide-Platform
npm --prefix client-frontend run build
```

### 3. 빌드 스크립트 (package.json)
```json
{
  "scripts": {
    "dev": "vite --base=/lotto/",
    "build": "rimraf dist && tsc -b && vite build && node scripts/generate-build-info.js && node scripts/copy-build-info.js",
    "lint": "eslint .",
    "preview": "vite preview"
  }
}
```

### 4. 빌드 정보 관리
dadp-hub 방식과 동일하게 빌드 정보를 관리합니다:
- `scripts/generate-build-info.js`: 빌드 정보 생성
- `scripts/copy-build-info.js`: 빌드 정보 복사
- `build-count.properties`: 빌드 카운트 관리

## 라우팅 설정

### 1. React Router 설정
```typescript
// src/router.tsx
import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from '@/components/common/Layout';
import { ProtectedRoute } from '@/components/common/ProtectedRoute';
import { Dashboard } from '@/pages/Dashboard';
import { Login } from '@/pages/Login';

const AppRouter: React.FC = () => (
  <BrowserRouter basename="/lotto">
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<Login />} />
      <Route element={<ProtectedRoute><Outlet /></ProtectedRoute>}>
        <Route element={<Layout />}>
          <Route path="/dashboard" element={<Dashboard />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  </BrowserRouter>
);
```

### 2. SPA Fallback 설정 (Spring Boot)
```java
// SpaRedirectController.java
@Controller
public class SpaRedirectController {
    
    @RequestMapping(value = {
        "/lotto/login",
        "/lotto/dashboard",
        "/lotto/{path:[^\\.]*}"
    })
    public String redirect() {
        return "forward:/index.html";
    }
}
```

## API 통신

### 1. API 클라이언트 설정
```typescript
// src/services/api.ts
import axios from 'axios';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

// 응답 인터셉터
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      window.location.href = '/lotto/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

## 상태 관리 (Zustand)

### 1. 스토어 작성
```typescript
// src/stores/authStore.ts
import { create } from 'zustand';

interface AuthStore {
  user: User | null;
  isAuthenticated: boolean;
  login: (user: User) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  isAuthenticated: false,
  login: (user) => set({ user, isAuthenticated: true }),
  logout: () => set({ user: null, isAuthenticated: false }),
}));
```

## 실행 위치 정책

### 핵심 원칙

**모든 프론트엔드 빌드/실행 명령은 반드시 프로젝트 루트(`C:\Projects\Lotto-Guide-Platform`)에서만 실행해야 합니다.**

```powershell
# ✅ 올바른 방법: 프로젝트 루트에서 실행
cd C:\Projects\Lotto-Guide-Platform
npm --prefix client-frontend run dev
npm --prefix client-frontend run build

# ❌ 금지: 하위 폴더로 이동 후 실행
cd client-frontend
npm run dev
npm run build
```

## 참조

- **dadp-hub 프론트엔드**: `C:\Projects\dadp\dadp-hub\client-frontend`
- **프론트엔드 가이드**: `docs/guides/frontend.md` (DADP 프로젝트)

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2026-01-09  
**작성자**: Lotto Guide Platform Development Team
