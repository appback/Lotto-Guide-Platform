# AWS 데이터베이스 문제 수정 스크립트

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

Write-Host "[INFO] === AWS 데이터베이스 문제 수정 ===" -ForegroundColor Cyan
Write-Host ""

# 1. PostgreSQL 컨테이너 상태 확인
Write-Host "[STEP 1] PostgreSQL 컨테이너 상태 확인..." -ForegroundColor Yellow
$cmd1 = "docker ps -a | grep postgres"
$result1 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd1
Write-Host $result1 -ForegroundColor Gray
Write-Host ""

# 2. 데이터베이스 생성
Write-Host "[STEP 2] 데이터베이스 생성 중..." -ForegroundColor Yellow
$cmd2 = "docker exec -i lotto-postgres psql -U postgres -c 'CREATE DATABASE lottoguide;' 2>&1 || echo '데이터베이스가 이미 존재하거나 컨테이너가 실행 중이 아닙니다'"
$result2 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd2
Write-Host $result2 -ForegroundColor Gray
Write-Host ""

# 3. 데이터베이스 목록 확인
Write-Host "[STEP 3] 데이터베이스 목록 확인..." -ForegroundColor Yellow
$cmd3 = "docker exec -i lotto-postgres psql -U postgres -c '\l' | grep lottoguide || echo '데이터베이스 확인 실패'"
$result3 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd3
Write-Host $result3 -ForegroundColor Gray
Write-Host ""

# 4. 컨테이너 재시작
Write-Host "[STEP 4] 컨테이너 재시작 중..." -ForegroundColor Yellow
$cmd4 = "cd $REMOTE_PATH && docker-compose -f docker-compose.aws.yml restart lotto-api"
$result4 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd4
Write-Host $result4 -ForegroundColor Gray
Write-Host ""

Write-Host "[INFO] 30초 대기 중..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# 5. 컨테이너 상태 확인
Write-Host "[STEP 5] 컨테이너 상태 확인..." -ForegroundColor Yellow
$cmd5 = "docker ps | grep lotto-api"
$result5 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd5
Write-Host $result5 -ForegroundColor Gray
Write-Host ""

Write-Host "[INFO] === 수정 완료 ===" -ForegroundColor Cyan
Write-Host "[TIP] 프론트엔드 문제를 해결하려면 다음을 실행하세요:" -ForegroundColor Yellow
Write-Host "  python scripts/run-automation.py --service lotto-api --stage aws-deploy" -ForegroundColor White
