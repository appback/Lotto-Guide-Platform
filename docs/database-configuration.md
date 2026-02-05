# 데이터베이스 설정 문서

> **프로젝트**: Lotto Guide Platform  
> **데이터베이스 이름**: `lottoguide` (고정)  
> **최종 업데이트**: 2026-01-17

## 📋 개요

Lotto Guide Platform은 **PostgreSQL** 데이터베이스를 사용하며, 데이터베이스 이름은 **`lottoguide`**로 고정되어 있습니다.

## 🔧 설정 파일

### 1. Docker Compose 설정

#### AWS 환경 (`docker/docker-compose.aws.yml`)
```yaml
services:
  postgres:
    environment:
      POSTGRES_DB: lottoguide  # 데이터베이스 이름
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d lottoguide"]
  
  lotto-api:
    environment:
      - DB_NAME=lottoguide
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/lottoguide
```

#### 로컬 환경 (`docker/docker-compose.local.yml`)
```yaml
services:
  postgres:
    environment:
      POSTGRES_DB: lottoguide  # 데이터베이스 이름
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d lottoguide"]
  
  lotto-api:
    environment:
      - DB_NAME=lottoguide
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/lottoguide
```

### 2. Spring Boot 설정 (`lotto-api/src/main/resources/application.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lottoguide
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
```

## 📊 데이터베이스 구조

### 주요 테이블
- `lotto_draw` - 로또 회차 정보
- `lotto_number_metrics` - 번호 통계
- `lotto_data_state` - 데이터 상태
- `mission_templates` - 미션 템플릿
- `mission_log` - 미션 로그
- `generated_set` - 생성된 번호 세트
- `generated_numbers` - 생성된 번호
- `strategy_description` - 전략 설명
- `system_options` - 시스템 옵션
- `destiny_limit_message` - 운명 제한 메시지
- `ai_loading_message` - AI 로딩 메시지
- `mission_phrase_a`, `mission_phrase_b`, `mission_phrase_c` - 미션 문구

## ⚠️ 중요 사항

### 1. 데이터베이스 이름 변경 금지
- **데이터베이스 이름은 `lottoguide`로 고정**
- 다른 이름으로 변경 시 모든 설정 파일을 동시에 수정해야 함
- 변경 전 반드시 문서화 필요

### 2. 초기화 스크립트
- 위치: `docker/init-scripts/01-init-mission-templates.sql`
- 실행 시점: PostgreSQL 컨테이너가 **처음 시작될 때만** 실행
- 기존 볼륨이 있으면 실행되지 않음

### 3. 마이그레이션
- 기존 데이터를 `lottoguide`로 마이그레이션하는 경우:
  ```bash
  # postgres 데이터베이스에서 lottoguide로 마이그레이션
  docker exec lotto-postgres pg_dump -U postgres -d postgres > /tmp/postgres_dump.sql
  docker exec -i lotto-postgres psql -U postgres -d lottoguide < /tmp/postgres_dump.sql
  ```

## 🔎 비밀번호가 왜 달라졌는지 확인 (DB 유지)

PostgreSQL 공식 이미지는 **데이터 디렉터리가 비어 있을 때만** `POSTGRES_PASSWORD`로 비밀번호를 설정합니다. 한 번 초기화된 볼륨은 이후 compose에서 `POSTGRES_PASSWORD`를 바꿔도 **기존 비밀번호가 유지**됩니다.

- 가능한 원인: 예전에 다른 `POSTGRES_PASSWORD`로 최초 실행, 다른 compose/스크립트로 최초 실행, EC2에 `.env` 등으로 오버라이드된 경우.

### EC2에서 확인 절차 (데이터 삭제 없음)

SSH 접속 후 `/home/ec2-user/lotto/docker`에서 실행.

**1) EC2에 compose를 덮어쓰는 .env 있는지**
```bash
cd /home/ec2-user/lotto/docker
cat .env 2>/dev/null || echo "(.env 없음)"
```

**2) 지금 postgres 컨테이너에 들어가는 환경 변수**
```bash
docker inspect lotto-postgres --format '{{range .Config.Env}}{{println .}}{{end}}' | grep -E 'POSTGRES|PASSWORD'
```

**3) lotto-api가 쓰는 DB 비밀번호**
```bash
docker exec lotto-api env | grep -E 'DB_PASSWORD|SPRING_DATASOURCE_PASSWORD'
```

**4) 비밀번호 "postgres"로 접속되는지 테스트**
```bash
PGPASSWORD=postgres psql -h localhost -p 5432 -U postgres -d lottoguide -c 'SELECT 1' 2>&1
```
- `SELECT 1` 한 줄 나오면 → DB는 비밀번호 `postgres`로 열림. 앱 쪽 환경만 점검하면 됨.
- `password authentication failed` 나오면 → 볼륨이 **다른 비밀번호로** 초기화된 상태. 그때는 (i) 예전에 썼을 비밀번호를 찾아서 compose/앱에 맞추거나, (ii) 데이터를 버려도 되면 `down -v` 후 재기동.

**5) postgres 컨테이너 안에서는 (비밀번호 없이) 접속 가능한지**
```bash
docker exec lotto-postgres psql -U postgres -d lottoguide -c 'SELECT 1'
```
- `database "lottoguide" does not exist` 나오면 **DB가 없는 상태**. 아래 "DB만 생성" 절차로 해결.

위 1~5 결과로 원인(env 오버라이드 / 볼륨 초기화 시 비밀번호 / DB 없음)을 구분할 수 있습니다.

### DB만 생성 (데이터 유지)

`lottoguide` DB가 없을 때 (비밀번호는 이미 맞는 경우):

```bash
cd /home/ec2-user/lotto/docker
docker exec lotto-postgres psql -U postgres -d postgres -c "CREATE DATABASE lottoguide;"
docker-compose -f docker-compose.aws.yml restart lotto-api
```

그래도 `password authentication failed`가 나오면, DB 비밀번호를 앱과 맞춤 (데이터 유지):

```bash
docker exec lotto-postgres psql -U postgres -d postgres -c "ALTER USER postgres PASSWORD 'postgres';"
docker-compose -f docker-compose.aws.yml restart lotto-api
```

---

## 🚨 AWS에서 "password authentication failed for user postgres" 해결

에러 메시지: `FATAL: password authentication failed for user "postgres"`

### 원인
- 애플리케이션이 사용하는 **비밀번호**와 PostgreSQL에 설정된 비밀번호가 다름.
- 또는 **볼륨이 예전에 다른 비밀번호로 최초 초기화**되어 있어서, 지금 compose의 `POSTGRES_PASSWORD`와 DB에 저장된 비밀번호가 다름.

### 해결 방법

#### 1) Docker Compose로 실행 중인 경우
- `docker-compose.aws.yml`에서는 `POSTGRES_PASSWORD=postgres`, `DB_PASSWORD=postgres`로 맞춰져 있음.
- **이미 생성된 PostgreSQL 볼륨**이 다른 비밀번호로 초기화된 적이 있으면, 지금 설정한 `postgres`와 불일치할 수 있음.
  - **방법 A**: 실제 DB 비밀번호를 알아서 `docker-compose.aws.yml`의 `DB_PASSWORD`, `SPRING_DATASOURCE_PASSWORD`, postgres 서비스의 `POSTGRES_PASSWORD`를 그 비밀번호로 통일.
  - **방법 B**: 데이터를 잃어도 된다면 볼륨을 제거한 뒤 다시 올려서 새로 초기화.
    ```bash
    docker compose -f docker-compose.aws.yml down -v
    docker compose -f docker-compose.aws.yml up -d
    ```

#### 2) JAR 직접 실행 또는 RDS 사용 시
- 애플리케이션에 **실제 DB 비밀번호**가 전달되도록 환경 변수 설정.
- 다음 중 하나를 반드시 맞춤:
  - `DB_PASSWORD` = PostgreSQL의 `postgres` 사용자 비밀번호  
  - 또는 `SPRING_DATASOURCE_PASSWORD` = 위와 동일한 비밀번호
- AWS SSM Parameter Store를 쓰는 경우, `/lotto/api/DB_PASSWORD` 값이 RDS(또는 해당 PostgreSQL) 마스터 비밀번호와 동일한지 확인.
- 연결 주소도 필요하면:
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://<호스트>:<포트>/lottoguide`  
  - 또는 `DB_HOST`, `DB_PORT`, `DB_NAME` 설정 (기본값으로 URL 자동 조합).

#### 3) 환경 변수 적용 여부 확인
- EC2/컨테이너에서 실제로 적용된 값 확인:
  ```bash
  # 컨테이너 안에서
  echo $DB_USERNAME $DB_PASSWORD
  # 또는
  env | grep -E 'DB_|SPRING_DATASOURCE'
  ```
- `DB_PASSWORD`가 비어 있거나 잘못된 값이면, 배포 스크립트(예: `lotto-api-env-sync.sh`) 및 SSM/환경 변수 설정을 다시 점검.

## 🔍 설정 확인 방법

### 데이터베이스 목록 확인
```bash
docker exec lotto-postgres psql -U postgres -l
```

### 테이블 목록 확인
```bash
docker exec lotto-postgres psql -U postgres -d lottoguide -c '\dt'
```

### 데이터 개수 확인
```bash
docker exec lotto-postgres psql -U postgres -d lottoguide -t -c 'SELECT COUNT(*) FROM lotto_draw;'
```

## 📝 관련 문서

- `docs/guidelines/environment-configuration.md` - 환경 설정 가이드
- `docs/guidelines/deployment-guidelines.md` - 배포 가이드
- `docs/deployment/aws-deployment-checklist.md` - AWS 배포 체크리스트

## 🔄 변경 이력

- **2026-01-17**: 데이터베이스 이름 `lottoguide`로 통일 및 문서화
- **이전**: 일부 환경에서 `postgres` 기본 데이터베이스 사용 (마이그레이션 완료)

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2026-01-17  
**작성자**: Lotto Guide Platform Development Team
