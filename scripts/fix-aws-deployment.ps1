# AWS 배포 수정 스크립트
# 프론트엔드 빌드 후 재배포

$ErrorActionPreference = "Stop"

$ROOT_DIR = Split-Path -Parent $PSScriptRoot

Write-Host "[INFO] === AWS 배포 수정 (프론트엔드 포함) ===" -ForegroundColor Cyan
Write-Host ""

# 1. 프론트엔드 빌드
Write-Host "[STEP 1] 프론트엔드 빌드 중..." -ForegroundColor Yellow
$frontendDir = Join-Path $ROOT_DIR "client-frontend"

if (-not (Test-Path $frontendDir)) {
    Write-Host "[ERROR] 프론트엔드 디렉토리를 찾을 수 없습니다: $frontendDir" -ForegroundColor Red
    exit 1
}

Push-Location $frontendDir

try {
    Write-Host "  npm install 실행 중..." -ForegroundColor Gray
    & npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] npm install 실패" -ForegroundColor Red
        exit 1
    }

    Write-Host "  npm run build 실행 중..." -ForegroundColor Gray
    & npm run build
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] npm run build 실패" -ForegroundColor Red
        exit 1
    }

    $distDir = Join-Path $frontendDir "dist"
    $indexFile = Join-Path $distDir "index.html"
    if (-not (Test-Path $indexFile)) {
        Write-Host "[ERROR] 프론트엔드 빌드 결과물을 찾을 수 없습니다: $indexFile" -ForegroundColor Red
        exit 1
    }

    Write-Host "[SUCCESS] 프론트엔드 빌드 완료" -ForegroundColor Green
} finally {
    Pop-Location
}

Write-Host ""

# 2. 백엔드 빌드 (프론트엔드 포함)
Write-Host "[STEP 2] 백엔드 빌드 중 (프론트엔드 포함)..." -ForegroundColor Yellow
Push-Location $ROOT_DIR

try {
    $mavenCmd = "mvn clean package -DskipTests -f lotto-api/pom.xml"
    Write-Host "  $mavenCmd" -ForegroundColor Gray
    & cmd /c $mavenCmd
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] 백엔드 빌드 실패" -ForegroundColor Red
        exit 1
    }

    # JAR 파일에 프론트엔드 포함 여부 확인
    $jarFile = Get-ChildItem -Path "$ROOT_DIR\lotto-api\target" -Filter "lotto-api-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    
    if ($null -eq $jarFile) {
        Write-Host "[ERROR] JAR 파일을 찾을 수 없습니다" -ForegroundColor Red
        exit 1
    }

    Write-Host "  JAR 파일 확인 중: $($jarFile.Name)" -ForegroundColor Gray
    $jarContent = & jar -tf $jarFile.FullName | Select-String "static/index.html"
    
    if ($jarContent) {
        Write-Host "[SUCCESS] JAR 파일에 프론트엔드 포함 확인됨" -ForegroundColor Green
    } else {
        Write-Host "[WARNING] JAR 파일에 프론트엔드가 포함되지 않았을 수 있습니다" -ForegroundColor Yellow
        Write-Host "  하지만 계속 진행합니다..." -ForegroundColor Gray
    }

    Write-Host "[SUCCESS] 백엔드 빌드 완료" -ForegroundColor Green
} finally {
    Pop-Location
}

Write-Host ""

# 3. AWS 배포
Write-Host "[STEP 3] AWS 배포 중..." -ForegroundColor Yellow
$deployScript = Join-Path $PSScriptRoot "deploy-lotto-api-aws.ps1"

if (-not (Test-Path $deployScript)) {
    Write-Host "[ERROR] 배포 스크립트를 찾을 수 없습니다: $deployScript" -ForegroundColor Red
    exit 1
}

& powershell -ExecutionPolicy Bypass -File $deployScript

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] AWS 배포 실패" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[SUCCESS] === 배포 수정 완료 ===" -ForegroundColor Green
Write-Host "[INFO] 접속 URL: http://15.164.228.217:8083/lotto/" -ForegroundColor Cyan
