# Lotto Guide API

appback Lotto Guide Platform의 백엔드 API 서버입니다.

## 기술 스택

- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- PostgreSQL / MySQL
- Maven

## 프로젝트 구조

```
io.appback.lottoguide
 ├─ api              # REST API 레이어
 ├─ application       # UseCase 및 Port
 ├─ domain            # 도메인 모델 및 비즈니스 로직
 ├─ infra             # 인프라스트럭처 (Persistence, LLM, Scheduler, Security)
 └─ config          # 설정 클래스
```

## 실행 방법

1. 데이터베이스 설정
   - PostgreSQL 또는 MySQL 설치 및 데이터베이스 생성
   - `application.yml`에서 연결 정보 설정

2. 애플리케이션 실행
   ```bash
   mvn spring-boot:run
   ```

3. 프로파일 설정
   - 개발: `spring.profiles.active=dev` (기본값)
   - 프로덕션: `spring.profiles.active=prod`

## API 엔드포인트

- `POST /api/v1/generate` - 번호 생성
- `POST /api/v1/mission` - 미션 생성
- `GET /api/v1/history` - 히스토리 조회 (Member only)

## 개발 가이드

자세한 작업 항목은 `working/` 디렉토리의 마크다운 파일들을 참고하세요.
