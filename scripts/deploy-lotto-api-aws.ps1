# Lotto API AWS 배포 스크립트
# EC2 서버에 lotto-api를 재빌드하고 배포

$ErrorActionPreference = "Stop"

$INSTANCE_IP = "15.164.228.217"
$INSTANCE_USER = "ec2-user"
$INSTANCE_DNS = "ec2-15-164-228-217.ap-northeast-2.compute.amazonaws.com"
$REMOTE_PATH = "/home/ec2-user/lotto/docker"
$KEY_FILE = "dadp-prod.pem"

# 키 파일 찾기 (현재 폴더 우선)
$KEY_FILE_PATH = Get-ChildItem -Path "." -Filter $KEY_FILE -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName

if ($null -eq $KEY_FILE_PATH) {
    # 상위 폴더에서 찾기
    $KEY_FILE_PATH = Get-ChildItem -Path ".." -Filter $KEY_FILE -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
}

if ($null -eq $KEY_FILE_PATH -or -not (Test-Path $KEY_FILE_PATH)) {
    Write-Host "[ERROR] Key file not found: $KEY_FILE" -ForegroundColor Red
    Write-Host "  Key file must be in current directory or parent directory." -ForegroundColor Yellow
    exit 1
}

Write-Host "[INFO] Key file: $KEY_FILE_PATH" -ForegroundColor Cyan

Write-Host "[INFO] Starting Lotto API deployment..." -ForegroundColor Green

$ROOT_DIR = Split-Path -Parent $PSScriptRoot

# JAR file search
Write-Host "[STEP 1] Searching for JAR file..." -ForegroundColor Yellow
$LOTTO_JAR = Get-ChildItem -Path "$ROOT_DIR\lotto-api\target" -Filter "lotto-api-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1

if ($null -eq $LOTTO_JAR -or -not (Test-Path $LOTTO_JAR.FullName)) {
    Write-Host "[ERROR] [STEP 1] Lotto API JAR file not found: $ROOT_DIR\lotto-api\target" -ForegroundColor Red
    Write-Host "  Please build JAR file first: mvn clean package -DskipTests -f lotto-api/pom.xml" -ForegroundColor Yellow
    exit 1
}

Write-Host "[SUCCESS] [STEP 1] JAR file found: $($LOTTO_JAR.Name)" -ForegroundColor Green

# Prepare JAR file (temporary location)
Write-Host "[STEP 2] Preparing JAR file..." -ForegroundColor Yellow
$LOCAL_JAR = Join-Path $PSScriptRoot "lotto-api-latest.jar"
Copy-Item $LOTTO_JAR.FullName -Destination $LOCAL_JAR -Force
Write-Host "[SUCCESS] [STEP 2] JAR file prepared" -ForegroundColor Green

# Create remote directory
Write-Host "[STEP 3] Creating remote directory..." -ForegroundColor Yellow
$cmdMkdir = "mkdir -p $REMOTE_PATH"
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdMkdir

if ($LASTEXITCODE -ne 0) {
    Write-Host "[WARNING] [STEP 3] Remote directory creation failed (continuing)" -ForegroundColor Yellow
} else {
    Write-Host "[SUCCESS] [STEP 3] Remote directory created" -ForegroundColor Green
}

# Copy JAR file to server
Write-Host "[STEP 4] Copying JAR file to server..." -ForegroundColor Yellow
& scp -i $KEY_FILE_PATH $LOCAL_JAR "${INSTANCE_USER}@${INSTANCE_DNS}:${REMOTE_PATH}/lotto-api-latest.jar"

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] [STEP 4] JAR file copy failed" -ForegroundColor Red
    exit 1
}

Write-Host "[SUCCESS] [STEP 4] JAR file copied" -ForegroundColor Green

# Copy Dockerfile
Write-Host "[STEP 5] Copying Dockerfile..." -ForegroundColor Yellow
$DOCKERFILE = Join-Path $ROOT_DIR "lotto-api\Dockerfile"

if (-not (Test-Path $DOCKERFILE)) {
    Write-Host "[ERROR] [STEP 5] Dockerfile not found: $DOCKERFILE" -ForegroundColor Red
    exit 1
}

& scp -i $KEY_FILE_PATH $DOCKERFILE "${INSTANCE_USER}@${INSTANCE_DNS}:${REMOTE_PATH}/Dockerfile"

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] [STEP 5] Dockerfile copy failed" -ForegroundColor Red
    exit 1
}

Write-Host "[SUCCESS] [STEP 5] Dockerfile copied" -ForegroundColor Green

# Copy docker-compose file
Write-Host "[STEP 6] Copying docker-compose file..." -ForegroundColor Yellow
$COMPOSE_FILE = Join-Path $ROOT_DIR "docker\docker-compose.aws.yml"

if (-not (Test-Path $COMPOSE_FILE)) {
    Write-Host "[ERROR] [STEP 6] docker-compose file not found: $COMPOSE_FILE" -ForegroundColor Red
    exit 1
}

& scp -i $KEY_FILE_PATH $COMPOSE_FILE "${INSTANCE_USER}@${INSTANCE_DNS}:${REMOTE_PATH}/docker-compose.aws.yml"

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] [STEP 6] docker-compose file copy failed" -ForegroundColor Red
    exit 1
}

Write-Host "[SUCCESS] [STEP 6] docker-compose file copied" -ForegroundColor Green

# Copy initialization scripts
Write-Host "[STEP 7] Copying initialization scripts..." -ForegroundColor Yellow
$INIT_SCRIPTS_DIR = Join-Path $ROOT_DIR "docker\init-scripts"

if (-not (Test-Path $INIT_SCRIPTS_DIR)) {
    Write-Host "[WARNING] [STEP 7] Initialization scripts directory not found: $INIT_SCRIPTS_DIR" -ForegroundColor Yellow
    Write-Host "  Skipping..." -ForegroundColor Gray
} else {
    # Copy entire directory
    $cmdInit = "mkdir -p $REMOTE_PATH/init-scripts"
    & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdInit
    
    & scp -i $KEY_FILE_PATH -r "$INIT_SCRIPTS_DIR/*" "${INSTANCE_USER}@${INSTANCE_DNS}:${REMOTE_PATH}/init-scripts/"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[WARNING] [STEP 7] Initialization scripts copy failed (continuing)" -ForegroundColor Yellow
    } else {
        Write-Host "[SUCCESS] [STEP 7] Initialization scripts copied" -ForegroundColor Green
    }
}

# Check database table (optional)
Write-Host "[STEP 8] Checking database table..." -ForegroundColor Yellow
$sqlCheck = "docker exec lotto-postgres psql -U postgres -d lottoguide -c '\d mission_templates' 2>&1 | grep -q 'mission_templates' && echo 'EXISTS' || echo 'NOT_EXISTS'"
$tableExists = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" "cd $REMOTE_PATH && $sqlCheck"

if ($tableExists -notmatch "EXISTS") {
    Write-Host "[WARNING] [STEP 8] mission_templates table not found." -ForegroundColor Yellow
    Write-Host "  Table will be created automatically on first PostgreSQL container start." -ForegroundColor Gray
} else {
    Write-Host "[SUCCESS] [STEP 8] mission_templates table exists" -ForegroundColor Green
}

# Restart Docker containers on server
Write-Host "[STEP 9] Restarting Docker containers on server..." -ForegroundColor Yellow

Write-Host "  Stopping and removing existing containers..." -ForegroundColor Gray
$cmd1 = "docker stop lotto-api lotto-postgres || true"
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd1
$cmd1b = "docker rm lotto-api lotto-postgres || true"
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd1b

Write-Host "  Moving JAR file to target directory..." -ForegroundColor Gray
$cmdJar = "mkdir -p $REMOTE_PATH/target && cp $REMOTE_PATH/lotto-api-latest.jar $REMOTE_PATH/target/lotto-api-1.0.0-SNAPSHOT.jar"
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdJar

Write-Host "  Building Docker image..." -ForegroundColor Gray
$cmdBuild = "cd $REMOTE_PATH && docker build -t lotto-api:latest -f Dockerfile ."
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdBuild

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] [STEP 9] Docker image build failed" -ForegroundColor Red
    exit 1
}

Write-Host "  Starting services with Docker Compose (postgres + lotto-api)..." -ForegroundColor Gray
# --force-recreate: Recreate containers even if volumes changed to load new JAR file
# postgres starts first, lotto-api starts automatically via depends_on
# Use docker-compose (v1) - most EC2 instances have this
$cmd2 = "cd $REMOTE_PATH && docker-compose -f docker-compose.aws.yml up -d --force-recreate"
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd2

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] [STEP 9] Docker Compose start failed" -ForegroundColor Red
    Write-Host "  Check logs: ssh -i $KEY_FILE ${INSTANCE_USER}@${INSTANCE_DNS} 'cd $REMOTE_PATH && docker-compose -f docker-compose.aws.yml logs --tail=50 lotto-api'" -ForegroundColor Yellow
    exit 1
}

Write-Host "[INFO] Waiting for health check (30 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

Write-Host "[INFO] Checking Lotto API logs..." -ForegroundColor Gray
$cmd3 = "cd $REMOTE_PATH && docker-compose -f docker-compose.aws.yml logs --tail 50 lotto-api"
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd3

Write-Host "[INFO] Checking health endpoint..." -ForegroundColor Gray
$cmd4 = "curl -s http://localhost:8083/lotto/actuator/health || echo 'Health check failed'"
$healthResult = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd4
Write-Host $healthResult -ForegroundColor Gray

if ($healthResult -match "UP|status.*UP") {
    Write-Host "[SUCCESS] Health check passed" -ForegroundColor Green
} else {
    Write-Host "[WARNING] Health check failed or needs verification" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[SUCCESS] Lotto API service deployment completed!" -ForegroundColor Green
Write-Host "[INFO] Access URLs:" -ForegroundColor Cyan
Write-Host "  - API: http://$INSTANCE_IP:8083/lotto/api/v1" -ForegroundColor White
Write-Host "  - Frontend: http://$INSTANCE_IP:8083/lotto/" -ForegroundColor White
Write-Host "  - Health: http://$INSTANCE_IP:8083/lotto/actuator/health" -ForegroundColor White
Write-Host ""
Write-Host "[INFO] Additional commands:" -ForegroundColor Cyan
Write-Host "  - Logs: ssh -i $KEY_FILE ${INSTANCE_USER}@${INSTANCE_DNS} 'cd $REMOTE_PATH && docker-compose -f docker-compose.aws.yml logs -f lotto-api'" -ForegroundColor Gray
$countCmd = "docker exec lotto-postgres psql -U postgres -d lottoguide -c 'SELECT COUNT(1) FROM mission_templates;'"
Write-Host "  - mission_templates data: ssh -i $KEY_FILE ${INSTANCE_USER}@${INSTANCE_DNS} `"$countCmd`"" -ForegroundColor Gray
