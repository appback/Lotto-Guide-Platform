# 자동화 원칙 가이드

> **프로젝트**: Lotto Guide Platform  
> **버전**: 1.0.0  
> **최종 업데이트**: 2026-01-09

## 개요

Lotto Guide Platform 프로젝트의 자동화 원칙 및 실행 규칙을 정의합니다. dadp 프로젝트의 자동화 스크립트 방식을 따릅니다.

## 자동화 스크립트 사용

### 핵심 원칙

**모든 빌드/배포는 `scripts/run-automation.py` 스크립트를 사용해야 합니다. 수동 빌드/배포는 금지됩니다.**

### 기본 사용법

```powershell
# 프로젝트 루트에서 실행
cd C:\Projects\Lotto-Guide-Platform

# 로컬 배포
python scripts/run-automation.py --service lotto-api --local

# AWS 배포
python scripts/run-automation.py --service lotto-api --stage aws-deploy

# 빌드만
python scripts/run-automation.py --service lotto-api --stage build

# Docker 재빌드
python scripts/run-automation.py --service lotto-api --stage docker-rebuild
```

### 주요 옵션

- `--service lotto-api`: 서비스 선택 (기본값: lotto-api)
- `--local`: 로컬 배포 모드 (localhost Docker)
- `--stage`: 실행 단계
  - `build`: 백엔드 빌드만
  - `deploy`: 배포만
  - `backend-build`: 백엔드 빌드만
  - `docker-rebuild`: Docker 재빌드
  - `all`: 전체 프로세스 (기본값)
  - `aws-deploy`: AWS 배포
- `--skip-prerequisites`: 사전 요구사항 확인 건너뛰기
- `--monitor`: 성능 모니터링 활성화

## 실행 위치 정책

### 핵심 원칙

**모든 빌드/배포/테스트 명령은 반드시 프로젝트 루트(`C:\Projects\Lotto-Guide-Platform`)에서만 실행해야 합니다.**

```powershell
# ✅ 올바른 방법: 프로젝트 루트에서 실행
cd C:\Projects\Lotto-Guide-Platform
mvn clean package

# ❌ 금지: 하위 폴더에서 실행
cd lotto-api
mvn clean package
```

## PowerShell 명령어 실행 규칙

### 1. 명령어 체이닝 규칙

PowerShell에서는 `&&` 대신 `;`를 사용해야 합니다.

```powershell
# ✅ 올바른 방법
cd C:\Projects\Lotto-Guide-Platform; mvn clean package

# ❌ 금지
cd C:\Projects\Lotto-Guide-Platform && mvn clean package
```

### 2. 환경변수 설정

```powershell
# 환경변수 설정
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# 환경변수 확인
echo $env:JAVA_HOME
echo $env:Path
```

## 파일 생성 및 관리 원칙

### 1. UTF-8 인코딩 필수

모든 스크립트 파일은 반드시 UTF-8 인코딩으로 생성해야 합니다.

### 2. 파일 생성 확인

파일 생성 후 1회만 확인하고, 실패 시 사용자에게 확인을 요청합니다.

## 코드 검증 원칙

### 1. 코드 수정 시 즉시 빌드

코드 수정 시 반드시 빌드를 실행하여 검증해야 합니다.

```powershell
# 프로젝트 루트에서 실행
cd C:\Projects\Lotto-Guide-Platform
mvn clean package
```

### 2. 빌드 실패 처리

빌드 실패 시 로그를 확인하고 문제를 해결한 후 다시 빌드를 실행합니다.

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2026-01-09  
**작성자**: Lotto Guide Platform Development Team
