# Git 명령어 사용 가이드

## 개요

Lotto Guide Platform 프로젝트에서 Git 명령어를 사용할 때의 가이드라인 및 주의사항을 정의합니다.

## 핵심 정책

### 1. 명령어 실행 정책
- **사용자 명시적 지시가 있을 때만 실행**
- 자동으로 커밋/푸시하지 않음
- 사용자가 '커밋해', '푸시해' 등으로 명시적으로 지시할 때만 실행

### 2. 실행 환경별 명령어 형식

#### PowerShell
```powershell
& "C:\Program Files\Git\bin\git.exe" <command>
```

#### Git Bash
```bash
git <command>
```

## 커밋 메시지 작성

### 형식
- Conventional Commits 형식 사용 권장
- 형식: `<type>: <subject>`
- 여러 줄 메시지는 본문에 상세 내용 작성
- **모든 Git 커밋 메시지는 반드시 영문(English)으로 작성해야 함**

### Type 종류
- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 수정
- `style`: 코드 포맷팅, 세미콜론 누락 등
- `refactor`: 코드 리팩토링
- `test`: 테스트 코드 추가/수정
- `chore`: 빌드 업무 수정, 패키지 매니저 설정 등

### 예시
```
feat: Add number generation API endpoint

- Implement POST /api/v1/generate endpoint
- Add number generation service with LLM integration
- Add validation for generation requests
```

## 명령어 제공 방식

### ❌ 금지 사항
1. **스크립트 파일 생성 금지**
   - `.sh`, `.ps1`, `.bat` 등 스크립트 파일을 생성하지 말 것
   - 사용자가 요청하지 않는 한 스크립트 파일 생성 금지

2. **불필요한 파일 생성 금지**
   - 임시 파일, 커밋 명령어 파일 등 생성 금지

### ✅ 올바른 방식
1. **직접 붙여넣을 수 있는 명령어만 제공**
   - Git Bash용: `cd /c/Projects/Lotto-Guide-Platform && git add . && git commit -m "..."` 형식
   - PowerShell용: 한 줄 또는 여러 줄로 직접 실행 가능한 명령어

2. **환경별 명령어 구분 제공**
   - 사용자가 사용하는 터미널 환경에 맞는 명령어 제공
   - Git Bash와 PowerShell 구분

## AI 작업 시 주의사항

1. **사용자 요청 시에만 Git 명령어 제공**
   - 커밋 메시지 추천 요청 시: 메시지만 제공
   - 명령어 필요 시: 직접 붙여넣을 수 있는 명령어만 제공

2. **스크립트 파일 생성 금지**
   - 사용자가 명시적으로 요청하지 않는 한 스크립트 파일 생성하지 않음

3. **환경 확인**
   - 사용자가 사용하는 터미널 환경 확인 (Git Bash / PowerShell)
   - 해당 환경에 맞는 명령어 형식 제공

## 참고
- Git 경로: `C:\Program Files\Git\bin\git.exe`
- 프로젝트 루트: `C:\Projects\Lotto-Guide-Platform`
- 저장소 URL: `https://github.com/appback/Lotto-Guide-Platform.git`

---

**문서 버전**: 1.0  
**최종 업데이트**: 2026-01-09  
**작성자**: Lotto Guide Platform Development Team
