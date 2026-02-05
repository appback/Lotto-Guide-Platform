# AWS 연결 상태 확인 스크립트
# EC2 인스턴스의 포트 접근 가능 여부를 확인합니다.

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

Write-Host "[INFO] === AWS 연결 상태 확인 ===" -ForegroundColor Cyan
Write-Host ""

# 1. Docker 컨테이너 상태 확인
Write-Host "[STEP 1] Docker 컨테이너 상태 확인..." -ForegroundColor Yellow
$cmd1 = "cd $REMOTE_PATH && docker-compose -f docker-compose.aws.yml ps"
$result1 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd1
Write-Host $result1 -ForegroundColor Gray
Write-Host ""

# 2. 포트 리스닝 상태 확인 (호스트)
Write-Host "[STEP 2] 호스트 포트 리스닝 상태 확인 (8083)..." -ForegroundColor Yellow
$cmd2 = "netstat -tlnp | grep :8083 || ss -tlnp | grep :8083 || echo '포트 8083이 리스닝되지 않음'"
$result2 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd2
Write-Host $result2 -ForegroundColor Gray
Write-Host ""

# 3. Docker 포트 바인딩 확인
Write-Host "[STEP 3] Docker 포트 바인딩 확인..." -ForegroundColor Yellow
$cmd3 = "docker ps --format 'table {{.Names}}\t{{.Ports}}' | grep lotto-api"
$result3 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd3
Write-Host $result3 -ForegroundColor Gray
Write-Host ""

# 4. 로컬에서 헬스 체크 (컨테이너 내부)
Write-Host "[STEP 4] 컨테이너 내부에서 헬스 체크 (localhost:8080)..." -ForegroundColor Yellow
$cmd4 = "docker exec lotto-api curl -s http://localhost:8080/lotto/actuator/health || echo '컨테이너 내부 헬스 체크 실패'"
$result4 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd4
Write-Host $result4 -ForegroundColor Gray
Write-Host ""

# 5. 호스트에서 헬스 체크 (localhost:8083)
Write-Host "[STEP 5] 호스트에서 헬스 체크 (localhost:8083)..." -ForegroundColor Yellow
$cmd5 = "curl -s http://localhost:8083/lotto/actuator/health || echo '호스트 헬스 체크 실패'"
$result5 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd5
Write-Host $result5 -ForegroundColor Gray
Write-Host ""

# 6. 방화벽 상태 확인 (ufw)
Write-Host "[STEP 6] 방화벽 상태 확인 (ufw)..." -ForegroundColor Yellow
$cmd6 = "sudo ufw status || echo 'ufw가 설치되지 않았거나 비활성화됨'"
$result6 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd6
Write-Host $result6 -ForegroundColor Gray
Write-Host ""

# 7. iptables 규칙 확인 (포트 8083)
Write-Host "[STEP 7] iptables 규칙 확인 (포트 8083)..." -ForegroundColor Yellow
$cmd7 = "sudo iptables -L -n | grep 8083 || echo 'iptables에 8083 포트 규칙이 없음'"
$result7 = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmd7
Write-Host $result7 -ForegroundColor Gray
Write-Host ""

# 8. 외부에서 접근 테스트 (로컬에서)
Write-Host "[STEP 8] 외부에서 접근 테스트 (로컬에서 $INSTANCE_IP:8083)..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://${INSTANCE_IP}:8083/lotto/actuator/health" -TimeoutSec 5 -UseBasicParsing
    Write-Host "[SUCCESS] 외부 접근 성공: $($response.StatusCode)" -ForegroundColor Green
    Write-Host $response.Content -ForegroundColor Gray
} catch {
    Write-Host "[FAILURE] 외부 접근 실패: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  → AWS 보안 그룹에서 포트 8083 인바운드 규칙을 확인하세요" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "[INFO] === 확인 완료 ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "[TIP] AWS 보안 그룹 확인 방법:" -ForegroundColor Yellow
Write-Host "  1. AWS Console → EC2 → Security Groups" -ForegroundColor White
Write-Host "  2. 해당 인스턴스의 보안 그룹 선택" -ForegroundColor White
Write-Host "  3. Inbound rules에서 포트 8083 (TCP) 규칙이 있는지 확인" -ForegroundColor White
Write-Host "  4. Source를 0.0.0.0/0 (모든 IP) 또는 특정 IP로 설정" -ForegroundColor White
