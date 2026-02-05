# 10. 테스트

## 목표
- 단위 테스트 작성
- 통합 테스트 작성

## 작업 항목

### 10.1 Unit Tests
- [ ] Domain 모델 테스트
  - Generator 엔진 테스트
  - Mission Policy 테스트
- [ ] UseCase 테스트
  - Mock을 활용한 비즈니스 로직 테스트

### 10.2 Integration Tests
- [ ] API 통합 테스트
  - `@SpringBootTest` 활용
- [ ] Repository 테스트
  - `@DataJpaTest` 활용

### 10.3 Test Data
- [ ] 테스트용 추첨 데이터 준비
- [ ] Fixture 생성

## 참고
- JUnit 5 사용
- Mockito 활용
- TestContainers 고려 (DB 테스트용)
