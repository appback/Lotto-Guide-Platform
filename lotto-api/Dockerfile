# Lotto Guide Platform API Dockerfile
FROM eclipse-temurin:17-jdk-alpine

# wget 설치 (헬스체크용)
RUN apk add --no-cache wget

# 작업 디렉토리 설정
WORKDIR /app

# JAR 파일 복사 (와일드카드 사용으로 버전 독립적)
COPY target/lotto-api-*.jar app.jar

# 포트 노출
EXPOSE 8080

# 헬스 체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
