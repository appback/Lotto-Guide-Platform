# AWS 배포 체크리스트

## 배포 전 확인 사항

### 1. 로컬 빌드 확인
- [x] 백엔드 빌드 성공 (`mvn clean package -DskipTests -f lotto-api/pom.xml`)
- [ ] 프론트엔드 빌드 성공 (`npm run build` in `client-frontend`)
- [ ] Docker 이미지 빌드 성공

### 2. 데이터베이스 스키마 변경 사항
**새로 추가된 테이블:**
- `mission_templates` - 미션 템플릿 저장 테이블

**⚠️ 중요: 프로덕션 환경에서는 수동 테이블 생성 필요**
- 프로덕션은 `ddl-auto: validate` 설정으로 자동 테이블 생성 안 됨
- 배포 전에 SQL 스크립트로 테이블 생성 필요

**테이블 생성 방법:**
```bash
# EC2에서 데이터베이스 접속
psql -h ${DB_HOST} -U ${DB_USERNAME} -d ${DB_NAME}

# SQL 스크립트 실행
\i docs/deployment/mission_templates_setup.sql
# 또는 직접 SQL 실행
```

**자동 처리:**
- `MissionTemplateInitializer`가 애플리케이션 시작 시 초기 데이터(24개 템플릿) 자동 삽입
- 중복 체크 포함 (이미 데이터가 있으면 스킵)

**확인 필요:**
- 데이터베이스 백업 권장 (배포 전)

### 3. EC2 접속 및 환경 확인

#### SSH 접속
```bash
ssh -i "dadp-prod.pem" ec2-user@ec2-15-164-228-217.ap-northeast-2.compute.amazonaws.com
```

#### 확인 사항
1. **Docker 이미지 위치 확인**
   ```bash
   docker images | grep lotto-api
   ```

2. **환경 변수 파일 확인**
   ```bash
   cat /etc/lotto-api.env
   # 또는
   ls -la /opt/lotto/docker/
   ```

3. **데이터베이스 연결 확인**
   ```bash
   # PostgreSQL 연결 테스트
   psql -h ${DB_HOST} -U ${DB_USERNAME} -d ${DB_NAME} -c "SELECT version();"
   ```

4. **현재 실행 중인 컨테이너 확인**
   ```bash
   docker ps | grep lotto-api
   ```

### 4. 배포 단계

#### Step 1: 로컬에서 Docker 이미지 빌드 및 푸시 (필요시)
```bash
# 로컬에서 실행
python scripts/run-automation.py --service lotto-api --stage docker-rebuild
```

#### Step 2: EC2에서 배포 실행

**방법 1: 직접 SSH 접속 후 실행**
```bash
# SSH 접속
ssh -i "dadp-prod.pem" ec2-user@ec2-15-164-228-217.ap-northeast-2.compute.amazonaws.com

# 작업 디렉토리로 이동
cd /opt/lotto/docker || cd /home/ec2-user/docker

# 환경 변수 동기화 (필요시)
sudo /usr/local/bin/lotto-api-env-sync.sh

# Docker 이미지 pull 또는 로컬 빌드된 이미지 사용
# (이미지가 ECR에 있다면)
# docker pull <ECR_IMAGE_URI>

# 컨테이너 재시작
docker compose -f docker-compose.aws.yml up -d --force-recreate lotto-api

# 로그 확인
docker compose -f docker-compose.aws.yml logs --tail=50 -f lotto-api
```

**방법 2: SSM Session Manager 사용**
```bash
aws ssm start-session --target i-039650bd0704f2e6f --region ap-northeast-2
```

### 5. 배포 후 확인

#### 헬스체크
```bash
# 컨테이너 내부에서
curl -f http://localhost:8080/lotto/actuator/health

# 또는 외부에서 (보안 그룹 설정된 경우)
curl -f http://ec2-15-164-228-217.ap-northeast-2.compute.amazonaws.com:8080/lotto/actuator/health
```

#### 데이터베이스 확인
```bash
# mission_templates 테이블 확인
psql -h ${DB_HOST} -U ${DB_USERNAME} -d ${DB_NAME} -c "SELECT COUNT(*) FROM mission_templates;"
# 예상 결과: 24개

# 테이블 구조 확인
psql -h ${DB_HOST} -U ${DB_USERNAME} -d ${DB_NAME} -c "\d mission_templates"
```

#### 애플리케이션 로그 확인
```bash
docker compose -f docker-compose.aws.yml logs --tail=100 lotto-api | grep -i "mission\|template\|error"
```

### 6. 롤백 계획 (문제 발생 시)

```bash
# 이전 이미지로 롤백
docker compose -f docker-compose.aws.yml down
# 이전 버전의 이미지로 재시작
docker compose -f docker-compose.aws.yml up -d lotto-api
```

## 주요 변경 사항 요약

1. **LLM 제거**: `LlmClientPort` 의존성 제거, 템플릿 기반 시스템으로 교체
2. **새 테이블**: `mission_templates` 테이블 추가
3. **초기 데이터**: 24개 미션 템플릿 자동 삽입 (`MissionTemplateInitializer`)
4. **윈도우 사이즈**: 프론트엔드에서 선택 UI 제거, 고정값(50) 사용

## 주의 사항

- 프로덕션 환경에서 `ddl-auto` 설정이 `validate`인 경우, 수동으로 테이블 생성 필요
- 초기 데이터 삽입은 애플리케이션 시작 시 자동 실행 (중복 체크 포함)
- 데이터베이스 백업 권장 (배포 전)
