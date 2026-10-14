#!/usr/bin/env bash
# 냥멍케어 백엔드 개발 환경 설치. 맥은 터미널, Windows는 Git Bash에서 `bash setup.sh`로 실행한다.
# 이미 맞는 버전이 있으면 건너뛴다. 여러 번 실행해도 된다.
# 프론트 저장소의 setup.sh와 공통 부분(맨 위 함수와 "공통" 절)이 같다. 고칠 때 둘 다 고친다.
set -euo pipefail
cd "$(dirname "$0")"

TODO=0
NEED_RESTART=0
ok()   { printf '  [완료] %s\n' "$1"; }
skip() { printf '  [있음] %s\n' "$1"; }
todo() { printf '  [직접] %s\n' "$1"; TODO=$((TODO + 1)); }
die()  { printf '\n[중단] %s\n' "$1" >&2; exit 1; }
step() { printf '\n== %s\n' "$1"; }

case "$(uname -s)" in
  Darwin) OS=mac ;;
  MINGW* | MSYS*) OS=win ;;
  *) die "지원하지 않는 환경이다: $(uname -s). 맥 터미널이나 Windows Git Bash에서 실행한다." ;;
esac

# 패키지 설치. 맥은 Homebrew, Windows는 winget
# pkg <brew 이름> <formula|cask> <winget ID> [winget 버전]
pkg() {
  if [ "$OS" = mac ]; then
    command -v brew >/dev/null || die "Homebrew가 없다. https://brew.sh 의 설치 명령을 먼저 실행한다."
    if [ "$2" = cask ]; then brew install --cask "$1"; else brew install "$1"; fi
  else
    command -v winget.exe >/dev/null || die "winget이 없다. Microsoft Store에서 '앱 설치 관리자'를 설치한다."
    if [ -n "${4:-}" ]; then
      winget.exe install --id "$3" -e --version "$4" --accept-package-agreements --accept-source-agreements
    else
      winget.exe install --id "$3" -e --accept-package-agreements --accept-source-agreements
    fi
    # winget이 바꾼 PATH는 새 창에서만 적용된다
    NEED_RESTART=1
  fi
}

# VS Code 명령줄 도구 위치. 맥은 PATH에 없으면 앱 안의 것을 쓴다
vscode_cli() {
  if command -v code >/dev/null; then command -v code; return; fi
  if [ "$OS" = mac ]; then
    local app
    app=$(mdfind "kMDItemCFBundleIdentifier == 'com.microsoft.VSCode'" 2>/dev/null | head -1)
    [ -n "$app" ] && [ -x "$app/Contents/Resources/app/bin/code" ] && echo "$app/Contents/Resources/app/bin/code"
  fi
  return 0
}

step "공통"

command -v git >/dev/null || die "Git이 없다. Windows는 Git for Windows를 설치하고 Git Bash에서 다시 실행한다."
skip "Git $(git --version | awk '{print $3}')"

if command -v claude >/dev/null; then
  skip "Claude Code $(claude --version 2>/dev/null | awk '{print $1}')"
else
  todo "Claude Code 설치. 맥: curl -fsSL https://claude.ai/install.sh | bash / Windows PowerShell: irm https://claude.ai/install.ps1 | iex"
fi

CODE=$(vscode_cli)
if [ -z "$CODE" ]; then
  todo "VS Code 설치 뒤 다시 실행한다(확장 설치를 위해)."
else
  installed=$("$CODE" --list-extensions | tr '[:upper:]' '[:lower:]')
  for ext in $(grep -o '"[^"]*\.[^"]*"' .vscode/extensions.json | tr -d '"'); do
    if printf '%s\n' "$installed" | grep -qx "$(printf '%s' "$ext" | tr '[:upper:]' '[:lower:]')"; then
      skip "VS Code 확장 $ext"
    else
      "$CODE" --install-extension "$ext" >/dev/null && ok "VS Code 확장 $ext"
    fi
  done
fi

# 전역 git 설정(회사 계정 등)은 건드리지 않고 이 저장소에만 둔다
for key in user.name user.email; do
  if [ -n "$(git config --local "$key" || true)" ]; then
    skip "git $key = $(git config --local "$key")"
  else
    read -rp "  이 저장소에 쓸 git $key: " value
    git config --local "$key" "$value"
    ok "git $key = $value"
  fi
done

# 개인 브랜치 이름 = 이니셜. 핸드오프 파일 이름과 커밋 접두사가 여기서 나온다
branch=$(git branch --show-current)
case "$branch" in
  "" | main | master | develop)
    read -rp "  개인 브랜치로 쓸 이니셜(대문자, 예: LJS): " initials
    if git show-ref --verify --quiet "refs/heads/$initials"; then
      git checkout "$initials"
    elif git ls-remote --exit-code --heads origin "$initials" >/dev/null 2>&1; then
      git fetch origin "$initials" && git checkout -b "$initials" --track "origin/$initials"
    else
      git checkout -b "$initials"
    fi
    ok "브랜치 $initials"
    ;;
  *) skip "브랜치 $branch" ;;
esac

step "백엔드"

# JDK 21: build.gradle의 toolchain 버전
if [ "$OS" = mac ] && /usr/libexec/java_home -v 21 >/dev/null 2>&1; then
  skip "JDK 21 ($(/usr/libexec/java_home -v 21))"
elif [ "$OS" = win ] && ls -d "/c/Program Files/Eclipse Adoptium/jdk-21"* >/dev/null 2>&1; then
  skip "JDK 21"
else
  pkg temurin@21 cask EclipseAdoptium.Temurin.21.JDK
  ok "JDK 21"
fi

# MySQL 8: 로컬 개발 DB
mysql_bin=""
for m in mysql /opt/homebrew/opt/mysql@8.4/bin/mysql /usr/local/opt/mysql@8.4/bin/mysql /usr/local/mysql/bin/mysql; do
  if command -v "$m" >/dev/null 2>&1; then mysql_bin=$(command -v "$m"); break; fi
done
if [ "$OS" = win ] && [ -z "$mysql_bin" ]; then
  for m in "/c/Program Files/MySQL/MySQL Server 8."*/bin/mysql.exe; do [ -x "$m" ] && mysql_bin="$m"; done
fi
if [ -n "$mysql_bin" ] && "$mysql_bin" --version | grep -q ' 8\.'; then
  skip "MySQL $("$mysql_bin" --version | grep -o ' 8\.[0-9.]*' | tr -d ' ')"
else
  pkg mysql@8.4 formula Oracle.MySQL 8.4.9
  ok "MySQL 8.4"
  if [ "$OS" = mac ]; then
    todo "MySQL 시작: brew services start mysql@8.4"
  else
    todo "MySQL 초기 설정(서비스 등록, root 비밀번호)을 MySQL 설치 안내대로 한다"
  fi
fi

# DB 접속 정보는 환경변수로만 넣는다(README)
for v in DB_URL DB_USERNAME DB_PASSWORD; do
  if [ -n "${!v:-}" ]; then skip "환경변수 $v"; else todo "환경변수 $v 설정(README의 '실행' 절)"; fi
done

if [ "$NEED_RESTART" = 1 ]; then
  printf '\n[다시 실행] 새로 설치한 프로그램은 새 창에서만 잡힌다. Git Bash를 닫았다가 열고 bash setup.sh를 한 번 더 실행한다.\n'
  exit 2
fi

step "확인"
./gradlew build -q && ok "./gradlew build"

printf '\n끝. 직접 할 일 %d개(위 [직접] 줄).\n' "$TODO"
