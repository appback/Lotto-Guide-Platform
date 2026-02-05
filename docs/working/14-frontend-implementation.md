# 14. 프론트엔드 구현

## 목표
- React + TypeScript + Vite 기반 프론트엔드 구현
- dadp-hub 방식 참조
- 기본 생성 및 Deep 생성 기능 구현

## 작업 항목

### 14.1 프로젝트 초기 설정 ✅
- [x] Vite + React + TypeScript 프로젝트 생성
  - `npm create vite@latest client-frontend -- --template react-ts`
- [x] 필수 패키지 설치
  - UI: `antd`, `@ant-design/icons`
  - 상태 관리: `zustand`
  - 라우팅: `react-router-dom`
  - API 통신: `axios`
  - 폼 관리: `react-hook-form`, `@hookform/resolvers`
  - 유틸리티: `zod`, `uuid`, `qs`, `dayjs`
  - 타입: `@types/uuid`, `@types/qs`, `@types/node`

### 14.2 기본 구조 생성 ✅
- [x] 디렉토리 구조 생성
  - `components/common`, `components/features`
  - `pages`, `services`, `stores`, `types`, `utils`, `hooks`, `config`, `styles`
- [x] API 클라이언트 설정
  - `services/api.ts`: Axios 인스턴스, 인터셉터 설정
  - `X-User-Id` 헤더 자동 추가
- [x] 타입 정의
  - `types/api.ts`: API 요청/응답 타입 정의
- [x] 서비스 레이어
  - `services/generateService.ts`: 번호 생성 API
  - `services/missionService.ts`: 미션 생성 API
  - `services/historyService.ts`: 히스토리 조회 API
- [x] 상태 관리
  - `stores/authStore.ts`: 인증 상태 관리 (Zustand)

### 14.3 라우터 설정 ✅
- [x] `router.tsx` 생성
  - React Router 설정
  - base path: `/lotto`
  - 라우트:
    - `/generate`: 기본 생성 페이지
    - `/deep-generate`: Deep 생성 페이지
    - `/history`: 히스토리 페이지

### 14.4 핵심 페이지 구현 ✅
- [x] `GeneratePage` (기본 번호 생성)
  - 전략 선택 (FREQUENT_TOP, OVERDUE_TOP, BALANCED)
  - 생성 개수 설정
  - 윈도우 크기 설정 (20, 50, 100)
  - 생성된 번호 표시
  - Explain Tags 표시
- [x] `DeepGeneratePage` (Deep 생성)
  - 생년월일 입력 (DatePicker)
  - 번호 생성 + 미션 생성 연동
  - 별자리 정보 표시
  - 미션 텍스트 표시
- [x] `HistoryPage` (히스토리 조회)
  - 테이블 형식으로 히스토리 표시
  - 페이징 처리
  - 생성된 번호 세트 표시

### 14.5 Vite 설정 ✅
- [x] `vite.config.ts` 업데이트
  - base path: `/lotto/`
  - 개발 서버: `127.0.0.1:5173`
  - API 프록시: `/api` → `http://localhost:8083`
  - TypeScript path alias: `@/*` → `./src/*`
- [x] `tsconfig.app.json` 업데이트
  - path mapping 설정
- [x] `package.json` 스크립트 업데이트
  - `dev`: `vite --base=/lotto/`
  - `build`: `rimraf dist && tsc -b && vite build`

### 14.6 App 설정 ✅
- [x] `App.tsx` 업데이트
  - Ant Design ConfigProvider 설정
  - 한국어 로케일 설정
- [x] `main.tsx` 업데이트
  - Ant Design CSS import

## 구현 세부사항

### API 통신
- Base URL: `http://localhost:8083/api/v1`
- 요청 인터셉터: `X-User-Id` 헤더 자동 추가
- 응답 인터셉터: 401 에러 시 로그인 페이지로 리다이렉트

### 페이지 구조
1. **GeneratePage**: 기본 번호 생성
   - 전략, 개수, 윈도우 크기 선택
   - 생성 버튼 클릭 시 API 호출
   - 결과 표시

2. **DeepGeneratePage**: Deep 생성
   - 생년월일 입력
   - 번호 생성 → Explain Tags 추출 → 미션 생성
   - 별자리 정보 및 미션 텍스트 표시

3. **HistoryPage**: 히스토리 조회
   - 테이블 형식
   - 페이징 지원
   - 생성된 번호 세트 표시

## 빌드 확인
- ✅ TypeScript 컴파일 성공
- ✅ Vite 빌드 성공
- ✅ 모든 타입 오류 해결

## 다음 단계
- 백엔드와 통합 테스트
- UI/UX 개선
- 에러 처리 강화
- 로딩 상태 개선
