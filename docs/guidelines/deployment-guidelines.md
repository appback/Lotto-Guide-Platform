# 배포 가이드라인

> **프로젝트**: Lotto Guide Platform  
> **배포 방식**: Docker + AWS EC2  
> **참조**: dadp 프로젝트의 배포 방식을 따름

## 개요

Lotto Guide Platform은 Docker를 사용하여 AWS EC2 인스턴스에 배포됩니다. 모든 접속 정보는 환경변수/시크릿으로 관리하며 하드코딩을 금지합니다.

## 목차

1. [로컬 개발 환경](#로컬-개발-환경)
2. [Docker 배포](#docker-배포)
3. [AWS 운영 환경](#aws-운영-환경)
4. [원격 접근 정책](#원격-접근-정책)
5. [환경별 설정](#환경별-설정)
6. [프로덕션 배포 체크리스트](#프로덕션-배포-체크리스트)

## 로컬 개발 환경

### 서비스 포트
- **Backend API**: 8080
- **Frontend**: 5173 (개발 서버)
- **PostgreSQL**: 5432
- **MySQL**: 3306 (선택)

### 데이터베이스 설정
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lottoguide
    username: postgres
    password: postgres
```

### 환경 변수 예시
```powershell
# Backend
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "lottoguide"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "postgres"
```

## 자동화 스크립트 사용 (권장)

### 기본 사용법

```powershell
# 프로젝트 루트에서 실행
cd C:\Projects\Lotto-Guide-Platform

# 로컬 배포
python scripts/run-automation.py --service lotto-api --local

# AWS 배포
python scripts/run-automation.py --service lotto-api --stage aws-deploy
```

### 자동화 스크립트가 수행하는 작업

1. 사전 요구사항 확인 (Python, Docker)
2. 백엔드 빌드 (Maven)
3. Docker 이미지 빌드
4. 로컬 배포 또는 AWS 배포
5. 헬스체크 확인

## Docker 배포

### 1. Dockerfile 구조

#### Backend Dockerfile
```dockerfile
# lotto-api/Dockerfile
FROM eclipse-temurin:17-jdk-alpine

# wget 설치 (헬스체크용)
RUN apk add --no-cache wget

# 작업 디렉토리 설정
WORKDIR /app

# JAR 파일 복사
COPY target/lotto-api-*.jar app.jar

# 포트 노출
EXPOSE 8080

# 헬스 체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2. Docker Compose 구성

#### 로컬 개발 환경 (docker-compose.local.yml)
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: lotto-postgres-local
    restart: unless-stopped
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: lottoguide
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d lottoguide"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - lotto-network

  lotto-api:
    build:
      context: ../lotto-api
      dockerfile: Dockerfile
    container_name: lotto-api-local
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - SERVER_PORT=8080
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME=lottoguide
      - DB_USERNAME=postgres
      - DB_PASSWORD=postgres
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/lottoguide
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=postgres
      - SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - lotto-network
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

volumes:
  postgres_data:

networks:
  lotto-network:
    driver: bridge
```

#### AWS 운영 환경 (docker-compose.aws.yml)
```yaml
version: '3.8'

services:
  lotto-api:
    image: lotto-api:latest
    container_name: lotto-api
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SERVER_PORT=8080
      # 환경 변수는 env-sync 스크립트 또는 SSM에서 주입
      - DB_HOST=${DB_HOST}
      - DB_PORT=${DB_PORT}
      - DB_NAME=${DB_NAME}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
    env_file:
      - /etc/lotto-api.env
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### 3. Docker 빌드 및 실행

#### 로컬 개발 환경
```powershell
# 프로젝트 루트에서 실행
cd C:\Projects\Lotto-Guide-Platform

# Docker Compose로 실행
docker-compose -f docker-compose.local.yml up -d

# 로그 확인
docker-compose -f docker-compose.local.yml logs -f lotto-api

# 중지
docker-compose -f docker-compose.local.yml down
```

#### 이미지 빌드
```powershell
# Backend 이미지 빌드
cd C:\Projects\Lotto-Guide-Platform
docker build -t lotto-api:latest -f lotto-api/Dockerfile lotto-api/
```

## AWS 운영 환경

### 1. AWS 인프라 구성

#### EC2 인스턴스 (엔진2 AWS)
- **인스턴스 ID**: `i-039650bd0704f2e6f`
- **인스턴스 이름**: `dadp-engine-2` (공유 인스턴스)
- **인스턴스 타입**: `t2.medium` (프로세싱 파워 여유 있음)
- **퍼블릭 DNS**: `ec2-15-164-228-217.ap-northeast-2.compute.amazonaws.com`
- **퍼블릭 IP**: `15.164.228.217`
- **프라이빗 IP**: `172.31.26.161`
- **운영 체제**: Amazon Linux
- **보안 그룹**: `sg-095f85d6a1e86a41f`
  - 인바운드: 8080 (HTTP), 22 (SSH - SSM 사용 권장)
  - 아웃바운드: 모든 트래픽 허용
- **IAM 역할**: `dadp-ec2-ssm-role`
- **작업 디렉토리**: `/opt/lotto/docker` 또는 `/home/ec2-user/docker`

> **참고**: 상세 인스턴스 정보는 [인스턴스 정보 문서](../infrastructure/lotto-api-instance-info.md)를 참조하세요.

#### RDS (PostgreSQL)
- **엔진**: PostgreSQL 15+
- **인스턴스 클래스**: db.t3.micro 이상
- **보안 그룹**: EC2 인스턴스 보안 그룹에서만 접근 허용 (5432)

### 2. 환경 변수 관리 (SSM Parameter Store)

#### SSM 파라미터 표준 키
```
/lotto/api/DB_HOST
/lotto/api/DB_PORT
/lotto/api/DB_NAME
/lotto/api/DB_USERNAME
/lotto/api/DB_PASSWORD (SecureString)
/lotto/api/SPRING_PROFILES_ACTIVE
```

#### env-sync 스크립트 예시
```bash
#!/bin/bash
# /usr/local/bin/lotto-api-env-sync.sh

# SSM에서 환경 변수 읽기
DB_HOST=$(aws ssm get-parameter --name /lotto/api/DB_HOST --query 'Parameter.Value' --output text --region ap-northeast-2)
DB_PORT=$(aws ssm get-parameter --name /lotto/api/DB_PORT --query 'Parameter.Value' --output text --region ap-northeast-2)
DB_NAME=$(aws ssm get-parameter --name /lotto/api/DB_NAME --query 'Parameter.Value' --output text --region ap-northeast-2)
DB_USERNAME=$(aws ssm get-parameter --name /lotto/api/DB_USERNAME --query 'Parameter.Value' --output text --region ap-northeast-2)
DB_PASSWORD=$(aws ssm get-parameter --name /lotto/api/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text --region ap-northeast-2)

# 환경 변수 파일 생성
cat > /etc/lotto-api.env <<EOF
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
DB_HOST=${DB_HOST}
DB_PORT=${DB_PORT}
DB_NAME=${DB_NAME}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}
SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
EOF

# 서비스 재시작
systemctl restart lotto-api
```

### 3. 배포 프로세스

#### 1단계: 이미지 빌드 및 푸시
```powershell
# 로컬에서 이미지 빌드
cd C:\Projects\Lotto-Guide-Platform
docker build -t lotto-api:latest -f lotto-api/Dockerfile lotto-api/

# ECR에 푸시 (ECR 설정 후)
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin <ECR_URI>
docker tag lotto-api:latest <ECR_URI>/lotto-api:latest
docker push <ECR_URI>/lotto-api:latest
```

#### 2단계: AWS 인스턴스에서 배포
```bash
# SSM 접속 (권장)
aws ssm start-session --target i-039650bd0704f2e6f --region ap-northeast-2

# 또는 CloudShell에서 실행
cd /opt/lotto/docker || cd /home/ec2-user/docker

# 최신 이미지 Pull (ECR 사용 시)
# docker pull <ECR_URI>/lotto-api:latest

# 환경 변수 동기화
sudo /usr/local/bin/lotto-api-env-sync.sh

# Docker Compose 재시작
docker-compose -f docker-compose.aws.yml up -d --force-recreate lotto-api

# 로그 확인
docker-compose -f docker-compose.aws.yml logs -f lotto-api

# 헬스체크 확인
curl http://localhost:8080/actuator/health
```

## 원격 접근 정책

### 핵심 정책

**SSH/키 기반 접속은 금지합니다. 운영/테스트 서버 접근은 AWS Systems Manager (SSM) 또는 AWS CloudShell만 사용합니다.**

### SSM Session Manager 사용 (권장)
```bash
# SSM 세션 시작
aws ssm start-session --target i-039650bd0704f2e6f --region ap-northeast-2

# 세션 내에서 실행
cd /opt/lotto/docker || cd /home/ec2-user/docker
docker-compose ps
docker-compose logs --tail=100 lotto-api
```

### CloudShell 사용
```bash
# AWS 콘솔 → CloudShell 실행 후
cd /opt/lotto/docker || cd /home/ec2-user/docker
docker-compose ps
docker-compose logs --tail=100 lotto-api
```

### SSM 명령 실행
```bash
aws ssm send-command \
  --instance-ids i-039650bd0704f2e6f \
  --region ap-northeast-2 \
  --document-name "AWS-RunShellScript" \
  --parameters '{"commands":[
    "set -e",
    "cd /opt/lotto/docker || cd /home/ec2-user/docker",
    "docker-compose ps",
    "docker-compose logs --tail=100 lotto-api"
  ]}'
```

## 환경별 설정

### 개발 환경 (로컬)
- **프로필**: `dev`
- **데이터베이스**: 로컬 PostgreSQL
- **포트**: 8080

### 운영 환경 (AWS)
- **프로필**: `prod`
- **데이터베이스**: AWS RDS PostgreSQL
- **포트**: 8080
- **보안**: VPC 내부 통신, 보안 그룹 제한

## 프로덕션 배포 체크리스트

### 사전 준비
- [ ] 인스턴스 정보 확인
  - 인스턴스 ID: `i-039650bd0704f2e6f`
  - 퍼블릭 DNS: `ec2-15-164-228-217.ap-northeast-2.compute.amazonaws.com`
  - 상세 정보: [인스턴스 정보 문서](../infrastructure/lotto-api-instance-info.md) 참조
- [ ] SSM 파라미터 설정 확인
  - `/lotto/api/DB_HOST`, `/lotto/api/DB_PORT`, `/lotto/api/DB_NAME`
  - `/lotto/api/DB_USERNAME`, `/lotto/api/DB_PASSWORD`
  - 금지 값: `localhost`, `127.0.0.1`
- [ ] 보안 그룹 설정 확인
  - RDS 인바운드: EC2 인스턴스 SG (`sg-095f85d6a1e86a41f`) 허용 (5432)
  - EC2 인바운드: 필요한 IP만 허용 (8080)
- [ ] 네이밍/태깅
  - `Name=dadp-engine-2` (공유 인스턴스), `Environment=prod`

### 배포 단계
- [ ] Docker 이미지 빌드 및 ECR 푸시
- [ ] env-sync 스크립트 실행
- [ ] Docker Compose 재시작
- [ ] 헬스체크 확인
  - `curl http://localhost:8080/actuator/health`
- [ ] 로그 확인
  - `docker-compose logs -f lotto-api`

### 배포 후 검증
- [ ] 애플리케이션 정상 시작 확인
- [ ] 데이터베이스 연결 확인 (JDBC 로그 확인)
- [ ] API 엔드포인트 동작 확인
- [ ] 헬스체크 엔드포인트 응답 확인

## 문제 해결

### 일반적인 문제들

1. **데이터베이스 연결 실패**
   - 보안 그룹에서 포트 5432 허용 확인
   - RDS 엔드포인트 확인
   - 데이터베이스 계정 권한 확인

2. **컨테이너 시작 실패**
   - 로그 확인: `docker-compose logs lotto-api`
   - 환경 변수 확인: `docker-compose config`
   - 이미지 확인: `docker images`

3. **헬스체크 실패**
   - 애플리케이션 포트 확인
   - Actuator 엔드포인트 확인
   - 컨테이너 내부 네트워크 확인

### 로그 확인 방법
```bash
# 로컬
docker-compose logs -f docker-compose.local.yml lotto-api

# AWS (CloudShell 또는 SSM)
docker-compose -f docker-compose.aws.yml logs -f lotto-api

# 최근 100줄만
docker-compose logs --tail=100 lotto-api
```

## 참고 자료

- [빌드 관리 가이드](build-management.md)
- [환경 설정 가이드](environment-configuration.md)
- [프론트엔드 가이드](frontend-guidelines.md)
- [인스턴스 정보 문서](../infrastructure/lotto-api-instance-info.md)

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2026-01-09  
**작성자**: Lotto Guide Platform Development Team
