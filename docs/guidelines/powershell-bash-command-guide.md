# PowerShell 환경에서 Bash 명령어 사용 금지 가이드

> **목적**: Windows PowerShell 환경에서 작업 시 Bash/Linux 명령어 사용을 금지하고, PowerShell 명령어만 사용하도록 하는 지침

**문서 버전**: 1.0  
**최종 업데이트**: 2026-01-09  
**작성자**: Lotto Guide Platform Development Team

---

## 🚫 핵심 정책

**PowerShell 환경에서는 Bash 명령어를 절대 사용하지 않는다.**

- AI는 사용자의 환경이 PowerShell인 경우, Bash 명령어를 사용하지 않아야 함
- 모든 명령어는 PowerShell 문법으로 작성해야 함
- Bash 명령어를 PowerShell로 변환하는 것이 아니라, 처음부터 PowerShell 명령어로 작성해야 함

---

## 금지 사항

### 1. Bash 명령어 사용 금지

```powershell
# ❌ 금지: Bash 명령어
cd project && npm run build
export JAVA_HOME=/path/to/java
cat file.txt
grep "pattern" file.txt
```

```powershell
# ✅ 올바른 방법: PowerShell 명령어
cd project; if ($?) { npm run build }
$env:JAVA_HOME = "C:\path\to\java"
Get-Content file.txt
Select-String "pattern" file.txt
```

### 2. Bash 스타일 경로 사용 금지

```powershell
# ❌ 금지: Unix 스타일 경로
cd /path/to/dir
export PATH=$PATH:/new/path
```

```powershell
# ✅ 올바른 방법: Windows 경로
cd C:\path\to\dir
$env:Path += ";C:\new\path"
```

### 3. Bash 스타일 환경 변수 사용 금지

```powershell
# ❌ 금지: Bash 스타일
export VAR=value
echo $VAR
```

```powershell
# ✅ 올바른 방법: PowerShell 스타일
$env:VAR = "value"
echo $env:VAR
```

### 4. Bash 스타일 조건문 사용 금지

```powershell
# ❌ 금지: Bash 스타일
if [ -f file ]; then
```

```powershell
# ✅ 올바른 방법: PowerShell 스타일
if (Test-Path file) {
```

---

## PowerShell 명령어 사용 규칙

### 1. 명령어 체이닝

PowerShell 5.x에서는 `&&` 연산자를 사용할 수 없습니다. 반드시 PowerShell 문법을 사용해야 합니다.

```powershell
# ❌ 금지: Bash 스타일
cd project && npm run build

# ✅ 올바른 방법: PowerShell 5.x
cd project; if ($?) { npm run build }

# ✅ 올바른 방법: PowerShell 7+ (가능하지만 일관성을 위해 세미콜론 사용 권장)
cd project && npm run build
```

### 2. 경로 처리

모든 경로는 Windows 형식으로 작성하고, 공백이 있는 경우 따옴표로 감싸야 합니다.

```powershell
# ✅ 올바른 방법
cd "C:\Program Files\Git\bin"
& "C:\Program Files\Git\bin\git.exe" status
```

### 3. 환경 변수

PowerShell 환경 변수는 `$env:` 접두사를 사용합니다.

```powershell
# ✅ 올바른 방법
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path += ";C:\new\path"
echo $env:JAVA_HOME
```

### 4. 파일 조작

PowerShell cmdlet을 사용합니다.

```powershell
# ✅ 올바른 방법
Get-Content file.txt          # cat 대신
Get-Content file.txt -TotalCount 10  # head 대신
Get-Content file.txt -Tail 10        # tail 대신
Select-String "pattern" file.txt      # grep 대신
```

### 5. Git 명령어

Git 명령어는 전체 경로를 사용하거나 PATH에 Git이 포함되어 있어야 합니다.

```powershell
# ✅ 올바른 방법
& "C:\Program Files\Git\bin\git.exe" status
& "C:\Program Files\Git\bin\git.exe" add .
& "C:\Program Files\Git\bin\git.exe" commit -m "message"
```

---

## 자동화 스크립트 작성 규칙

### 프로젝트 루트에서 실행

```powershell
# ✅ 올바른 방법: 프로젝트 루트에서 실행
cd C:\Projects\Lotto-Guide-Platform
mvn clean package

# ❌ 금지: 하위 폴더로 이동 후 실행
cd lotto-api
mvn clean package
```

### 빌드 명령어

```powershell
# ✅ 올바른 방법: PowerShell 명령어
cd C:\Projects\Lotto-Guide-Platform
mvn -f lotto-api/pom.xml clean package
```

---

## AI 작업 시 체크리스트

AI가 PowerShell 환경에서 작업할 때 다음을 확인해야 합니다:

- [ ] Bash 명령어(`&&`, `export`, `cat`, `grep` 등)를 사용하지 않았는가?
- [ ] 모든 경로가 Windows 형식(`C:\path`)인가?
- [ ] 환경 변수가 PowerShell 형식(`$env:VAR`)인가?
- [ ] 파일 조작이 PowerShell cmdlet(`Get-Content`, `Select-String` 등)을 사용하는가?
- [ ] 명령어 체이닝이 PowerShell 문법(`; if ($?) { }`)을 사용하는가?

---

## 예외 사항

### Git Bash 사용 시

Git Bash를 명시적으로 사용하는 경우에만 Bash 명령어를 사용할 수 있습니다.

```powershell
# Git Bash 명시적 사용
& "C:\Program Files\Git\bin\bash.exe" -c "cd project && npm run build"
```

하지만 가능하면 PowerShell 명령어로 변환하는 것을 권장합니다.

---

## 관련 문서

- `.ai-config.json` - AI 정책 및 가이드
- `docs/guidelines/automation-principles.md` - 자동화 원칙
- `docs/guidelines/build-management.md` - 빌드 관리 가이드

---

**문서 버전**: 1.0  
**최종 업데이트**: 2026-01-09  
**작성자**: Lotto Guide Platform Development Team
