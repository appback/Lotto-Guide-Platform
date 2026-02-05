# AI 작업 가이드

> **⚠️ 중요: 작업 시작 전 반드시 [`.ai-config.json`](../.ai-config.json)을 먼저 확인하세요.**

## 📋 핵심 원칙

**`.ai-config.json`이 모든 정책의 단일 진실 소스(Single Source of Truth)입니다.**

이 문서는 `.ai-config.json`으로의 접근 경로를 제공하는 지침서입니다.

## 🚀 작업 시작 절차

1. **`.ai-config.json` 확인** (필수)
   - 모든 정책, 가이드라인, 금지사항이 여기에 정의되어 있습니다.
   - `ai_guidelines.core_policies` 섹션: 핵심 정책 및 금지사항
   - `document_hierarchy` 섹션: 문서 계층 구조 및 검색 가이드
   - `document_search_guide` 섹션: 문서 검색 방법 및 빠른 참조

2. **작업 유형별 필수 문서 확인**
   - 코딩 문제 → `docs/guidelines/coding-standards.md`
   - 로깅 문제 → `docs/guidelines/logging-policy.md`
   - 빌드 문제 → `docs/guidelines/build-management.md`
   - 환경 설정 → `docs/guidelines/environment-configuration.md`

3. **프로젝트 구조 확인**
   - `working/README.md`: 작업 진행 상황
   - `docs/docs_prototype_mvp_architecture_appback_v1.2.md`: 아키텍처 설계

## 📚 문서 구조

> **상세한 문서 계층 구조**: `.ai-config.json`의 `document_hierarchy` 섹션 참조

### 핵심 문서
- **`.ai-config.json`**: 중앙 정책 저장소 (모든 정책의 단일 진실 소스)
- **`docs/index.md`**: 전체 문서 구조 안내
- **`working/README.md`**: 작업 진행 상황

### 주요 분류
- `docs/guidelines/`: 프로젝트/기술 가이드라인
- `docs/`: 설계 문서
- `working/`: 작업 중인 문서

## 🔍 빠른 참조

### 작업 유형별 필수 문서
- **코딩 표준**: `docs/guidelines/coding-standards.md`
- **로깅 정책**: `docs/guidelines/logging-policy.md`
- **빌드 관리**: `docs/guidelines/build-management.md`
- **PowerShell 가이드**: `docs/guidelines/powershell-bash-command-guide.md`
- **Git 가이드**: `docs/guidelines/git-command-guide.md`
- **환경 설정**: `docs/guidelines/environment-configuration.md`

## ⚠️ 절대 금지사항

> **상세한 금지사항**: `.ai-config.json`의 `ai_guidelines.core_policies.prohibited_behaviors` 섹션 참조

### 핵심 금지사항 요약
- **Git 명령어**: 사용자 명시적 지시 없이 자동 실행 금지
- **빌드/배포**: 프로젝트 루트에서만 실행
- **PowerShell 환경**: Bash 명령어 사용 금지

## 🔧 빌드 및 테스트

> **상세한 빌드 정책**: `.ai-config.json`의 `build_and_test` 섹션 참조

### 핵심 원칙
- **프로젝트 루트에서만 실행**: 하위 디렉토리 이동 금지
- **Maven 빌드**: `mvn clean package` 사용

### 빠른 실행
```powershell
# 프로젝트 루트에서 실행
cd C:\Projects\Lotto-Guide-Platform
mvn clean package
mvn spring-boot:run
```

## 📖 추가 문서

### 아키텍처
- **설계 문서**: `docs/docs_prototype_mvp_architecture_appback_v1.2.md`
- **작업 문서**: `working/README.md`

### 문서 구조
- **전체 문서 구조**: `docs/index.md`
- **문서 계층 구조**: `.ai-config.json`의 `document_hierarchy` 섹션

---

**버전**: 1.0.0  
**최종 업데이트**: 2026-01-09  
**작성자**: Lotto Guide Platform Development Team
