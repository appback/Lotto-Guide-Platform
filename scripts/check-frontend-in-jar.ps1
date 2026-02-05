# JAR 파일에 프론트엔드 파일이 포함되어 있는지 확인

$ROOT_DIR = Split-Path -Parent $PSScriptRoot
$JAR_FILE = Get-ChildItem -Path "$ROOT_DIR\lotto-api\target" -Filter "lotto-api-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1

if ($null -eq $JAR_FILE) {
    Write-Host "[ERROR] JAR 파일을 찾을 수 없습니다" -ForegroundColor Red
    exit 1
}

Write-Host "[INFO] JAR 파일: $($JAR_FILE.FullName)" -ForegroundColor Cyan
Write-Host ""

# JAR 파일 내용 확인 (압축 해제 없이)
Write-Host "[STEP 1] JAR 파일 내부 확인 (index.html 검색)..." -ForegroundColor Yellow
$jarContent = jar -tf $JAR_FILE.FullName | Select-String "static/index.html"

if ($jarContent) {
    Write-Host "[SUCCESS] index.html 발견:" -ForegroundColor Green
    $jarContent | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
} else {
    Write-Host "[FAILURE] index.html을 찾을 수 없습니다!" -ForegroundColor Red
    Write-Host "  → 프론트엔드 빌드가 JAR에 포함되지 않았습니다" -ForegroundColor Yellow
}

Write-Host ""

# static 디렉토리 확인
Write-Host "[STEP 2] static 디렉토리 확인..." -ForegroundColor Yellow
$staticFiles = jar -tf $JAR_FILE.FullName | Select-String "^BOOT-INF/classes/static/" | Select-Object -First 10

if ($staticFiles) {
    Write-Host "[SUCCESS] static 파일 발견 (처음 10개):" -ForegroundColor Green
    $staticFiles | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
} else {
    Write-Host "[FAILURE] static 디렉토리를 찾을 수 없습니다!" -ForegroundColor Red
}

Write-Host ""

# 프론트엔드 빌드 결과물 확인
Write-Host "[STEP 3] 로컬 프론트엔드 빌드 결과물 확인..." -ForegroundColor Yellow
$distDir = Join-Path $ROOT_DIR "client-frontend\dist"

if (Test-Path $distDir) {
    $indexFile = Join-Path $distDir "index.html"
    if (Test-Path $indexFile) {
        Write-Host "[SUCCESS] 로컬 빌드 결과물 존재: $distDir" -ForegroundColor Green
        $fileCount = (Get-ChildItem -Path $distDir -Recurse -File).Count
        Write-Host "  파일 개수: $fileCount" -ForegroundColor Gray
    } else {
        Write-Host "[FAILURE] index.html이 없습니다: $distDir" -ForegroundColor Red
    }
} else {
    Write-Host "[FAILURE] 프론트엔드 빌드 결과물이 없습니다: $distDir" -ForegroundColor Red
    Write-Host "  → 먼저 프론트엔드를 빌드하세요: npm run build (client-frontend 디렉토리에서)" -ForegroundColor Yellow
}
