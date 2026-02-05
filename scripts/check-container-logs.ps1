# 컨테이너 로그 확인 스크립트

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

Write-Host "[INFO] === 컨테이너 로그 확인 ===" -ForegroundColor Cyan
Write-Host ""

# 컨테이너 상태 확인
Write-Host "[STEP 1] 컨테이너 상태 확인..." -ForegroundColor Yellow
$cmd1 = "docker ps -a | grep lotto-api"
$result1 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd1
Write-Host $result1 -ForegroundColor Gray
Write-Host ""

# 최근 로그 확인
Write-Host "[STEP 2] 최근 로그 확인 (마지막 100줄)..." -ForegroundColor Yellow
$cmd2 = "cd $REMOTE_PATH && docker-compose -f docker-compose.aws.yml logs --tail=100 lotto-api"
$result2 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd2
Write-Host $result2 -ForegroundColor Gray
Write-Host ""

# 에러 로그만 확인
Write-Host "[STEP 3] 에러 로그 확인..." -ForegroundColor Yellow
$cmd3 = "cd $REMOTE_PATH && docker-compose -f docker-compose.aws.yml logs --tail=50 lotto-api | grep -i error || echo '에러 로그 없음'"
$result3 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd3
Write-Host $result3 -ForegroundColor Gray
