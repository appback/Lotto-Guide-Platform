# AWS 서버에서 기존 데이터 찾기 스크립트

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

Write-Host "[INFO] Searching for existing lotto data..." -ForegroundColor Green
Write-Host ""

# 1. 모든 데이터베이스 목록
Write-Host "[1] All databases:" -ForegroundColor Yellow
$cmd1 = "docker exec lotto-postgres psql -U postgres -l"
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd1
Write-Host ""

# 2. 모든 데이터베이스에서 lotto 관련 테이블 찾기
Write-Host "[2] Searching for lotto-related tables in ALL databases..." -ForegroundColor Yellow
$cmd2 = @"
for db in \$(docker exec lotto-postgres psql -U postgres -lqt | cut -d \| -f 1 | grep -v template | grep -v '^$' | xargs); do
  echo "=== Database: \$db ==="
  tables=\$(docker exec lotto-postgres psql -U postgres -d "\$db" -t -c "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND (tablename LIKE 'lotto%' OR tablename LIKE 'mission%' OR tablename LIKE 'generated%' OR tablename LIKE 'strategy%' OR tablename LIKE 'system%' OR tablename LIKE 'destiny%');" 2>/dev/null | xargs)
  if [ -n "\$tables" ]; then
    echo "  Found tables: \$tables"
    for table in \$tables; do
      count=\$(docker exec lotto-postgres psql -U postgres -d "\$db" -t -c "SELECT COUNT(*) FROM \$table;" 2>/dev/null | xargs)
      echo "    - \$table: \$count rows"
    done
  else
    echo "  No lotto-related tables found"
  fi
  echo ""
done
"@
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd2
Write-Host ""

# 3. lotto_draw 테이블이 있는 데이터베이스와 데이터 개수
Write-Host "[3] Searching for lotto_draw table with data..." -ForegroundColor Yellow
$cmd3 = @"
for db in \$(docker exec lotto-postgres psql -U postgres -lqt | cut -d \| -f 1 | grep -v template | grep -v '^$' | xargs); do
  exists=\$(docker exec lotto-postgres psql -U postgres -d "\$db" -t -c "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'lotto_draw');" 2>/dev/null | xargs)
  if [ "\$exists" = "t" ]; then
    count=\$(docker exec lotto-postgres psql -U postgres -d "\$db" -t -c "SELECT COUNT(*) FROM lotto_draw;" 2>/dev/null | xargs)
    max_draw=\$(docker exec lotto-postgres psql -U postgres -d "\$db" -t -c "SELECT MAX(draw_no) FROM lotto_draw;" 2>/dev/null | xargs)
    echo "Database: \$db"
    echo "  lotto_draw table exists: YES"
    echo "  Total rows: \$count"
    echo "  Max draw_no: \$max_draw"
    echo ""
  fi
done
"@
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd3
Write-Host ""

# 4. 모든 테이블 목록 (어떤 데이터베이스에 뭐가 있는지)
Write-Host "[4] All tables in each database:" -ForegroundColor Yellow
$cmd4 = @"
for db in \$(docker exec lotto-postgres psql -U postgres -lqt | cut -d \| -f 1 | grep -v template | grep -v '^$' | xargs); do
  echo "=== Database: \$db ==="
  docker exec lotto-postgres psql -U postgres -d "\$db" -c '\dt' 2>/dev/null | grep -v "Did not find" | grep -v "List of relations" | grep -v "Schema" | grep -v "---" | grep -v "rows)" | grep -v "^$" | sed 's/^/  /'
  echo ""
done
"@
& ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd4

Write-Host "[INFO] Search completed!" -ForegroundColor Green
