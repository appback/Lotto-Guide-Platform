# 코딩 표준 가이드라인

> **프로젝트**: Lotto Guide Platform  
> **버전**: 1.0.0  
> **최종 업데이트**: 2026-01-09  
> **작성자**: Lotto Guide Platform Development Team

## 개요

Lotto Guide Platform 프로젝트의 모든 코드 작성에 적용되는 표준을 정의합니다.

## Java 코딩 표준

### 1. 명명 규칙
- **클래스명**: PascalCase (예: `UserService`, `ApiResponse`)
- **메서드명**: camelCase (예: `getUserById`, `validateToken`)
- **변수명**: camelCase (예: `userName`, `accessToken`)
- **상수명**: UPPER_SNAKE_CASE (예: `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT`)
- **패키지명**: 소문자 (예: `io.appback.lottoguide`)
- **boolean 반환 메서드**: `is` 접두사 필수 (예: `isValid()`, `isEnabled()`)
  - ❌ 금지: `getValid()`, `getEnabled()` 등 `get` 접두사 사용
  - ✅ 권장: `isValid()`, `isEnabled()` 등 `is` 접두사 사용

### 2. 파일 구조 (Clean Architecture)
```
src/main/java/
├── io/appback/lottoguide/
│   ├── api/              # REST API 레이어
│   ├── application/      # UseCase 및 Port
│   ├── domain/           # 도메인 모델 및 비즈니스 로직
│   ├── infra/            # 인프라스트럭처 (Persistence, LLM, Scheduler, Security)
│   └── config/           # 설정 클래스
```

### 3. 클래스 구조
```java
// 1. 패키지 선언
package io.appback.lottoguide.service;

// 2. import 문 (표준 라이브러리 → 서드파티 → 프로젝트 내부)
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import io.appback.lottoguide.domain.User;
import io.appback.lottoguide.api.dto.UserDto;

// 3. 클래스 선언
@Service
public class UserService {
    
    // 4. 필드 (private final 우선)
    private final UserRepository userRepository;
    
    // 5. 생성자
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    // 6. public 메서드
    public UserDto createUser(CreateUserRequest request) {
        // 구현
    }
    
    // 7. private 메서드
    private void validateUser(User user) {
        // 구현
    }
}
```

### 4. 메서드 작성 규칙
- **단일 책임 원칙**: 하나의 메서드는 하나의 기능만 수행
- **메서드 길이**: 20줄 이하 권장
- **매개변수**: 3개 이하 권장
- **반환값**: 명확한 타입 지정

### 5. 예외 처리

**핵심 원칙**:
- 예측 가능한 문제는 Exception을 발생시키지 않고 정상 처리
- 예측 불가능한 문제만 Exception 발생 및 로그 출력 (`ERROR` 레벨)
- Exception 로그는 어떤 문제인지 모를 때만 출력

**기본 예시**:
```java
// 체크 예외는 명시적으로 처리
try {
    return userRepository.findById(id);
} catch (DataAccessException e) {
    // 예측 불가능한 문제: 원인을 알 수 없음
    log.error("❌ 데이터베이스 연결 실패: userId={}", id, e);
    throw new RuntimeException("서버 내부 오류가 발생했습니다");
}

// 언체크 예외는 상위로 전파
public UserDto getUserById(Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + id));
    return UserDto.from(user);
}
```

### 6. 로깅 표준
```java
// 로그 레벨 사용
logger.debug("디버그 정보: {}", value);
logger.info("정보 로그: 사용자 {} 로그인", username);
logger.warn("경고 로그: 사용자 {} 권한 부족", username);
logger.error("오류 로그: 사용자 {} 조회 실패", id, exception);

// 구조화된 로깅
logger.info("API 호출: {} {} - 사용자: {}", method, path, username);
logger.error("인증 실패: 사용자={}, 원인={}", username, cause);
```

## Spring Boot 표준

### 1. 어노테이션 사용
- **@RestController**: REST API 컨트롤러
- **@Service**: 비즈니스 로직 서비스
- **@Repository**: 데이터 접근 계층
- **@Component**: 일반 컴포넌트
- **@Configuration**: 설정 클래스

### 2. 의존성 주입
```java
// 생성자 주입 권장
@Service
public class UserService {
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// 필드 주입 금지
@Service
public class UserService {
    @Autowired  // 금지
    private UserRepository userRepository;
}
```

### 3. API 응답 표준
```java
// 성공 응답
return ResponseEntity.ok(data);
return ResponseEntity.created(uri).body(newResource);
return ResponseEntity.noContent().build();

// 오류 응답
return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationErrors);
```

## Clean Architecture 준수

### 1. 레이어 간 의존성 규칙
- **domain**: 다른 레이어에 의존하지 않음
- **application**: domain만 의존
- **infra**: application의 Port 인터페이스를 구현
- **api**: application의 UseCase를 호출

### 2. 도메인 모델 예시
```java
// domain/Number.java
public class Number {
    private final List<Integer> numbers;
    
    public Number(List<Integer> numbers) {
        this.numbers = validate(numbers);
    }
    
    private List<Integer> validate(List<Integer> numbers) {
        // 도메인 검증 로직
    }
}
```

### 3. UseCase 예시
```java
// application/GenerateNumberUseCase.java
public class GenerateNumberUseCase {
    private final NumberGenerator numberGenerator;
    
    public Number execute(GenerateNumberRequest request) {
        return numberGenerator.generate(request);
    }
}
```

## 데이터베이스 표준

### 1. 엔티티 설계
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### 2. Repository 패턴
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByUsernameContaining(String username);
}
```

### 3. 트랜잭션 관리
```java
@Service
@Transactional(readOnly = true)
public class UserService {
    
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        // 쓰기 작업
    }
    
    public UserDto getUserById(Long id) {
        // 읽기 작업
    }
}
```

## 테스트 표준

### 1. 단위 테스트
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    @DisplayName("사용자 생성 성공")
    void createUser_Success() {
        // Given
        CreateUserRequest request = new CreateUserRequest("testuser", "test@example.com");
        User savedUser = new User(1L, "testuser", "test@example.com");
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // When
        UserDto result = userService.createUser(request);
        
        // Then
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).save(any(User.class));
    }
}
```

### 2. 통합 테스트
```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserServiceIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    @DisplayName("사용자 생성 및 조회 통합 테스트")
    void createAndRetrieveUser() {
        // Given
        CreateUserRequest request = new CreateUserRequest("testuser", "test@example.com");
        
        // When
        UserDto createdUser = userService.createUser(request);
        UserDto retrievedUser = userService.getUserById(createdUser.getId());
        
        // Then
        assertThat(retrievedUser.getUsername()).isEqualTo("testuser");
    }
}
```

## 보안 표준

### 1. 입력값 검증
```java
// DTO 검증
public class CreateUserRequest {
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 3, max = 50, message = "사용자명은 3-50자 사이여야 합니다")
    private String username;
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;
}
```

## 성능 최적화

### 1. 데이터베이스 최적화
- **인덱스 사용**: 자주 조회되는 컬럼에 인덱스 생성
- **N+1 문제 해결**: fetch join 또는 @EntityGraph 사용
- **페이징 처리**: 대용량 데이터 조회 시 페이징 적용

### 2. 캐싱 전략
```java
// Redis 캐싱
@Cacheable(value = "users", key = "#id")
public UserDto getUserById(Long id) {
    return userRepository.findById(id)
        .map(UserDto::from)
        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다"));
}
```

---

**버전**: 1.0.0  
**최종 업데이트**: 2026-01-09  
**작성자**: Lotto Guide Platform Development Team
