# Lotto Guide Platform 인프라 문서

> **최종 업데이트**: 2026-01-09

---

## 📋 문서 구조

`infrastructure` 폴더는 **AWS 인프라 구성, 상태, 접근 방법**을 다루는 문서들을 포함합니다.

### 핵심 문서

#### **EC2 인스턴스**
- **[lotto-api-instance-info.md](lotto-api-instance-info.md)**: Lotto Guide Platform API 인스턴스 정보
  - 인스턴스 기본 정보 및 사양
  - 네트워크 구성
  - 보안 정보
  - 접속 방법 (SSM/CloudShell)
  - 배포 프로세스
  - 문제 해결 가이드

---

## 🎯 문서 사용 가이드

### 인프라 상태 확인 시
1. **EC2 인스턴스**: `lotto-api-instance-info.md` 참조
2. **인스턴스 접속**: SSM 또는 CloudShell 사용

### 배포 시
1. **인스턴스 정보**: `lotto-api-instance-info.md`의 배포 프로세스 참조
2. **환경 변수**: SSM Parameter Store 확인

### 인프라 문제 해결 시
1. **접속 문제**: `lotto-api-instance-info.md`의 문제 해결 섹션 참조
2. **서비스 문제**: 로그 확인 및 환경 변수 검증

---

## 📝 문서 관리 원칙

1. **인프라 중심**: AWS 리소스 구성 및 상태 문서만 포함
2. **실무 가이드**: 접근 방법, 확인 명령어, 문제 해결 방법 제공
3. **최신성 유지**: 인프라 변경 시 즉시 문서 업데이트
4. **보안 주의**: 접속 정보, 비밀번호 등 민감 정보 주의

---

## 🔗 관련 문서

### 배포 및 운영
- **[배포 가이드라인](../guidelines/deployment-guidelines.md)**: 배포 정책 및 전략
- **[환경 설정](../guidelines/environment-configuration.md)**: 환경별 설정 가이드

### 설계 문서
- **[프로젝트 아키텍처](../docs_prototype_mvp_architecture_appback_v1.2.md)**: 전체 시스템 설계

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2026-01-09  
**작성자**: Lotto Guide Platform Development Team
