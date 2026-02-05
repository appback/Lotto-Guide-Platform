# Lotto Guide Platform - 작업 가이드

이 디렉토리는 아키텍처 문서를 기반으로 한 작업 항목들을 포함합니다.

## 📊 진행 상태

**현재 진행 상황은 [PROGRESS.md](PROGRESS.md)를 참조하세요.**

## 작업 항목 목록

1. **[01-project-setup.md](01-project-setup.md)** - 프로젝트 초기 설정
2. **[02-database-model.md](02-database-model.md)** - 데이터베이스 모델 및 엔티티
3. **[03-domain-model.md](03-domain-model.md)** - Domain 모델 생성
4. **[04-number-generation.md](04-number-generation.md)** - Number Generation 엔진 구현
5. **[05-mission-llm.md](05-mission-llm.md)** - Mission LLM 통합
6. **[06-api-layer.md](06-api-layer.md)** - API 레이어 구현
7. **[07-application-layer.md](07-application-layer.md)** - Application 레이어 구현
8. **[08-infrastructure-layer.md](08-infrastructure-layer.md)** - Infrastructure 레이어 구현
9. **[09-config-layer.md](09-config-layer.md)** - Config 레이어 구현
10. **[10-testing.md](10-testing.md)** - 테스트

## 프로젝트 구조

```
working/
├── lotto-api/              # Spring Boot 프로젝트
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/io/appback/lottoguide/
│   │   │   └── resources/
│   │   └── test/
│   └── README.md
└── [작업 항목별 마크다운 파일들]
```

## 시작하기

1. 각 작업 항목의 마크다운 파일을 순서대로 확인하세요.
2. `lotto-api/` 디렉토리에서 Spring Boot 프로젝트를 개발하세요.
3. 아키텍처 문서(`../docs/docs_prototype_mvp_architecture_appback_v1.2.md`)를 참고하세요.

## 기술 스택

- Java 17
- Spring Boot 3.3.5
- Maven
- PostgreSQL
- Spring Data JPA
- Hibernate (ddl-auto: update)

## 현재 상태 요약

- ✅ **프로젝트 초기 설정 완료**: pom.xml, application.yml, Docker 설정
- ✅ **인프라 설정 완료**: Docker Compose, 자동화 스크립트
- 🚧 **구현 대기 중**: Domain 모델, Entity, UseCase, API 등
