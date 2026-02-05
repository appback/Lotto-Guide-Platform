# 실제 사용 중인 데이터베이스 확인

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

Write-Host "[INFO] 실제 사용 중인 데이터베이스 확인 중..." -ForegroundColor Green
Write-Host ""

# lotto_draw 테이블이 있는 데이터베이스 찾기
Write-Host "[1] lotto_draw 테이블이 있는 데이터베이스 찾기:" -ForegroundColor Yellow
$cmd1 = @"
for db in \$(docker exec lotto-postgres psql -U postgres -lqt | cut -d \| -f 1 | grep -v template | grep -v '^$' | xargs); do
  exists=\$(docker exec lotto-postgres psql -U postgres -d "\$db" -t -c "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'lotto_draw');" 2>/dev/null | xargs)
  if [ "\$exists" = "t" ]; then
    count=\$(docker exec lotto-postgres psql -U postgres -d "\$db" -t -c "SELECT COUNT(*) FROM lotto_draw;" 2>/dev/null | xargs)
    echo "  데이터베이스: \$db - lotto_draw 테이블 있음 (데이터: \$count 개)"
  fi
done
"@
$result1 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd1
Write-Host $result1 -ForegroundColor Gray
Write-Host ""

# 모든 데이터베이스에서 모든 테이블 확인
Write-Host "[2] 모든 데이터베이스의 테이블 목록:" -ForegroundColor Yellow
$cmd2 = @"
for db in \$(docker exec lotto-postgres psql -U postgres -lqt | cut -d \| -f 1 | grep -v template | grep -v '^$' | xargs); do
  echo "=== \$db ==="
  docker exec lotto-postgres psql -U postgres -d "\$db" -c '\dt' 2>/dev/null | grep -v "Did not find" | grep -v "List of relations" | grep -v "Schema" | grep -v "---" | grep -v "rows)" | grep -v "^$" | sed 's/^/  /'
  echo ""
done
"@
$result2 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd2
Write-Host $result2 -ForegroundColor Gray

Write-Host "[INFO] 확인 완료!" -ForegroundColor Green
