#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Lotto Guide Platform 자동화 스크립트
- lotto-api 서비스 빌드 및 배포 자동화
- --service lotto-api
- --local: 로컬 배포
- --stage aws-deploy: AWS 배포
"""

import argparse
import subprocess
import sys
import time
import re
from datetime import datetime
from pathlib import Path
import requests
import os
import shutil

# UTF-8 인코딩 설정
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
sys.stderr.reconfigure(encoding='utf-8', errors='replace')

# === 프로젝트 루트 설정 ===
PROJECT_ROOT = Path(__file__).parent.parent.resolve()
os.chdir(PROJECT_ROOT)

# 서비스 설정
SERVICE_CONFIG = {
    'lotto-api': {
        'container': 'lotto-api-local',
        'port': 8080,
        'health_endpoint': '/lotto/actuator/health',  # Context Path: /lotto
        'jar_pattern': 'lotto-api/target/lotto-api-*.jar',
        'service_name': 'lotto-api',
        'has_frontend': False,
        'compose_path_local': 'docker/docker-compose.local.yml',
        'compose_path_aws': 'docker/docker-compose.aws.yml'
    }
}

def run_command(command: str, monitor: bool = False, ignore_errors: bool = False, timeout: int = None) -> tuple[bool, list[str], list[str], dict]:
    """명령어 실행"""
    errors = []
    warnings = []
    performance_data = {}
    
    print(f"[COMMAND] {command}")
    
    # 타임아웃 설정: 명시적 타임아웃이 있으면 사용, 없으면 기본값
    if timeout is None:
        # Maven 빌드는 시간이 오래 걸릴 수 있으므로 타임아웃을 30분(1800초)으로 설정
        # 배포 스크립트는 SSH/SCP/Docker 빌드 등으로 시간이 오래 걸릴 수 있음
        if 'mvn' in command:
            timeout_seconds = 1800
        elif 'deploy-lotto-api-aws.ps1' in command or 'deploy' in command.lower():
            timeout_seconds = 1800  # 배포 스크립트: 30분
        else:
            timeout_seconds = 300  # 기본: 5분
    else:
        timeout_seconds = timeout
    
    start_time = time.time()
    try:
        result = subprocess.run(command, shell=True, capture_output=True, text=True, encoding='utf-8', errors='ignore', timeout=timeout_seconds)
        end_time = time.time()
        duration = end_time - start_time
        
        performance_data = {
            'duration': duration,
            'return_code': result.returncode,
            'stdout_lines': len(result.stdout.splitlines()) if result.stdout else 0,
            'stderr_lines': len(result.stderr.splitlines()) if result.stderr else 0
        }
        
        if result.returncode != 0:
            error_msg = f"명령어 실행 실패 (return code: {result.returncode})"
            if ignore_errors:
                if result.stderr:
                    print(f"[STDERR] {result.stderr}")
                warnings.append(f"명령어 실패 (무시됨): {error_msg}")
                print(f"[WARNING] {error_msg} (무시됨)")
                return True, errors, warnings, performance_data
            else:
                errors.append(error_msg)
                if result.stderr:
                    print(f"[STDERR] {result.stderr}")
                    errors.append(f"stderr: {result.stderr}")
                if result.stdout:
                    print(f"[STDOUT] {result.stdout}")
                print(f"[ERROR] {error_msg}")
                return False, errors, warnings, performance_data
        
        if monitor:
            print(f"[PERFORMANCE] 실행 시간: {duration:.2f}초, 출력 라인: {performance_data['stdout_lines']}")
        
        print(f"[SUCCESS] 명령어 실행 완료")
        return True, errors, warnings, performance_data
        
    except subprocess.TimeoutExpired:
        timeout_minutes = timeout_seconds // 60
        error_msg = f"명령어 타임아웃 ({timeout_minutes}분)"
        errors.append(error_msg)
        print(f"[ERROR] {error_msg}")
        return False, errors, warnings, performance_data
    except Exception as e:
        error_msg = f"명령어 실행 중 오류: {e}"
        errors.append(error_msg)
        print(f"[ERROR] {error_msg}")
        return False, errors, warnings, performance_data

def check_prerequisites() -> tuple[bool, list[str], list[str]]:
    """사전 요구사항 확인"""
    errors = []
    warnings = []
    
    print("\n[INFO] 사전 요구사항 확인 중...")
    
    # Python 버전 확인
    python_version = sys.version_info
    print(f"[INFO] Python 버전: {sys.version}")
    if python_version.major >= 3 and python_version.minor >= 8:
        print("[SUCCESS] Python 버전 확인 완료")
    else:
        error_msg = f"Python 버전이 낮습니다: {sys.version}"
        errors.append(error_msg)
        print(f"[ERROR] {error_msg}")
    
    # Docker 확인
    try:
        result = subprocess.run(['docker', '--version'], capture_output=True, text=True)
        if result.returncode == 0:
            docker_version = result.stdout.strip()
            print(f"[INFO] Docker 버전: {docker_version}")
        else:
            error_msg = "Docker가 설치되지 않았습니다"
            errors.append(error_msg)
            print(f"[ERROR] {error_msg}")
    except Exception as e:
        error_msg = f"Docker 확인 실패: {e}"
        errors.append(error_msg)
        print(f"[ERROR] {error_msg}")
    
    return len(errors) == 0, errors, warnings

def find_latest_jar(jar_pattern: str) -> Path | None:
    """최신 JAR 파일 찾기"""
    jar_pattern_normalized = jar_pattern.replace('\\', '/')
    jar_files = list(Path('.').glob(jar_pattern_normalized))
    
    if not jar_files:
        return None
    
    existing_jars = [f for f in jar_files if f.exists()]
    if not existing_jars:
        return None
    
    latest_jar = max(existing_jars, key=lambda f: f.stat().st_mtime)
    return latest_jar

def build_frontend(monitor: bool = False) -> bool:
    """프론트엔드 빌드"""
    print(f"\n[STEP 2] 프론트엔드 빌드 중...")
    
    frontend_dir = Path("client-frontend")
    if not frontend_dir.exists():
        print(f"[ERROR] 프론트엔드 디렉토리를 찾을 수 없습니다: {frontend_dir}")
        return False
    
    # 작업 디렉토리를 client-frontend로 변경
    original_cwd = os.getcwd()
    os.chdir(frontend_dir)
    
    try:
        # npm install (node_modules가 없거나 package.json이 변경된 경우)
        print(f"[INFO] npm 의존성 설치 중...")
        npm_install_cmd = "npm install"
        success, errors, warnings, performance_data = run_command(npm_install_cmd, monitor=monitor, ignore_errors=False)
        if not success:
            print(f"[WARNING] npm install 실패 (계속 진행)")
        
        # npm run build
        print(f"[INFO] 프론트엔드 빌드 중...")
        npm_build_cmd = "npm run build"
        success, errors, warnings, performance_data = run_command(npm_build_cmd, monitor=monitor, ignore_errors=False)
        if not success:
            print(f"[FAILURE] 프론트엔드 빌드 실패")
            return False
        
        # dist 디렉토리 확인
        dist_dir = Path("dist")
        if not dist_dir.exists() or not any(dist_dir.iterdir()):
            print(f"[ERROR] 프론트엔드 빌드 결과물을 찾을 수 없습니다: {dist_dir}")
            return False
        
        print(f"[SUCCESS] 프론트엔드 빌드 완료")
        return True
    finally:
        # 원래 작업 디렉토리로 복원
        os.chdir(original_cwd)

def build_backend(monitor: bool = False) -> bool:
    """백엔드 빌드"""
    print(f"\n[STEP 3] 백엔드 빌드 중...")
    
    # 단일 모듈 프로젝트이므로 lotto-api/pom.xml을 직접 지정
    maven_build_cmd = "mvn clean package -DskipTests -f lotto-api/pom.xml"
    success, errors, warnings, performance_data = run_command(maven_build_cmd, monitor=monitor)
    if not success:
        print(f"[FAILURE] lotto-api 빌드 실패")
        return False
    
    print(f"[SUCCESS] lotto-api 빌드 완료")
    return True

def build_docker_image(local: bool = False) -> bool:
    """Docker 이미지 빌드"""
    print(f"\n[INFO] lotto-api Docker 이미지 빌드 중...")
    
    compose_path = SERVICE_CONFIG['lotto-api']['compose_path_local'] if local else SERVICE_CONFIG['lotto-api']['compose_path_aws']
    
    if not Path(compose_path).exists():
        print(f"[ERROR] Docker Compose 파일을 찾을 수 없습니다: {compose_path}")
        return False
    
    build_cmd = f"docker compose -f {compose_path} build --no-cache lotto-api"
    success, errors, warnings, performance_data = run_command(build_cmd)
    if not success:
        print(f"[ERROR] lotto-api Docker 빌드 실패")
        return False
    
    print(f"[SUCCESS] lotto-api Docker 빌드 완료")
    return True

def deploy_local() -> bool:
    """로컬 배포"""
    print(f"\n[STEP 5] 로컬 배포 중...")
    
    compose_path = SERVICE_CONFIG['lotto-api']['compose_path_local']
    container_name = SERVICE_CONFIG['lotto-api']['container']
    # 로컬에서는 8083 포트로 매핑됨 (8080-8082는 다른 서비스 사용 중)
    local_port = 8083
    health_endpoint = f"http://localhost:{local_port}{SERVICE_CONFIG['lotto-api']['health_endpoint']}"
    
    # 기존 컨테이너 정리 (포트 충돌 방지)
    print(f"[INFO] 기존 컨테이너 정리 중...")
    cleanup_cmd = f"docker compose -f {compose_path} down"
    run_command(cleanup_cmd, ignore_errors=True)
    
    # Docker Compose 재시작 (postgres와 lotto-api 모두)
    print(f"[INFO] Docker Compose 서비스 시작 중...")
    recreate_cmd = f"docker compose -f {compose_path} up -d --force-recreate"
    success, errors, warnings, performance_data = run_command(recreate_cmd)
    if not success:
        print(f"[ERROR] lotto-api 재시작 실패")
        return False
    
    print(f"[SUCCESS] lotto-api 재시작 완료")
    
    # 헬스체크 대기
    print(f"[INFO] lotto-api 헬스체크 대기 중...")
    health = wait_for_container_healthy(container_name, health_endpoint, timeout=60)
    
    if health:
        print(f"[SUCCESS] lotto-api 헬스체크 완료")
        print(f"[INFO] 접속 정보:")
        print(f"   - API: http://localhost:{local_port}/lotto/api/v1/")
        print(f"   - Health: http://localhost:{local_port}/lotto/actuator/health")
        print(f"   - Frontend: http://localhost:{local_port}/lotto/")
        print(f"   - PostgreSQL: localhost:5434 (내부: postgres:5432)")
        return True
    else:
        print(f"[WARNING] lotto-api 헬스체크 실패")
        print_docker_logs(container_name)
        return False

def deploy_aws() -> bool:
    """AWS 배포"""
    print(f"\n[INFO] lotto-api AWS 배포 중...")
    
    # PowerShell 배포 스크립트 실행
    script_path = Path(__file__).parent / "deploy-lotto-api-aws.ps1"
    
    if not script_path.exists():
        print(f"[ERROR] 배포 스크립트를 찾을 수 없습니다: {script_path}")
        return False
    
    print(f"[INFO] 배포 스크립트 실행: {script_path}")
    print(f"[INFO] 배포는 SSH 연결, 파일 복사, Docker 빌드 등으로 시간이 오래 걸릴 수 있습니다 (최대 30분)")
    
    # PowerShell 스크립트 실행 (타임아웃 30분)
    if sys.platform == "win32":
        # Windows에서 PowerShell 실행
        cmd = f'powershell -ExecutionPolicy Bypass -File "{script_path}"'
    else:
        # Linux/Mac에서는 pwsh 사용 (PowerShell Core)
        cmd = f'pwsh -File "{script_path}"'
    
    success, errors, warnings, performance_data = run_command(cmd, monitor=True, timeout=1800)
    
    if not success:
        print(f"[FAILURE] AWS 배포 실패")
        if errors:
            print(f"[ERROR] 오류 상세:")
            for error in errors:
                print(f"  - {error}")
        return False
    
    print(f"[SUCCESS] AWS 배포 완료")
    return True

def wait_for_container_healthy(container_name: str, health_endpoint: str = None, timeout: int = 120):
    """컨테이너 헬스체크 대기"""
    start = time.time()
    retry_count = 0
    max_retries = 60  # 최대 재시도 횟수 증가 (애플리케이션 시작 시간 고려)
    
    while retry_count < max_retries:
        retry_count += 1
        elapsed = time.time() - start
        if elapsed > timeout:
            print(f"[ERROR] 헬스체크 타임아웃 ({timeout}초)")
            return False
        
        # 컨테이너 상태 확인
        try:
            result = subprocess.run(['docker', 'ps', '--filter', f'name={container_name}', '--format', '{{.Status}}'], 
                                  capture_output=True, text=True, timeout=5)
            status = result.stdout.strip()
            
            # 컨테이너가 실행 중이면 헬스체크 엔드포인트 확인
            if 'Up' in status:
                if health_endpoint:
                    try:
                        resp = requests.get(health_endpoint, timeout=3)
                        # 200 (정상) 또는 401 (인증 필요하지만 서버는 정상 동작 중) 모두 정상으로 처리
                        if resp.status_code == 200 or resp.status_code == 401:
                            if resp.status_code == 200:
                                print(f"[SUCCESS] 헬스체크 통과: {health_endpoint}")
                            else:
                                print(f"[SUCCESS] 헬스체크 통과 (인증 필요하지만 서버 정상 동작): {health_endpoint}")
                            return True
                        else:
                            if retry_count % 5 == 0:  # 5회마다 진행 상황 출력
                                print(f"[WAIT] 헬스체크 대기 중... (시도 {retry_count}/{max_retries}, 상태 코드: {resp.status_code})")
                    except requests.exceptions.ConnectionError:
                        if retry_count % 5 == 0:  # 5회마다 진행 상황 출력
                            print(f"[WAIT] 헬스체크 엔드포인트 연결 대기 중... (시도 {retry_count}/{max_retries})")
                    except Exception as e:
                        if retry_count < max_retries:
                            if retry_count % 5 == 0:
                                print(f"[WAIT] 헬스체크 오류 (재시도 중): {e}")
                        else:
                            print(f"[ERROR] 헬스체크 엔드포인트 연결 실패: {e}")
                            return False
                else:
                    # 헬스체크 엔드포인트가 없으면 컨테이너 상태만 확인
                    if 'healthy' in status.lower() or 'Up' in status:
                        print(f"[SUCCESS] 컨테이너 상태 확인 완료")
                        return True
        except Exception as e:
            if retry_count < max_retries:
                if retry_count % 5 == 0:
                    print(f"[WAIT] 컨테이너 상태 확인 중... (시도 {retry_count}/{max_retries})")
            else:
                print(f"[ERROR] 컨테이너 상태 확인 실패: {e}")
                return False
        
        time.sleep(2)
    
    print(f"[ERROR] 헬스체크 실패: 최대 재시도 횟수({max_retries}) 초과")
    return False

def print_docker_logs(container_name: str):
    """Docker 컨테이너 로그 출력"""
    print(f"\n[INFO] {container_name} 컨테이너 로그:")
    log_cmd = f"docker logs --tail=50 {container_name}"
    run_command(log_cmd, ignore_errors=True)

def run_service_automation(service_name: str, monitor: bool = False, stage: str = 'all', local: bool = False) -> bool:
    """서비스 자동화 실행"""
    print(f"\n[INFO] === {service_name} 자동화 시작 ===")
    
    # 1: 사전 요구사항 확인
    print(f"\n[STEP 1] 사전 요구사항 확인")
    if not check_prerequisites()[0]:
        print(f"[FAILURE] {service_name} 사전 요구사항 확인 실패")
        return False
    
    # 2: 프론트엔드 빌드
    if stage in ['build', 'frontend-build', 'all', 'aws-deploy']:
        if not build_frontend(monitor):
            print(f"[FAILURE] {service_name} 프론트엔드 빌드 실패")
            return False
    
    # frontend-build는 여기서 종료
    if stage == 'frontend-build':
        print(f"\n[SUCCESS] {service_name} 프론트엔드 빌드 완료")
        return True
    
    # 3: 백엔드 빌드
    if stage in ['build', 'backend-build', 'all', 'aws-deploy']:
        if not build_backend(monitor):
            print(f"[FAILURE] {service_name} 백엔드 빌드 실패")
            return False
    
    # backend-build는 여기서 종료
    if stage == 'backend-build':
        print(f"\n[SUCCESS] {service_name} 백엔드 빌드 완료")
        return True
    
    # 4: Docker 이미지 빌드
    if stage in ['docker-rebuild', 'all', 'deploy', 'aws-deploy']:
        if not build_docker_image(local):
            print(f"[FAILURE] {service_name} Docker 빌드 실패")
            return False
    
    # 5: 배포
    if stage in ['deploy', 'all']:
        if local:
            if not deploy_local():
                print(f"[FAILURE] {service_name} 로컬 배포 실패")
                return False
        else:
            print(f"[INFO] 배포 모드가 지정되지 않았습니다. --local 또는 --stage aws-deploy를 사용하세요.")
    
    # 6: AWS 배포
    if stage == 'aws-deploy':
        if not deploy_aws():
            print(f"[FAILURE] {service_name} AWS 배포 실패")
            return False
    
    print(f"[SUCCESS] {service_name} 자동화 완료")
    return True

def main():
    parser = argparse.ArgumentParser(description='Lotto Guide Platform 자동화 스크립트')
    parser.add_argument('--service', choices=['lotto-api'], 
                       default='lotto-api', help='서비스 선택 (기본값: lotto-api)')
    parser.add_argument('--stage', choices=['build', 'deploy', 'frontend-build', 'backend-build', 'docker-rebuild', 'verify', 'all', 'aws-deploy'], 
                       default='all', help='실행 단계')
    parser.add_argument('--skip-prerequisites', action='store_true', 
                       help='사전 요구사항 확인 건너뛰기')
    parser.add_argument('--monitor', action='store_true', 
                       help='성능 모니터링 활성화')
    parser.add_argument('--local', action='store_true',
                       help='로컬 배포 모드 (localhost Docker)')
    args = parser.parse_args()
    
    automation_id = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    print(f"\n[INFO] === Lotto Guide Platform 자동화 (Automation ID: {automation_id}) ===")
    print(f"[INFO] 서비스: {args.service}")
    print(f"[INFO] 단계: {args.stage}")
    print(f"[INFO] 모드: {'로컬' if args.local else 'AWS' if args.stage == 'aws-deploy' else '기본'}")
    
    start_time = time.time()
    
    if not args.skip_prerequisites:
        prereq_success, errors, warnings = check_prerequisites()
        if not prereq_success:
            print(f"\n[FAILURE] 사전 요구사항 확인 실패")
            return False
    
    service_success = run_service_automation(args.service, args.monitor, args.stage, args.local)
    if not service_success:
        print(f"\n[FAILURE] {args.service} 자동화 실패")
        return False
    
    total_duration = time.time() - start_time
    print(f"\n[SUCCESS] Lotto Guide Platform 자동화 완료! (소요 시간: {total_duration:.2f}초)")
    return True

if __name__ == '__main__':
    success = main()
    sys.exit(0 if success else 1)
