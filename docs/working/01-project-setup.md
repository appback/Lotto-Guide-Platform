# 01. 프로젝트 초기 설정

## 목표
- Maven 기반 Spring Boot 프로젝트 생성
- Java 17 설정
- appback namespace 적용 (`io.appback.lottoguide`)
- 기본 의존성 설정

## 작업 항목
- [ ] `pom.xml` 생성
  - Java 17
  - Spring Boot 3.x (Java 17 호환 버전)
  - Spring Data JPA
  - PostgreSQL/MySQL Driver
  - Jackson
  - Spring Security (기본)
  - Lombok (선택)
- [ ] 프로젝트 디렉토리 구조 생성
  - `src/main/java/io/appback/lottoguide/`
  - `src/main/resources/`
  - `src/test/java/`
- [ ] `application.yml` 기본 설정
- [ ] 메인 애플리케이션 클래스 생성 (`LottoGuideApplication`)

## 참고
- Maven coordinates: `io.appback:lotto-api`
- Root package: `io.appback.lottoguide`
