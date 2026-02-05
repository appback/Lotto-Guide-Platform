# Lotto Guide Platform API 인스턴스 정보

## 📋 기본 정보

### **인스턴스 식별**
- **인스턴스 ID**: `i-039650bd0704f2e6f` (고정)
- **인스턴스 이름**: `dadp-engine-2` (공유 인스턴스)
- **환경**: `production`
- **목적**: `lotto-api-deployment` (프로세싱 파워 여유 활용)

### **AWS 계정 정보**
- **계정 ID**: `834873515944` (고정)
- **리전**: `ap-northeast-2` (서울) (고정)
- **가용 영역**: 확인 필요
- **IAM 역할**: `dadp-ec2-ssm-role` (고정)

## 🌐 네트워크 정보

### **IP 주소**
- **프라이빗 IP**: `172.31.26.161` (고정)
- **프라이빗 DNS**: `ip-172-31-26-161.ap-northeast-2.compute.internal` (고정)
- **퍼블릭 IP**: `15.164.228.217` (변동될 수 있음)
- **퍼블릭 DNS**: `ec2-15-164-228-217.ap-northeast-2.compute.amazonaws.com` (변동될 수 있음)

### **네트워크 구성**
- **VPC ID**: `vpc-f8168890` (고정)
- **서브넷 ID**: `subnet-74f72e38` (고정)
- **보안 그룹**: `sg-095f85d6a1e86a41f` (launch-wizard-38) (고정)

## 💻 인스턴스 사양

### **하드웨어 사양**
- **인스턴스 타입**: `t2.medium` (고정)
- **아키텍처**: `x86_64` (고정)
- **CPU**: 2 vCPU (고정)
- **메모리**: 4GB (고정)
- **스토리지**: EBS (고정)
- **프로세싱 파워**: 여유 있음 (Lotto Guide Platform 배포에 적합)

### **시작 시간**
- **시작 시간**: `Wed Oct 29 2025 16:30:28 GMT+0900`

## 🔒 보안 정보

### **보안 그룹 규칙**

#### **인바운드 규칙**
| 포트 | 프로토콜 | 소스 | 설명 |
|------|----------|------|------|
| 22 | TCP | 0.0.0.0/0 | SSH 접근 (임시, SSM 사용 권장) |
| 8080 | TCP | 필요시 설정 | Lotto API 서비스 |

#### **아웃바운드 규칙**
- **전체 트래픽 허용**

### **키 페어**
- **키 이름**: `dadp-prod` (고정)
- **키 파일**: `C:\Projects\dadp\dadp-prod.pem` (참조용, SSM 사용 권장)

## 🚀 서비스 구성

### **Lotto Guide Platform 서비스**
- **Backend API**: 포트 `8080`
- **컨테이너**: Docker 사용
- **컨텍스트 패스**: `/api/v1/`

### **공유 인프라**
- **인스턴스**: dadp-engine-2와 공유 (프로세싱 파워 여유 활용)
- **네트워크**: 동일 VPC 내에서 통신

## 📁 디렉토리 구조

### **작업 디렉토리** (예상)
- **메인 디렉토리**: `/opt/lotto/docker` 또는 `/home/ec2-user/docker`
- **로그 디렉토리**: `/var/log/lotto` (확인 필요)
- **설정 디렉토리**: `/etc/lotto` (확인 필요)
- **환경 변수 파일**: `/etc/lotto-api.env`

## 🔧 접속 정보

### **SSM 접속** (권장)
```bash
# AWS Systems Manager Session Manager 사용
aws ssm start-session --target i-039650bd0704f2e6f --region ap-northeast-2
```

### **CloudShell 접속**
```bash
# AWS 콘솔 → CloudShell 실행 후
cd /opt/lotto/docker || cd /home/ec2-user/docker
docker-compose ps
docker-compose logs -f lotto-api
```

### **SSH 접속** (비권장, SSM 사용 권장)
```bash
# SSH 접속 명령어 (퍼블릭 DNS 사용)
ssh -i "dadp-prod.pem" ec2-user@ec2-15-164-228-217.ap-northeast-2.compute.amazonaws.com

# 또는 퍼블릭 IP 사용
ssh -i "dadp-prod.pem" ec2-user@15.164.228.217
```

### **서비스 접속**
- **API 서비스**: `http://15.164.228.217:8080/api/v1/` (변동될 수 있음)
- **헬스체크**: `http://15.164.228.217:8080/actuator/health`

## ⚙️ 환경 변수 설정

### **SSM Parameter Store 키**
```
/lotto/api/DB_HOST
/lotto/api/DB_PORT
/lotto/api/DB_NAME
/lotto/api/DB_USERNAME
/lotto/api/DB_PASSWORD (SecureString)
/lotto/api/SPRING_PROFILES_ACTIVE
```

### **env-sync 스크립트**
```bash
# 환경 변수 동기화
sudo /usr/local/bin/lotto-api-env-sync.sh

# 서비스 재시작
sudo systemctl restart lotto-api
```

## 📋 배포 체크리스트

### **초기 설정 필요**
- [ ] 인스턴스 접속 확인 (SSM)
- [ ] Docker 설치 확인
- [ ] 환경 변수 설정 확인 (SSM Parameter Store)
- [ ] env-sync 스크립트 배포
- [ ] Docker Compose 설정
- [ ] Lotto API 서비스 배포
- [ ] 헬스체크 통과

### **배포 준비**
- [ ] Backend JAR 빌드
- [ ] Docker 이미지 빌드
- [ ] ECR 푸시 (선택)
- [ ] 환경 변수 동기화
- [ ] Docker Compose 재시작
- [ ] 헬스체크 확인

## ⚠️ 주의사항

### **인스턴스 상태**
- **인스턴스 상태**: 실행 중 ✅
- **IMDSv2**: Required
- **관리형**: false
- **공유 인스턴스**: dadp-engine-2와 공유

### **보안 주의사항**
- **SSH 접근**: 임시로 0.0.0.0/0 허용 (SSM 사용 권장)
- **원격 접근**: SSM 또는 CloudShell만 사용 (SSH 금지)
- **아웃바운드**: 모든 통신 허용 ✅

### **프로세싱 파워**
- **현재 상태**: 여유 있음
- **Lotto Guide Platform 배포**: 적합
- **공유 사용**: dadp-engine-2와 함께 사용

## 🔄 배포 프로세스

### **1단계: 로컬 빌드**
```powershell
# 프로젝트 루트에서 실행
cd C:\Projects\Lotto-Guide-Platform
mvn clean package
docker build -t lotto-api:latest -f lotto-api/Dockerfile lotto-api/
```

### **2단계: 이미지 전송 (선택)**
```bash
# ECR에 푸시 (ECR 설정 후)
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin <ECR_URI>
docker tag lotto-api:latest <ECR_URI>/lotto-api:latest
docker push <ECR_URI>/lotto-api:latest
```

### **3단계: AWS 인스턴스 배포**
```bash
# SSM 또는 CloudShell에서 실행
cd /opt/lotto/docker || cd /home/ec2-user/docker

# 환경 변수 동기화
sudo /usr/local/bin/lotto-api-env-sync.sh

# Docker Compose 재시작
docker-compose -f docker-compose.aws.yml up -d --force-recreate lotto-api

# 로그 확인
docker-compose -f docker-compose.aws.yml logs -f lotto-api
```

## 📞 문제 해결

### **일반적인 문제들**

1. **인스턴스 접속 실패**
   - SSM Agent 상태 확인
   - IAM 역할 확인 (`dadp-ec2-ssm-role`)
   - 보안 그룹 확인

2. **서비스 시작 실패**
   - 로그 확인: `docker-compose logs lotto-api`
   - 환경 변수 확인: `cat /etc/lotto-api.env`
   - 포트 충돌 확인: `netstat -tulpn | grep 8080`

3. **데이터베이스 연결 실패**
   - RDS 보안 그룹 확인
   - SSM 파라미터 확인
   - 네트워크 연결 확인

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2026-01-09  
**작성자**: Lotto Guide Platform Development Team
