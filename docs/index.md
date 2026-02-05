# Lotto Guide Platform 프로젝트 문서

> **⚠️ 중요: 문서 구조 및 검색 가이드는 [`.ai-config.json`](../.ai-config.json)의 `document_hierarchy` 및 `document_search_guide` 섹션을 참조하세요.**

## 📋 핵심 문서

### AI 작업 시작점
1. **[`.ai-config.json`](../.ai-config.json)** ⭐ **가장 먼저 확인** - 중앙 정책 저장소 (모든 정책의 단일 진실 소스)
2. **[AI 작업 가이드](ai-guide.md)** - `.ai-config.json` 접근 지침서
3. **[프로젝트 아키텍처](docs_prototype_mvp_architecture_appback_v1.2.md)** - MVP 아키텍처 설계 문서

## 📁 문서 구조

> **상세한 문서 계층 구조**: `.ai-config.json`의 `document_hierarchy` 섹션 참조

### 주요 분류
- **[가이드라인](guidelines/)** - 프로젝트/기술 가이드라인
- **[인프라 문서](infrastructure/)** - AWS 인프라 구성 및 인스턴스 정보
- **[작업 문서](working/)** - 작업 중인 문서 및 진행 상황
- **[설계 문서](docs_prototype_mvp_architecture_appback_v1.2.md)** - 시스템 전체 설계 문서

## 🚀 빠른 시작

### 개발자용
1. **[`.ai-config.json`](../.ai-config.json)** - 모든 정책 확인
2. **[코딩 표준](guidelines/coding-standards.md)** - 코드 작성 규칙
3. **[빌드 관리](guidelines/build-management.md)** - 빌드 작업 가이드
4. **[환경 설정](guidelines/environment-configuration.md)** - 개발 환경 설정
5. **[배포 가이드](guidelines/deployment-guidelines.md)** - Docker 및 AWS 배포 가이드
6. **[인스턴스 정보](infrastructure/lotto-api-instance-info.md)** - AWS EC2 인스턴스 정보

## 📦 프로젝트 구조

### Clean Architecture 패턴
- **api** - REST API 레이어
- **application** - UseCase 및 Port
- **domain** - 도메인 모델 및 비즈니스 로직
- **infra** - 인프라스트럭처 (Persistence, LLM, Scheduler, Security)
- **config** - 설정 클래스

## 🔍 문서 검색 가이드

> **상세한 검색 방법**: `.ai-config.json`의 `document_search_guide` 섹션 참조

### 검색 절차
1. `.ai-config.json`의 `document_hierarchy`에서 관련 분류 확인
2. `guidelines`에서 정책/가이드 문서 확인
3. `working`에서 구체적 구현 문서 확인

---

**버전**: 1.0.0  
**최종 업데이트**: 2026-01-09  
**작성자**: Lotto Guide Platform Development Team
