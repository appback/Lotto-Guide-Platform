# 15. SPA 리다이렉트 설정

## 목표
- SPA(Single Page Application) 라우팅 지원
- 직접 URL 접근 시 404 방지
- dadp-hub 방식 참조

## 작업 항목

### 15.1 SpaRedirectController 생성 ✅
- [x] `SpaRedirectController` 생성
  - 위치: `api/controller/SpaRedirectController.java`
  - Context Path: `/lotto`
  - SPA 경로를 index.html로 포워딩
  - API 경로(`/api/**`)는 제외

### 15.2 WebConfig 업데이트 ✅
- [x] 정적 리소스 핸들러 설정 추가
  - `/lotto/assets/**`: 정적 리소스 (캐시 3600초)
  - `/lotto/build-info.json`, `/lotto/vite.svg`: 빌드 정보 (캐시 없음)
  - `/lotto/index.html`: 명시적 서빙 (캐시 없음)

### 15.3 Application Properties 설정 ✅
- [x] Context Path 설정
  - `server.servlet.context-path: /lotto`
  - 모든 요청은 `/lotto`로 시작

### 15.4 SecurityConfig 업데이트 ✅
- [x] SPA 라우팅 경로 허용
  - `/lotto/assets/**`: 정적 리소스
  - `/lotto`, `/lotto/`, `/lotto/index.html`: 메인 페이지
  - `/lotto/generate`, `/lotto/deep-generate`, `/lotto/history`: SPA 페이지

### 15.5 Maven 빌드 설정 ✅
- [x] 프론트엔드 빌드 결과물 복사
  - `maven-resources-plugin` 설정
  - `client-frontend/dist` → `target/classes/static` 복사
  - 빌드 시 자동으로 프론트엔드 결과물 포함

### 15.6 ApiExceptionHandler 개선 ✅
- [x] 정적 리소스 예외 처리
  - `NoResourceFoundException`, `NoHandlerFoundException` 별도 처리
  - favicon.ico, .well-known 등 브라우저 자동 요청 무시

## 구현 세부사항

### SPA 라우팅 흐름
1. 사용자가 `/lotto/generate` 직접 접근
2. `SpaRedirectController`가 요청을 받음
3. `static/index.html` 반환
4. React Router가 클라이언트 사이드에서 라우팅 처리

### 정적 리소스 서빙
- `/lotto/assets/**`: Vite 빌드 결과물 (JS, CSS 등)
- `/lotto/index.html`: SPA 진입점
- `/lotto/build-info.json`: 빌드 정보

### 빌드 프로세스
1. 프론트엔드 빌드: `npm --prefix client-frontend run build`
2. 백엔드 빌드: `mvn clean package`
   - `maven-resources-plugin`이 `client-frontend/dist`를 `target/classes/static`으로 복사
3. JAR 파일에 프론트엔드 결과물 포함

## 참고
- dadp-hub의 `SpaRedirectController`와 `WebConfig` 참조
- Context Path는 `/lotto`로 설정
- API 경로는 `/api/v1/**`로 유지
