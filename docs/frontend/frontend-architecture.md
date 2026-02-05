# 프론트엔드 레이어 구성 가이드

본 문서는 로또 번호 생성 플랫폼의 프론트엔드 아키텍처와 레이어 구성을 설명합니다.

## 목차

1. [전체 구조 개요](#전체-구조-개요)
2. [라우팅 구조](#라우팅-구조)
3. [페이지 계층](#페이지-계층)
4. [컴포넌트 계층](#컴포넌트-계층)
5. [서비스 계층](#서비스-계층)
6. [상태 관리](#상태-관리)
7. [스타일링](#스타일링)

---

## 전체 구조 개요

### 아키텍처 패턴

프론트엔드는 **탭 기반 단일 페이지 애플리케이션(SPA)** 구조를 따릅니다.

```
App.tsx
  └─ AppRouter (React Router)
      ├─ MainPage (탭 컨테이너)
      │   ├─ 운명의 번호 추천 (DeepGeneratePage)
      │   ├─ 로또 번호 추천 (GeneratePage)
      │   ├─ AI 기반 추천 (AIGeneratePage)
      │   └─ 히스토리 (HistoryPage)
      └─ AdminPage (관리자 페이지)
```

### 디렉토리 구조

```
client-frontend/src/
├── pages/              # 페이지 컴포넌트
│   ├── MainPage.tsx    # 탭 기반 메인 페이지
│   ├── GeneratePage.tsx      # 기본 전략 번호 생성
│   ├── DeepGeneratePage.tsx  # 운명의 번호 추천
│   ├── AIGeneratePage.tsx    # AI 전략 번호 생성
│   ├── HistoryPage.tsx       # 생성 히스토리
│   └── AdminPage.tsx         # 관리자 페이지
├── components/         # 재사용 가능한 컴포넌트
│   └── LottoBall.tsx  # 로또 볼 컴포넌트
├── services/          # API 서비스 레이어
│   ├── api.ts         # API 클라이언트
│   ├── generateService.ts
│   ├── historyService.ts
│   ├── adminService.ts
│   └── missionService.ts
├── types/             # TypeScript 타입 정의
│   └── api.ts
├── config/            # 설정 파일
│   └── environment.ts
├── stores/            # 상태 관리 (향후 확장)
│   └── authStore.ts
├── router.tsx         # 라우팅 설정
└── App.tsx            # 루트 컴포넌트
```

---

## 라우팅 구조

### 라우트 정의

**파일**: `src/router.tsx`

```typescript
/                    → MainPage (탭 기반 메인 페이지)
/admin               → AdminPage (관리자 페이지)
/*                   → MainPage (리다이렉트)
```

### 라우팅 특징

* **기본 경로**: `/lotto` (basename)
* **SPA 구조**: 클라이언트 사이드 라우팅
* **탭 기반 네비게이션**: 메인 페이지 내부에서 탭으로 이동
* **관리자 페이지**: 별도 라우트로 분리

---

## 페이지 계층

### 1. MainPage (탭 컨테이너)

**파일**: `src/pages/MainPage.tsx`

**역할**: 
* 탭 기반 네비게이션 제공
* 4개 주요 기능을 탭으로 통합

**탭 구성**:

| 탭 키 | 탭 이름 | 컴포넌트 | 설명 |
|------|--------|---------|------|
| `destiny` | 운명의 번호 추천 | `DeepGeneratePage` | 별자리 기반 랜덤 생성 |
| `basic` | 로또 번호 추천 | `GeneratePage` | 기본 전략 6개 |
| `ai` | AI 기반 추천 (유료) | `AIGeneratePage` | AI 전략 4개 (준비 중) |
| `history` | 히스토리 | `HistoryPage` | 생성 이력 조회 |

**상태 관리**:
* `activeTab`: 현재 활성화된 탭 (로컬 상태)

---

### 2. DeepGeneratePage (운명의 번호 추천)

**파일**: `src/pages/DeepGeneratePage.tsx`

**역할**:
* 별자리 기반 운명의 번호 추천
* 랜덤 생성 (전략: `BALANCED` 고정)

**주요 기능**:
* 생년월일 입력 (선택사항)
* 별자리 선택
* 번호 생성 (랜덤)
* 미션 텍스트 생성 (별자리 기반)

**상태 관리**:
* `ballSize`: 볼 크기 (localStorage)
* `zodiacSign`: 선택한 별자리 (localStorage)
* `birthDate`: 생년월일
* `count`: 생성 개수
* `generatedSets`: 생성된 번호 세트
* `mission`: 미션 텍스트

**API 호출**:
* `generateService.generate()`: 번호 생성
* `missionService.generateMission()`: 미션 생성

---

### 3. GeneratePage (로또 번호 추천)

**파일**: `src/pages/GeneratePage.tsx`

**역할**:
* 기본 전략 6개를 사용한 번호 생성

**지원 전략**:
* `FREQUENT_TOP`: 고빈도 우선
* `OVERDUE_TOP`: 과거 데이터 우선
* `BALANCED`: 균형
* `WHEELING_SYSTEM`: 5등 보장 조합
* `WEIGHTED_RANDOM`: 통계 가중 랜덤
* `PATTERN_MATCHER`: 패턴 필터링

**주요 기능**:
* 전략 선택
* 생성 개수 설정
* 볼 크기 선택
* 전략 상세 정보 모달

**상태 관리**:
* `strategy`: 선택한 전략
* `count`: 생성 개수
* `ballSize`: 볼 크기 (localStorage)
* `strategyTips`: 전략별 간단 설명
* `strategyDetails`: 전략별 상세 정보

**API 호출**:
* `generateService.generate()`: 번호 생성
* `generateService.getStrategyDescriptions()`: 전략 설명 조회

---

### 4. AIGeneratePage (AI 기반 추천)

**파일**: `src/pages/AIGeneratePage.tsx`

**역할**:
* AI 전략 안내 페이지 (현재 준비 중)

**예정된 AI 전략**:
* `AI_PATTERN_REASONER`: AI 패턴 분석 추천
* `AI_DECISION_FILTER`: AI 판단 필터 추천
* `AI_SIMULATED_LEARNING`: AI 시뮬레이션 추천
* `AI_WEIGHT_EVOLUTION`: AI 가중치 진화 추천

**현재 상태**:
* 🚧 개발 중
* 유료 플랜 안내 표시
* 기술적 설명 제공

---

### 5. HistoryPage (히스토리)

**파일**: `src/pages/HistoryPage.tsx`

**역할**:
* 생성된 번호 이력 조회

**주요 기능**:
* 페이지네이션
* 볼 크기 선택
* 전략별 필터링 (향후 확장 가능)

**상태 관리**:
* `history`: 히스토리 목록
* `page`: 현재 페이지
* `size`: 페이지 크기
* `ballSize`: 볼 크기 (localStorage)

**데이터 소스**:
* `historyService.getHistory()`: 로컬 스토리지에서 조회

---

### 6. AdminPage (관리자 페이지)

**파일**: `src/pages/AdminPage.tsx`

**역할**:
* 관리자 전용 기능 제공

**주요 기능**:
* 추첨 데이터 수집
* CSV 업로드/다운로드
* 전략 설명 관리
* 회차 데이터 관리

**접근 제어**:
* 별도 라우트 (`/admin`)
* 향후 인증 추가 예정

---

## 컴포넌트 계층

### LottoBall

**파일**: `src/components/LottoBall.tsx`

**역할**: 개별 로또 번호 볼 렌더링

**Props**:
```typescript
{
  number: number;        // 로또 번호 (1-45)
  size?: 'small' | 'medium' | 'large';
  animated?: boolean;     // 생성 시 애니메이션
}
```

**특징**:
* 번호 범위별 색상 자동 할당
* 크기별 반응형 스타일
* CSS 애니메이션 지원

---

### LottoBallGroup

**파일**: `src/components/LottoBall.tsx`

**역할**: 여러 로또 번호를 그룹으로 표시

**Props**:
```typescript
{
  numbers: number[];      // 번호 배열
  size?: 'small' | 'medium' | 'large';
  animated?: boolean;
  gap?: number;          // 볼 간격 (px)
}
```

**특징**:
* 반응형 레이아웃
* 동적 간격 조정
* 볼 크기에 따른 간격 자동 조정

---

## 서비스 계층

### API 클라이언트

**파일**: `src/services/api.ts`

**역할**:
* 공통 API 클라이언트
* 요청/응답 인터셉터
* 에러 처리

**기능**:
* `get<T>()`: GET 요청
* `post<T>()`: POST 요청
* `put<T>()`: PUT 요청
* `delete<T>()`: DELETE 요청

---

### generateService

**파일**: `src/services/generateService.ts`

**역할**: 번호 생성 관련 API 호출

**메서드**:
* `generate(request)`: 번호 생성
* `getStrategyDescriptions()`: 전략 설명 조회

**특징**:
* 생성 후 자동으로 히스토리에 저장

---

### historyService

**파일**: `src/services/historyService.ts`

**역할**: 히스토리 관리 (로컬 스토리지)

**메서드**:
* `addHistory()`: 히스토리 추가
* `getHistory()`: 히스토리 조회
* `clearHistory()`: 히스토리 삭제

**데이터 저장소**: `localStorage`

---

### adminService

**파일**: `src/services/adminService.ts`

**역할**: 관리자 기능 API 호출

**주요 기능**:
* CSV 업로드/다운로드
* 추첨 데이터 수집
* 전략 설명 관리
* 회차 데이터 관리

---

### missionService

**파일**: `src/services/missionService.ts`

**역할**: 미션 텍스트 생성

**메서드**:
* `generateMission()`: 별자리 기반 미션 생성

---

## 상태 관리

### 로컬 스토리지 (localStorage)

**사용 목적**: 클라이언트 사이드 영구 저장

**저장 데이터**:

| 키 | 용도 | 사용 페이지 |
|---|------|-----------|
| `lotto_ball_size` | 볼 크기 설정 | 모든 페이지 |
| `lotto_last_zodiac` | 마지막 선택 별자리 | DeepGeneratePage |
| `lotto_strategy_content_hashes` | 전략 설명 해시 | AdminPage |
| `lotto_history_*` | 생성 히스토리 | HistoryPage |

---

### React State

**사용 목적**: 컴포넌트 내부 상태 관리

**주요 상태**:
* 폼 입력 값
* 로딩 상태
* 생성된 번호 세트
* 모달 표시 여부
* 탭 활성화 상태

---

### 전역 상태 (향후 확장)

**파일**: `src/stores/authStore.ts`

**현재 상태**: 기본 구조만 존재, 향후 확장 예정

**예정된 기능**:
* 사용자 인증 상태
* 플랜 정보 (Basic / AI)
* 설정 정보

---

## 스타일링

### CSS 파일

**컴포넌트별 CSS**:
* `LottoBall.css`: 로또 볼 스타일

**전역 CSS**:
* `App.css`: 앱 전역 스타일
* `index.css`: 기본 리셋 및 전역 스타일

---

### Ant Design

**UI 프레임워크**: Ant Design (antd)

**사용 컴포넌트**:
* `Card`: 카드 레이아웃
* `Tabs`: 탭 네비게이션
* `Button`: 버튼
* `Select`: 드롭다운
* `InputNumber`: 숫자 입력
* `Table`: 테이블
* `Modal`: 모달
* `Alert`: 알림
* `Radio`: 라디오 버튼
* `Space`: 간격 조정
* `Typography`: 텍스트 스타일

**로케일**: 한국어 (`ko_KR`)

---

## 데이터 흐름

### 번호 생성 흐름

```
사용자 입력
  ↓
GeneratePage / DeepGeneratePage
  ↓
generateService.generate()
  ↓
API 호출 (POST /api/v1/generate)
  ↓
응답 수신
  ↓
히스토리 저장 (historyService)
  ↓
UI 업데이트
```

### 전략 설명 로드 흐름

```
페이지 마운트
  ↓
generateService.getStrategyDescriptions()
  ↓
API 호출 (GET /api/v1/generate/strategy-descriptions)
  ↓
응답 수신
  ↓
상태 업데이트 (strategyTips, strategyDetails)
  ↓
UI 표시
```

---

## 반응형 디자인

### 볼 크기 시스템

**3단계 크기**:
* `small`: 모바일 최적화 (간격 4-6px)
* `medium`: 기본 크기 (간격 8-12px)
* `large`: 데스크톱 (간격 12px)

**저장**: localStorage에 저장되어 모든 페이지에서 일관성 유지

---

## 향후 확장 계획

### 1. 상태 관리 라이브러리

**예정**: Zustand 또는 Redux Toolkit

**용도**:
* 전역 상태 관리
* 사용자 인증
* 플랜 정보
* 설정 정보

---

### 2. AI 전략 통합

**예정**: AIGeneratePage에 실제 AI 전략 추가

**필요 작업**:
* AI 전략 선택 UI
* ML 모델 호출 통합
* 유료 플랜 체크

---

### 3. 인증 시스템

**예정**: JWT 기반 인증

**구현 계획**:
* 로그인/회원가입 페이지
* 토큰 관리
* 플랜별 접근 제어

---

## 기술 스택

### 핵심 라이브러리

| 라이브러리 | 버전 | 용도 |
|----------|------|------|
| React | 18.x | UI 프레임워크 |
| TypeScript | 5.x | 타입 안정성 |
| Ant Design | 5.x | UI 컴포넌트 |
| React Router | 6.x | 라우팅 |
| Vite | 5.x | 빌드 도구 |

---

## 개발 가이드

### 새 페이지 추가

1. `src/pages/`에 새 페이지 컴포넌트 생성
2. `MainPage.tsx`의 탭에 추가 (필요시)
3. 또는 `router.tsx`에 새 라우트 추가

### 새 서비스 추가

1. `src/services/`에 새 서비스 파일 생성
2. `api.ts`의 `apiClient` 사용
3. 타입 정의는 `src/types/api.ts`에 추가

### 새 컴포넌트 추가

1. `src/components/`에 새 컴포넌트 생성
2. 재사용 가능한 컴포넌트로 설계
3. Props 타입 명시

---

## 파일 구조 상세

### pages/

```
pages/
├── MainPage.tsx           # 탭 컨테이너 (메인 페이지)
├── GeneratePage.tsx        # 기본 전략 번호 생성
├── DeepGeneratePage.tsx   # 운명의 번호 추천
├── AIGeneratePage.tsx     # AI 전략 번호 생성
├── HistoryPage.tsx        # 생성 히스토리
└── AdminPage.tsx          # 관리자 페이지
```

### services/

```
services/
├── api.ts                 # API 클라이언트 (공통)
├── generateService.ts     # 번호 생성 서비스
├── historyService.ts      # 히스토리 서비스 (localStorage)
├── adminService.ts        # 관리자 서비스
└── missionService.ts      # 미션 생성 서비스
```

### components/

```
components/
├── LottoBall.tsx          # 로또 볼 컴포넌트
└── LottoBall.css          # 로또 볼 스타일
```

---

## 업데이트 이력

- **2026-01-14**: 초기 문서 작성
  - 탭 기반 레이아웃 구조 문서화
  - 페이지 계층 및 컴포넌트 구조 정리
  - 서비스 레이어 설명 추가

---

## 참고 자료

- [전략 가이드](../api/number-generation-strategies.md)
- [플랜 구성 가이드](../service/strategy-pricing-guide.md)
- [API 명세서](../api/api-specification.md)
