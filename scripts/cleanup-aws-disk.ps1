# AWS EC2 인스턴스 디스크 용량 정리 스크립트
# Docker 이미지, 컨테이너, 볼륨 등 불필요한 파일 정리

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

Write-Host "[INFO] Key file: $KEY_FILE_PATH" -ForegroundColor Cyan
Write-Host "[INFO] Starting disk cleanup on AWS EC2 instance..." -ForegroundColor Green
Write-Host ""

# 1. 디스크 사용량 확인
Write-Host "[STEP 1] Checking disk usage..." -ForegroundColor Yellow
$cmdDisk = "df -h /"
$diskUsage = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdDisk
Write-Host $diskUsage -ForegroundColor Gray
Write-Host ""

# 2. Docker 디스크 사용량 확인
Write-Host "[STEP 2] Checking Docker disk usage..." -ForegroundColor Yellow
$cmdDocker = "docker system df"
$dockerUsage = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdDocker
Write-Host $dockerUsage -ForegroundColor Gray
Write-Host ""

# 3. 중지된 컨테이너 제거
Write-Host "[STEP 3] Removing stopped containers..." -ForegroundColor Yellow
$cmdStopContainers = "docker container prune -f"
$result = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdStopContainers
Write-Host $result -ForegroundColor Gray
Write-Host "[SUCCESS] [STEP 3] Stopped containers removed" -ForegroundColor Green
Write-Host ""

# 4. 사용하지 않는 이미지 제거 (dangling images)
Write-Host "[STEP 4] Removing dangling images..." -ForegroundColor Yellow
$cmdDangling = "docker image prune -f"
$result = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdDangling
Write-Host $result -ForegroundColor Gray
Write-Host "[SUCCESS] [STEP 4] Dangling images removed" -ForegroundColor Green
Write-Host ""

# 5. 사용하지 않는 볼륨 및 오래된 볼륨 제거
Write-Host "[STEP 5] Removing unused and old volumes..." -ForegroundColor Yellow

# 먼저 사용하지 않는 볼륨 제거
Write-Host "  Removing unused volumes..." -ForegroundColor Gray
$cmdVolumes = "docker volume prune -f"
$result = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdVolumes
Write-Host $result -ForegroundColor Gray

# 사용 중이 아닌 볼륨 목록 확인 및 제거 (7일 이상 사용되지 않은 볼륨)
Write-Host "  Checking for old unused volumes..." -ForegroundColor Gray
$cmdListVolumes = "docker volume ls -q"
$volumes = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdListVolumes

if ($volumes) {
    $volumesArray = $volumes -split "`n" | Where-Object { $_.Trim() -ne "" }
    $removedCount = 0
    
    foreach ($volume in $volumesArray) {
        if ($volume.Trim() -eq "") { continue }
        
        # 볼륨이 컨테이너에 마운트되어 있는지 확인
        $cmdCheckMount = "docker ps -a --filter volume=$volume --format '{{.Names}}'"
        $mountedContainers = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdCheckMount
        
        if (-not $mountedContainers -or $mountedContainers.Trim() -eq "") {
            # 마운트되지 않은 볼륨 - 오래된 것인지 확인 (7일 이상)
            $cmdVolumeInfo = "docker volume inspect $volume --format '{{.CreatedAt}}' 2>/dev/null"
            $createdAt = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdVolumeInfo
            
            if ($createdAt) {
                # 볼륨 제거 시도
                $cmdRemoveVolume = "docker volume rm $volume 2>&1"
                $removeResult = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdRemoveVolume
                
                if ($LASTEXITCODE -eq 0) {
                    $removedCount++
                    Write-Host "    Removed volume: $volume" -ForegroundColor Gray
                }
            }
        }
    }
    
    if ($removedCount -gt 0) {
        Write-Host "  Removed $removedCount old unused volume(s)" -ForegroundColor Gray
    } else {
        Write-Host "  No old unused volumes found" -ForegroundColor Gray
    }
} else {
    Write-Host "  No volumes to check" -ForegroundColor Gray
}

Write-Host "[SUCCESS] [STEP 5] Unused and old volumes removed" -ForegroundColor Green
Write-Host ""

# 6. 사용하지 않는 네트워크 제거
Write-Host "[STEP 6] Removing unused networks..." -ForegroundColor Yellow
$cmdNetworks = "docker network prune -f"
$result = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdNetworks
Write-Host $result -ForegroundColor Gray
Write-Host "[SUCCESS] [STEP 6] Unused networks removed" -ForegroundColor Green
Write-Host ""

# 7. 오래된 JAR 파일 정리 (최근 3개만 유지)
Write-Host "[STEP 7] Cleaning up old JAR files..." -ForegroundColor Yellow
$cmdJarCleanup = "cd $REMOTE_PATH && ls -t lotto-api-*.jar 2>/dev/null | tail -n +4 | xargs -r rm -f && echo 'Old JAR files removed' || echo 'No old JAR files to remove'"
$result = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdJarCleanup
Write-Host $result -ForegroundColor Gray
Write-Host "[SUCCESS] [STEP 7] Old JAR files cleaned" -ForegroundColor Green
Write-Host ""

# 8. Docker 빌드 캐시 정리 (선택적 - 필요시 주석 해제)
Write-Host "[STEP 8] Cleaning Docker build cache..." -ForegroundColor Yellow
$cmdBuildCache = "docker builder prune -f"
$result = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdBuildCache
Write-Host $result -ForegroundColor Gray
Write-Host "[SUCCESS] [STEP 8] Docker build cache cleaned" -ForegroundColor Green
Write-Host ""

# 9. 로그 파일 정리 (7일 이상 된 로그)
Write-Host "[STEP 9] Cleaning old log files..." -ForegroundColor Yellow
$cmdLogs = "find $REMOTE_PATH -name '*.log' -type f -mtime +7 -delete && echo 'Old log files removed' || echo 'No old log files to remove'"
$result = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdLogs
Write-Host $result -ForegroundColor Gray
Write-Host "[SUCCESS] [STEP 9] Old log files cleaned" -ForegroundColor Green
Write-Host ""

# 10. 최종 디스크 사용량 확인
Write-Host "[STEP 10] Final disk usage check..." -ForegroundColor Yellow
$cmdFinalDisk = "df -h /"
$finalDiskUsage = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdFinalDisk
Write-Host $finalDiskUsage -ForegroundColor Gray
Write-Host ""

$cmdFinalDocker = "docker system df"
$finalDockerUsage = & ssh -i $KEY_FILE_PATH "${INSTANCE_USER}@${INSTANCE_DNS}" $cmdFinalDocker
Write-Host $finalDockerUsage -ForegroundColor Gray
Write-Host ""

Write-Host "[SUCCESS] Disk cleanup completed!" -ForegroundColor Green
Write-Host "[INFO] You can now retry the deployment." -ForegroundColor Cyan
