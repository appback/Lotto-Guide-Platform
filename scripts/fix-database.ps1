# PostgreSQL 데이터베이스 생성 및 수정 스크립트
# EC2 서버에서 실행하여 데이터베이스 문제 해결

$ErrorActionPreference = "Stop"

$INSTANCE_IP = "15.164.228.217"
$INSTANCE_USER = "ec2-user"
$INSTANCE_DNS = "ec2-15-164-228-217.ap-northeast-2.compute.amazonaws.com"
$REMOTE_PATH = "/home/ec2-user/lotto/docker"
$KEY_FILE = "dadp-prod.pem"

# 키 파일 찾기
$KEY_FILE_PATH = Get-ChildItem -Path "." -Filter $KEY_FILE -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName

if ($null -eq $KEY_FILE_PATH) {
    $KEY_FILE_PATH = Get-ChildItem -Path ".." -Filter $KEY_FILE -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
}

if ($null -eq $KEY_FILE_PATH -or -not (Test-Path $KEY_FILE_PATH)) {
    Write-Host "[ERROR] Key file not found: $KEY_FILE" -ForegroundColor Red
    exit 1
}

Write-Host "[INFO] Key file: $KEY_FILE_PATH" -ForegroundColor Cyan
Write-Host "[INFO] Fixing PostgreSQL database..." -ForegroundColor Green

# 1. PostgreSQL 컨테이너 상태 확인
Write-Host "[STEP 1] Checking PostgreSQL container status..." -ForegroundColor Yellow
$checkCmd = "docker ps -a | grep lotto-postgres || echo 'NOT_FOUND'"
$containerStatus = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $checkCmd

if ($containerStatus -match "NOT_FOUND" -or $containerStatus -eq "") {
    Write-Host "[WARNING] PostgreSQL container not found. Starting Docker Compose..." -ForegroundColor Yellow
    $startCmd = "cd $REMOTE_PATH && docker-compose -f docker-compose.aws.yml up -d postgres"
    & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $startCmd
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Failed to start PostgreSQL container" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "[INFO] Waiting for PostgreSQL to be ready (10 seconds)..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10
} else {
    Write-Host "[SUCCESS] PostgreSQL container found" -ForegroundColor Green
    Write-Host "  $containerStatus" -ForegroundColor Gray
}

# 2. 데이터베이스 존재 여부 확인
Write-Host "[STEP 2] Checking if database 'lottoguide' exists..." -ForegroundColor Yellow
$checkDbCmd = "docker exec lotto-postgres psql -U postgres -lqt | cut -d \| -f 1 | grep -qw lottoguide && echo 'EXISTS' || echo 'NOT_EXISTS'"
$dbExists = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $checkDbCmd

if ($dbExists -match "NOT_EXISTS") {
    Write-Host "[INFO] Database 'lottoguide' does not exist. Creating..." -ForegroundColor Yellow
    
    # 데이터베이스 생성
    $createDbCmd = "docker exec lotto-postgres psql -U postgres -c 'CREATE DATABASE lottoguide;'"
    & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $createDbCmd
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Failed to create database" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "[SUCCESS] Database 'lottoguide' created" -ForegroundColor Green
} else {
    Write-Host "[SUCCESS] Database 'lottoguide' already exists" -ForegroundColor Green
}

# 3. 데이터베이스 연결 테스트
Write-Host "[STEP 3] Testing database connection..." -ForegroundColor Yellow
$testCmd = "docker exec lotto-postgres psql -U postgres -d lottoguide -c 'SELECT version();' | head -n 3"
$testResult = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $testCmd

if ($LASTEXITCODE -eq 0) {
    Write-Host "[SUCCESS] Database connection successful" -ForegroundColor Green
    Write-Host "  $testResult" -ForegroundColor Gray
} else {
    Write-Host "[ERROR] Database connection failed" -ForegroundColor Red
    exit 1
}

# 4. lotto-api 컨테이너 재시작
Write-Host "[STEP 4] Restarting lotto-api container..." -ForegroundColor Yellow
$restartCmd = "cd $REMOTE_PATH && docker-compose -f docker-compose.aws.yml restart lotto-api"
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $restartCmd

if ($LASTEXITCODE -eq 0) {
    Write-Host "[SUCCESS] lotto-api container restarted" -ForegroundColor Green
} else {
    Write-Host "[WARNING] Failed to restart lotto-api (may not be running)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[SUCCESS] Database fix completed!" -ForegroundColor Green
Write-Host "[INFO] Checking lotto-api logs..." -ForegroundColor Cyan
$logCmd = "cd $REMOTE_PATH && docker-compose -f docker-compose.aws.yml logs --tail 30 lotto-api"
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $logCmd
