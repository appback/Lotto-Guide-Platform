# AWS 서버에서 기존 데이터베이스 확인 스크립트

$ErrorActionPreference = "Stop"

$INSTANCE_IP = "15.164.228.217"
$INSTANCE_USER = "ec2-user"
$INSTANCE_DNS = "ec2-15-164-228-217.ap-northeast-2.compute.amazonaws.com"
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

Write-Host "[INFO] Checking existing databases and tables..." -ForegroundColor Green
Write-Host ""

# 1. 모든 데이터베이스 목록
Write-Host "[1] All databases:" -ForegroundColor Yellow
$cmd1 = "docker exec lotto-postgres psql -U postgres -l"
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd1
Write-Host ""

# 2. postgres 데이터베이스의 테이블 확인
Write-Host "[2] Tables in 'postgres' database:" -ForegroundColor Yellow
$cmd2 = "docker exec lotto-postgres psql -U postgres -d postgres -c '\dt'"
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd2
Write-Host ""

# 3. readme_to_recover 데이터베이스의 테이블 확인
Write-Host "[3] Tables in 'readme_to_recover' database:" -ForegroundColor Yellow
$cmd3 = "docker exec lotto-postgres psql -U postgres -d readme_to_recover -c '\dt'"
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd3
Write-Host ""

# 4. lotto_draw 테이블이 있는 데이터베이스 찾기
Write-Host "[4] Searching for 'lotto_draw' table in all databases..." -ForegroundColor Yellow
$cmd4 = @"
for db in postgres readme_to_recover; do
  echo "Checking database: \$db"
  docker exec lotto-postgres psql -U postgres -d \$db -c "SELECT tablename FROM pg_tables WHERE tablename = 'lotto_draw';" 2>/dev/null | grep -q lotto_draw && echo "  -> Found lotto_draw in \$db" || echo "  -> Not found in \$db"
done
"@
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd4
Write-Host ""

# 5. 모든 데이터베이스에서 lotto 관련 테이블 찾기
Write-Host "[5] Searching for lotto-related tables in all databases..." -ForegroundColor Yellow
$cmd5 = @"
for db in postgres readme_to_recover; do
  echo "Database: \$db"
  docker exec lotto-postgres psql -U postgres -d \$db -c "SELECT tablename FROM pg_tables WHERE tablename LIKE 'lotto%' OR tablename LIKE 'mission%' OR tablename LIKE 'generated%';" 2>/dev/null | grep -v "tablename" | grep -v "---" | grep -v "rows" | grep -v "^$" | sed 's/^/  -> /'
done
"@
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd5
Write-Host ""

Write-Host "[INFO] Check completed!" -ForegroundColor Green
