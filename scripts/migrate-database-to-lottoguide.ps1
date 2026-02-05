# postgres 데이터베이스에서 lottoguide로 마이그레이션

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

if ($null -eq $KEY_FILE_PATH) {
    $KEY_FILE_PATH = "C:\Projects\Lotto-Guide-Platform\dadp-prod.pem"
}

if ($null -eq $KEY_FILE_PATH -or -not (Test-Path $KEY_FILE_PATH)) {
    Write-Host "[ERROR] Key file not found: $KEY_FILE" -ForegroundColor Red
    exit 1
}

Write-Host "[INFO] 데이터베이스 마이그레이션 시작..." -ForegroundColor Green
Write-Host "  Source: postgres" -ForegroundColor Yellow
Write-Host "  Target: lottoguide" -ForegroundColor Yellow
Write-Host ""

# 1. lottoguide 데이터베이스 생성
Write-Host "[STEP 1] Creating lottoguide database..." -ForegroundColor Yellow
$createDbCmd = "docker exec lotto-postgres psql -U postgres -c 'CREATE DATABASE lottoguide;' 2>&1"
$createResult = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $createDbCmd

if ($LASTEXITCODE -ne 0) {
    if ($createResult -match "already exists") {
        Write-Host "[INFO] Database 'lottoguide' already exists" -ForegroundColor Yellow
    } else {
        Write-Host "[ERROR] Failed to create database: $createResult" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[SUCCESS] Database 'lottoguide' created" -ForegroundColor Green
}

# 2. postgres 데이터베이스의 모든 테이블 목록 가져오기
Write-Host "[STEP 2] Getting table list from postgres database..." -ForegroundColor Yellow
$getTablesCmd = "docker exec lotto-postgres psql -U postgres -d postgres -t -c \"SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;\""
$tablesOutput = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $getTablesCmd
$tables = $tablesOutput -split "`n" | Where-Object { $_ -match '\S' } | ForEach-Object { $_.Trim() }

Write-Host "[INFO] Found $($tables.Count) tables to migrate" -ForegroundColor Cyan
Write-Host "  Tables: $($tables -join ', ')" -ForegroundColor Gray
Write-Host ""

# 3. pg_dump로 postgres 데이터베이스 전체 덤프
Write-Host "[STEP 3] Dumping postgres database..." -ForegroundColor Yellow
$dumpCmd = "docker exec lotto-postgres pg_dump -U postgres -d postgres --schema-only --no-owner --no-acl > /tmp/postgres_schema.sql 2>&1 && echo 'SCHEMA_DUMP_OK' || echo 'SCHEMA_DUMP_FAILED'"
$dumpResult = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $dumpCmd

if ($dumpResult -notmatch "SCHEMA_DUMP_OK") {
    Write-Host "[ERROR] Schema dump failed" -ForegroundColor Red
    exit 1
}

$dataDumpCmd = "docker exec lotto-postgres pg_dump -U postgres -d postgres --data-only --no-owner --no-acl > /tmp/postgres_data.sql 2>&1 && echo 'DATA_DUMP_OK' || echo 'DATA_DUMP_FAILED'"
$dataDumpResult = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $dataDumpCmd

if ($dataDumpResult -notmatch "DATA_DUMP_OK") {
    Write-Host "[ERROR] Data dump failed" -ForegroundColor Red
    exit 1
}

Write-Host "[SUCCESS] Database dumped" -ForegroundColor Green

# 4. 스키마를 lottoguide에 복원 (데이터베이스 이름 변경)
Write-Host "[STEP 4] Restoring schema to lottoguide database..." -ForegroundColor Yellow
$restoreSchemaCmd = @"
docker exec -i lotto-postgres psql -U postgres -d lottoguide < /tmp/postgres_schema.sql 2>&1 | tail -5
"@
$restoreResult = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $restoreSchemaCmd

Write-Host "[SUCCESS] Schema restored" -ForegroundColor Green

# 5. 데이터를 lottoguide에 복원
Write-Host "[STEP 5] Restoring data to lottoguide database..." -ForegroundColor Yellow
$restoreDataCmd = @"
docker exec -i lotto-postgres psql -U postgres -d lottoguide < /tmp/postgres_data.sql 2>&1 | tail -5
"@
$restoreDataResult = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $restoreDataCmd

Write-Host "[SUCCESS] Data restored" -ForegroundColor Green

# 6. 검증: 테이블 개수 확인
Write-Host "[STEP 6] Verifying migration..." -ForegroundColor Yellow
$verifyCmd = "docker exec lotto-postgres psql -U postgres -d lottoguide -t -c \"SELECT COUNT(*) FROM pg_tables WHERE schemaname = 'public';\""
$tableCount = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $verifyCmd
$tableCount = $tableCount.Trim()

Write-Host "[INFO] Tables in lottoguide: $tableCount" -ForegroundColor Cyan

# 7. lotto_draw 데이터 개수 확인
$countCmd = "docker exec lotto-postgres psql -U postgres -d lottoguide -t -c 'SELECT COUNT(*) FROM lotto_draw;'"
$drawCount = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $countCmd
$drawCount = $drawCount.Trim()

Write-Host "[INFO] lotto_draw records: $drawCount" -ForegroundColor Cyan

# 8. 임시 파일 정리
Write-Host "[STEP 7] Cleaning up temporary files..." -ForegroundColor Yellow
$cleanupCmd = "rm -f /tmp/postgres_schema.sql /tmp/postgres_data.sql && echo 'CLEANUP_OK' || echo 'CLEANUP_FAILED'"
$cleanupResult = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cleanupCmd

Write-Host ""
Write-Host "[SUCCESS] 마이그레이션 완료!" -ForegroundColor Green
Write-Host "[INFO] 이제 docker-compose를 재시작하면 lottoguide 데이터베이스를 사용합니다." -ForegroundColor Cyan
