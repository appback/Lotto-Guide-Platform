# 환경 설정 가이드

> **프로젝트**: Lotto Guide Platform  
> **버전**: 1.0.0  
> **최종 업데이트**: 2026-01-09

## 개요

Lotto Guide Platform 프로젝트의 환경 설정 가이드를 정의합니다.

## 개발 환경 설정

### 1. Java 설정

- **Java 버전**: 17
- **Java 경로**: `C:\Program Files\JetBrains\IntelliJ IDEA 2025.1.3\jbr\bin`

### 2. Maven 설정

- **Maven 버전**: 3.9.9
- **Maven 경로**: `C:\Program Files\JetBrains\IntelliJ IDEA 2025.1.3\plugins\maven\lib\maven3`

### 3. 데이터베이스 설정

#### PostgreSQL (기본)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lottoguide
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
```

#### MySQL (선택)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lottoguide
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
```

## 프로파일 설정

### 1. 개발 환경 (dev)

기본 프로파일입니다.

```yaml
spring:
  profiles:
    active: dev
```

### 2. 프로덕션 환경 (prod)

```yaml
spring:
  profiles:
    active: prod
```

## 환경 변수 설정

### 1. 데이터베이스 연결 정보

환경 변수를 통해 데이터베이스 연결 정보를 설정할 수 있습니다.

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "postgres"
```

### 2. 애플리케이션 설정

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
```

## application.yml 수정 정책

### 1. 수정 전 확인사항

- 수정 이유 확인
- 기존/변경 설정값 비교
- 다른 서비스 영향도 검토

### 2. 수정 후 검증

- 빌드 실행하여 정상 동작 확인
- 테스트 실행하여 검증

### 3. 금지 사항

- 명확한 이유 없이 `application.yml` 수정 금지
- 수정 후 검증 없이 진행 금지

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2026-01-09  
**작성자**: Lotto Guide Platform Development Team
