# 빌드 관리 가이드

> **프로젝트**: Lotto Guide Platform  
> **버전**: 1.0.0  
> **최종 업데이트**: 2026-01-09

## 개요

Lotto Guide Platform 프로젝트의 빌드 관리 가이드입니다. dadp 프로젝트의 자동화 스크립트 방식을 따릅니다.

## 자동화 스크립트 사용 (권장)

### 기본 사용법

```powershell
# 프로젝트 루트에서 실행
cd C:\Projects\Lotto-Guide-Platform

# 로컬 배포 (빌드 + Docker 빌드 + 배포)
python scripts/run-automation.py --service lotto-api --local

# AWS 배포
python scripts/run-automation.py --service lotto-api --stage aws-deploy

# 빌드만
python scripts/run-automation.py --service lotto-api --stage build
```

### 금지 사항

- ❌ 수동 빌드: `mvn clean package` 직접 실행 금지
- ❌ 수동 Docker 빌드: `docker build` 직접 실행 금지
- ❌ 하위 폴더에서 실행: 프로젝트 루트 이외에서 실행 금지

## Maven 빌드 관리

### 1. Maven 명령어

```powershell
# 프로젝트 루트에서 실행
cd C:\Projects\Lotto-Guide-Platform

# 전체 빌드
mvn clean package

# 테스트 포함 빌드
mvn clean install

# 테스트만 실행
mvn test

# 테스트 스킵 빌드
mvn clean package -DskipTests

# JavaDoc 생성
mvn javadoc:javadoc

# 의존성 트리 확인
mvn dependency:tree
```

### 2. 빌드 실행 위치

**⚠️ 중요**: 모든 빌드 명령은 반드시 프로젝트 루트(`C:\Projects\Lotto-Guide-Platform`)에서 실행해야 합니다.

```powershell
# ✅ 올바른 방법: 프로젝트 루트에서 실행
cd C:\Projects\Lotto-Guide-Platform
mvn clean package

# ❌ 금지: 하위 폴더에서 실행
cd lotto-api
mvn clean package
```

### 3. 빌드 검증 체크리스트

- [ ] 전체 빌드 성공
- [ ] 테스트 통과
- [ ] 의존성 해결
- [ ] JAR 파일 생성
- [ ] 배포 준비 완료

## 애플리케이션 실행

### 1. Spring Boot 실행

```powershell
# 프로젝트 루트에서 실행
cd C:\Projects\Lotto-Guide-Platform
mvn spring-boot:run
```

### 2. 프로파일 설정

```powershell
# 개발 환경 (기본)
mvn spring-boot:run

# 프로덕션 환경
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## 로그 파일 관리

### 1. 빌드 로그 파일 생성

```powershell
# 빌드 로그
mvn clean package > build.log 2>&1

# 테스트 로그
mvn test > test.log 2>&1
```

### 2. 로그 파일 관리 규칙

- 모든 빌드 작업은 반드시 로그 파일 생성
- 로그 파일명은 명확하고 구분 가능하게 설정
- 로그 파일은 버전 관리에서 제외 (`.gitignore`에 추가)
- 로그 파일은 정기적으로 정리

## 버전 관리

### 1. 버전 업데이트 시 필수 작업

- `pom.xml` 버전 업데이트
- 빌드 실행하여 버전 동기화 확인
- 테스트 실행하여 정상 동작 확인
- 변경사항 문서화

### 2. 버전 정보 일관성 확인

- `pom.xml`의 버전 정보
- 문서 내 버전 참조

## 의존성 관리

### 1. 의존성 해결

```powershell
# 의존성 트리 확인
mvn dependency:tree

# 의존성 해결
mvn dependency:resolve

# 의존성 분석
mvn dependency:analyze
```

### 2. 의존성 업데이트

- 정기적인 의존성 버전 업데이트
- 보안 취약점 확인
- 호환성 테스트 수행

## 코드 검증

### 1. 코드 수정 시 즉시 빌드

코드 수정 후 반드시 빌드를 실행하여 검증해야 합니다.

```powershell
# 프로젝트 루트에서 실행
cd C:\Projects\Lotto-Guide-Platform
mvn clean package
```

### 2. 빌드 실패 시

빌드 실패 시 로그를 확인하고 문제를 해결한 후 다시 빌드를 실행합니다.

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2026-01-09  
**작성자**: Lotto Guide Platform Development Team
