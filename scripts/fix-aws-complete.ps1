# AWS 배포 완전 수정 스크립트
# 데이터베이스 + 프론트엔드 문제 해결

$ErrorActionPreference = "Stop"

$INSTANCE_IP = "15.164.228.217"
$INSTANCE_USER = "ec2-user"
$INSTANCE_DNS = "ec2-15-164-228-217.ap-northeast-2.compute.amazonaws.com"
$REMOTE_PATH = "/home/ec2-user/lotto/docker"
$KEY_FILE = "dadp-prod.pem"
$ROOT_DIR = Split-Path -Parent $PSScriptRoot

# 키 파일 찾기
$KEY_FILE_PATH = Get-ChildItem -Path "." -Filter $KEY_FILE -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName

if ($null -eq $KEY_FILE_PATH) {
    $KEY_FILE_PATH = Get-ChildItem -Path ".." -Filter $KEY_FILE -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
}

if ($null -eq $KEY_FILE_PATH -or -not (Test-Path $KEY_FILE_PATH)) {
    Write-Host "[ERROR] Key file not found: $KEY_FILE" -ForegroundColor Red
    exit 1
}

Write-Host "[INFO] === AWS 배포 완전 수정 ===" -ForegroundColor Cyan
Write-Host ""

# ===== 1단계: 로컬에서 프론트엔드 빌드 =====
Write-Host "[PHASE 1] 로컬에서 프론트엔드 빌드..." -ForegroundColor Yellow
$frontendDir = Join-Path $ROOT_DIR "client-frontend"

if (-not (Test-Path $frontendDir)) {
    Write-Host "[ERROR] 프론트엔드 디렉토리를 찾을 수 없습니다" -ForegroundColor Red
    exit 1
}

Push-Location $frontendDir
try {
    Write-Host "  npm install..." -ForegroundColor Gray
    & npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] npm install 실패" -ForegroundColor Red
        exit 1
    }

    Write-Host "  npm run build..." -ForegroundColor Gray
    & npm run build
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] npm run build 실패" -ForegroundColor Red
        exit 1
    }

    $indexFile = Join-Path $frontendDir "dist\index.html"
    if (-not (Test-Path $indexFile)) {
        Write-Host "[ERROR] 프론트엔드 빌드 결과물 없음" -ForegroundColor Red
        exit 1
    }

    Write-Host "[SUCCESS] 프론트엔드 빌드 완료" -ForegroundColor Green
} finally {
    Pop-Location
}

Write-Host ""

# ===== 2단계: 로컬에서 백엔드 빌드 (프론트엔드 포함) =====
Write-Host "[PHASE 2] 로컬에서 백엔드 빌드 (프론트엔드 포함)..." -ForegroundColor Yellow
Push-Location $ROOT_DIR
try {
    Write-Host "  mvn clean package..." -ForegroundColor Gray
    & mvn clean package -DskipTests -f lotto-api/pom.xml
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] 백엔드 빌드 실패" -ForegroundColor Red
        exit 1
    }

    # JAR 파일 확인
    $jarFile = Get-ChildItem -Path "$ROOT_DIR\lotto-api\target" -Filter "lotto-api-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    
    if ($null -eq $jarFile) {
        Write-Host "[ERROR] JAR 파일 없음" -ForegroundColor Red
        exit 1
    }

    Write-Host "[SUCCESS] 백엔드 빌드 완료: $($jarFile.Name)" -ForegroundColor Green
} finally {
    Pop-Location
}

Write-Host ""

# ===== 3단계: AWS에서 데이터베이스 확인 및 생성 =====
Write-Host "[PHASE 3] AWS에서 데이터베이스 확인..." -ForegroundColor Yellow

# PostgreSQL 컨테이너 재시작 (데이터베이스 자동 생성)
Write-Host "  PostgreSQL 컨테이너 재시작 중..." -ForegroundColor Gray
$cmdDb = "cd $REMOTE_PATH && docker-compose -f docker-compose.aws.yml restart postgres"
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdDb

Write-Host "  데이터베이스 생성 대기 중 (10초)..." -ForegroundColor Gray
Start-Sleep -Seconds 10

# 데이터베이스 확인
$cmdCheck = "docker exec -i lotto-postgres psql -U postgres -c 'SELECT 1 FROM pg_database WHERE datname=''lottoguide'';' -t 2>&1 | grep -q 1 && echo 'EXISTS' || echo 'NOT_EXISTS'"
$dbExists = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdCheck

if ($dbExists -match "NOT_EXISTS") {
    Write-Host "  데이터베이스 수동 생성 중..." -ForegroundColor Gray
    $cmdCreate = "docker exec -i lotto-postgres psql -U postgres -c 'CREATE DATABASE lottoguide;'"
    & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdCreate
    Write-Host "[SUCCESS] 데이터베이스 생성 완료" -ForegroundColor Green
} else {
    Write-Host "[SUCCESS] 데이터베이스 존재 확인" -ForegroundColor Green
}

Write-Host ""

# ===== 4단계: JAR 파일 업로드 및 배포 =====
Write-Host "[PHASE 4] JAR 파일 업로드 및 배포..." -ForegroundColor Yellow

# JAR 파일 복사
$jarFile = Get-ChildItem -Path "$ROOT_DIR\lotto-api\target" -Filter "lotto-api-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$localJar = Join-Path $PSScriptRoot "lotto-api-latest.jar"
Copy-Item $jarFile.FullName -Destination $localJar -Force

Write-Host "  JAR 파일 업로드 중..." -ForegroundColor Gray
& scp -i $KEY_FILE_PATH $localJar "${INSTANCE_USER}@${INSTANCE_DNS}:${REMOTE_PATH}/lotto-api-latest.jar"

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] JAR 파일 업로드 실패" -ForegroundColor Red
    exit 1
}

# JAR 파일을 target 디렉토리로 이동
$cmdJar = "mkdir -p $REMOTE_PATH/target && cp $REMOTE_PATH/lotto-api-latest.jar $REMOTE_PATH/target/lotto-api-1.0.0-SNAPSHOT.jar"
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdJar

# Docker 이미지 재빌드
Write-Host "  Docker 이미지 재빌드 중..." -ForegroundColor Gray
$cmdBuild = "cd $REMOTE_PATH && docker build -t lotto-api:latest -f Dockerfile ."
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdBuild

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Docker 이미지 빌드 실패" -ForegroundColor Red
    exit 1
}

# 컨테이너 재시작
Write-Host "  컨테이너 재시작 중..." -ForegroundColor Gray
$cmdRestart = "cd $REMOTE_PATH && docker-compose -f docker-compose.aws.yml up -d --force-recreate lotto-api"
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdRestart

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] 컨테이너 재시작 실패" -ForegroundColor Red
    exit 1
}

Write-Host "[SUCCESS] 배포 완료" -ForegroundColor Green
Write-Host ""

# ===== 5단계: 헬스 체크 =====
Write-Host "[PHASE 5] 헬스 체크 대기 (30초)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

Write-Host "  헬스 체크 실행 중..." -ForegroundColor Gray
$cmdHealth = "curl -s http://localhost:8083/lotto/actuator/health || echo 'HEALTH_CHECK_FAILED'"
$healthResult = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdHealth

if ($healthResult -match "UP|status.*UP") {
    Write-Host "[SUCCESS] 헬스 체크 통과" -ForegroundColor Green
} else {
    Write-Host "[WARNING] 헬스 체크 실패 또는 확인 필요" -ForegroundColor Yellow
    Write-Host $healthResult -ForegroundColor Gray
}

Write-Host ""
Write-Host "[INFO] === 수정 완료 ===" -ForegroundColor Cyan
Write-Host "[INFO] 접속 URL: http://$INSTANCE_IP:8083/lotto/" -ForegroundColor White
Write-Host "[INFO] 로그 확인: ssh -i $KEY_FILE ${INSTANCE_USER}@${INSTANCE_DNS} 'cd $REMOTE_PATH && docker-compose -f docker-compose.aws.yml logs -f lotto-api'" -ForegroundColor Gray
