#!/usr/bin/env bash
# 냥멍케어 백엔드 개발 환경 설치. 맥은 터미널에서 `bash setup.sh`, Windows는 cmd·PowerShell에서 `setup.bat`(또는 Git Bash에서 `bash setup.sh`)로 실행한다.
# 이미 맞는 버전이 있으면 건너뛴다. 여러 번 실행해도 된다. `--reset-db`를 붙이면 로컬 DB를 지우고 db/의 SQL을 처음부터 다시 실행한다.
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
  *) die "지원하지 않는 환경입니다: $(uname -s). 맥은 터미널에서 bash setup.sh, Windows는 setup.bat로 실행해 주세요." ;;
esac

# 패키지 설치. 맥은 Homebrew, Windows는 winget
# pkg <brew 이름> <formula|cask> <winget ID> [winget 버전]
pkg() {
  if [ "$OS" = mac ]; then
    command -v brew >/dev/null || die "Homebrew가 없습니다. https://brew.sh 의 설치 명령을 먼저 실행해 주세요."
    if [ "$2" = cask ]; then brew install --cask "$1"; else brew install "$1"; fi
  else
    command -v winget.exe >/dev/null || die "winget이 없습니다. Microsoft Store에서 '앱 설치 관리자'를 설치해 주세요."
    if [ -n "${4:-}" ]; then
      winget.exe install --id "$3" -e --version "$4" --accept-package-agreements --accept-source-agreements
    else
      winget.exe install --id "$3" -e --accept-package-agreements --accept-source-agreements
    fi
    # winget이 바꾼 PATH는 새 창에서만 적용된다
    NEED_RESTART=1
  fi
}

# 셸 설정 파일에 한 줄을 넣는다. 이미 있으면 그대로 둔다
persist_line() {
  mkdir -p "$(dirname "$1")"
  touch "$1"
  grep -qxF "$2" "$1" || printf '\n%s\n' "$2" >> "$1"
}

# 새 창에서도 이 폴더의 명령이 잡히도록 PATH에 영구로 넣는다. 맥은 ~/.zshrc, Windows는 사용자 PATH
persist_path() {
  if [ "$OS" = mac ]; then
    persist_line "$HOME/.zshrc" "export PATH=\"$1:\$PATH\""
    return
  fi
  # %USERPROFILE% 같은 항목이 풀리지 않도록 레지스트리 값을 ExpandString으로 그대로 다시 쓴다.
  # 마지막 줄은 열려 있는 탐색기에 환경변수 변경을 알리려고 빈 변수를 지우는 것이다
  WIN_DIR="$(cygpath -w "$1")" powershell.exe -NoProfile -Command '
    $key = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey("Environment", $true)
    $parts = @(([string]$key.GetValue("Path", "", "DoNotExpandEnvironmentNames")) -split ";" | Where-Object { $_ })
    if ($parts -notcontains $env:WIN_DIR) {
      $key.SetValue("Path", (($parts + $env:WIN_DIR) -join ";"), [Microsoft.Win32.RegistryValueKind]::ExpandString)
      [Environment]::SetEnvironmentVariable("NYANGMEONG_SETUP", $null, "User")
    }'
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

command -v git >/dev/null || die "Git이 없습니다. Windows는 Git for Windows를 설치한 뒤 다시 실행해 주세요."
skip "Git $(git --version | awk '{print $3}')"

# Claude Code: 공식 설치 스크립트가 ~/.local/bin에 설치한다. 로그인은 처음 실행할 때 직접 한다
if command -v claude >/dev/null; then
  skip "Claude Code $(claude --version 2>/dev/null | awk '{print $1}')"
else
  if [ "$OS" = mac ]; then
    curl -fsSL https://claude.ai/install.sh | bash
  else
    powershell.exe -NoProfile -ExecutionPolicy Bypass -Command 'irm https://claude.ai/install.ps1 | iex'
  fi
  export PATH="$HOME/.local/bin:$PATH"
  command -v claude >/dev/null || die "Claude Code 설치에 실패했습니다. 위 출력을 확인해 주세요."
  persist_path "$HOME/.local/bin"
  ok "Claude Code $(claude --version 2>/dev/null | awk '{print $1}')"
  todo "저장소 폴더에서 claude 를 실행해 로그인해 주세요"
fi

CODE=$(vscode_cli)
if [ -z "$CODE" ]; then
  todo "VS Code를 설치한 뒤 다시 실행해 주세요(확장 설치를 위해)."
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

# 전역 git 설정(회사 계정 등)은 건드리지 않고 이 저장소에만 둔다. 전역 값이 있으면 그대로 쓸지 묻는다
for key in user.name user.email; do
  if [ -n "$(git config --local "$key" || true)" ]; then
    skip "git $key = $(git config --local "$key")"
  else
    value=$(git config --global "$key" || true)
    if [ -n "$value" ]; then
      read -rp "  이 저장소에도 전역 git $key($value) 값을 사용하시겠습니까? [Y/n] " answer
      case "$answer" in [nN]*) value="" ;; esac
    fi
    while [ -z "$value" ]; do read -rp "  이 저장소에 사용할 git $key 값을 입력하세요: " value; done
    git config --local "$key" "$value"
    ok "git $key = $value"
  fi
done

# 개인 브랜치 이름 = 이니셜. 핸드오프 파일 이름과 커밋 접두사가 여기서 나온다
branch=$(git branch --show-current)
case "$branch" in
  "" | main | master | develop)
    read -rp "  개인 브랜치로 사용할 이니셜을 입력하세요(대문자, 예: LJS): " initials
    if git show-ref --verify --quiet "refs/heads/$initials"; then
      git checkout "$initials"
    elif git ls-remote --exit-code --heads origin "$initials" >/dev/null 2>&1; then
      git fetch origin "$initials" && git checkout -b "$initials" --track "origin/$initials"
    else
      git checkout -b "$initials"
    fi
    branch=$initials
    ok "브랜치 $branch"
    ;;
  *) skip "브랜치 $branch" ;;
esac

# 원격에 없는 개인 브랜치는 공개 저장소이므로 물어보고 올린다
if ! git ls-remote --exit-code --heads origin "$branch" >/dev/null 2>&1; then
  read -rp "  origin에 $branch 브랜치가 없습니다. 지금 올리시겠습니까? [Y/n] " answer
  case "$answer" in
    [nN]*) todo "개인 브랜치를 올려 주세요: git push -u origin $branch" ;;
    *)
      if git push -q -u origin "$branch"; then ok "브랜치 $branch 푸시"
      else todo "푸시하지 못했습니다. 저장소 권한을 확인한 뒤 git push -u origin $branch 를 실행해 주세요"; fi
      ;;
  esac
fi

step "백엔드"

RESET_DB=0
for arg in "$@"; do
  case "$arg" in --reset-db) RESET_DB=1 ;; esac
done

# JDK: build.gradle의 toolchain 버전. 배포판(Temurin, Oracle, Microsoft)은 가리지 않는다
want_jdk=$(grep -o 'JavaLanguageVersion.of([0-9]*)' build.gradle | grep -o '[0-9][0-9]*')
have_jdk=""
if [ "$OS" = mac ]; then
  have_jdk=$(/usr/libexec/java_home -v "$want_jdk" 2>/dev/null || true)
else
  for d in "/c/Program Files/Eclipse Adoptium/jdk-$want_jdk"* "/c/Program Files/Java/jdk-$want_jdk"* "/c/Program Files/Microsoft/jdk-$want_jdk"*; do
    [ -x "$d/bin/java.exe" ] && have_jdk="$d"
  done
fi
if [ -n "$have_jdk" ]; then
  skip "JDK $want_jdk ($have_jdk)"
else
  pkg "temurin@$want_jdk" cask "EclipseAdoptium.Temurin.$want_jdk.JDK"
  ok "JDK $want_jdk"
fi

# MySQL 8: 로컬 개발 DB
find_mysql() {
  mysql_bin=""
  local m
  for m in mysql /opt/homebrew/opt/mysql@8.4/bin/mysql /usr/local/opt/mysql@8.4/bin/mysql /usr/local/mysql/bin/mysql; do
    if command -v "$m" >/dev/null 2>&1; then mysql_bin=$(command -v "$m"); break; fi
  done
  if [ "$OS" = win ] && [ -z "$mysql_bin" ]; then
    for m in "/c/Program Files/MySQL/MySQL Server 8."*/bin/mysql.exe; do [ -x "$m" ] && mysql_bin="$m"; done
  fi
  return 0
}
find_mysql
if [ -n "$mysql_bin" ] && "$mysql_bin" --version | grep -q ' 8\.'; then
  skip "MySQL $("$mysql_bin" --version | sed -n 's/.*Ver \([0-9.]*\).*/\1/p')"
else
  pkg mysql@8.4 formula Oracle.MySQL 8.4.9
  find_mysql
  ok "MySQL 8.4"
fi

if [ "$NEED_RESTART" = 1 ]; then
  printf '\n[다시 실행] 새로 설치한 프로그램은 새 창에서만 인식됩니다. 창을 닫았다가 새로 열고 같은 명령(setup.bat 또는 bash setup.sh)을 한 번 더 실행해 주세요.\n'
  exit 2
fi

# DB: 로컬 MySQL에 스키마·전용 계정을 만들고 db/의 SQL을 실행한다.
# 접속 정보는 .env.local(git 제외)에 쓰고 Spring이 읽는다(application.yml의 spring.config.import)
DB_NAME=PAWEVER
DB_USER=nyangmeong
DB_PORT=${DB_PORT:-3306}  # 3306이 아닌 MySQL을 쓸 때만 환경변수로 바꾼다
[ -n "$mysql_bin" ] || die "MySQL 명령을 찾지 못했습니다. 창을 새로 열고 다시 실행해 주세요."
mysqladmin_bin="$(dirname "$mysql_bin")/mysqladmin"

# db_sql <비밀번호> <계정> [mysql 인자...]. 비밀번호는 명령줄에 남지 않도록 MYSQL_PWD로 넘긴다
db_sql() {
  local pass=$1 user=$2
  shift 2
  MYSQL_PWD="$pass" "$mysql_bin" --protocol=TCP -h127.0.0.1 -P"$DB_PORT" -u"$user" --default-character-set=utf8mb4 "$@"
}
mysql_alive() { "$mysqladmin_bin" --protocol=TCP -h127.0.0.1 -P"$DB_PORT" ping >/dev/null 2>&1; }
wait_mysql() {
  local _
  for _ in $(seq 1 30); do mysql_alive && return 0; sleep 1; done
  return 1
}

# Windows 관리자 권한으로 PowerShell 스크립트를 실행한다(UAC 창이 뜬다)
win_admin() {
  local ps1
  ps1=$(mktemp --suffix=.ps1)
  printf '%s\n' "$1" > "$ps1"
  powershell.exe -NoProfile -Command "Start-Process powershell -Verb RunAs -Wait -ArgumentList '-NoProfile','-ExecutionPolicy','Bypass','-File','\"$(cygpath -w "$ps1")\"'" \
    || { rm -f "$ps1"; die "관리자 권한 실행이 취소됐거나 실패했습니다. 다시 실행해 주세요."; }
  rm -f "$ps1"
}

fresh_root=0
if ! mysql_alive; then
  if [ "$OS" = mac ]; then
    brew services start mysql@8.4
  else
    base=$(cd "$(dirname "$mysql_bin")/.." && pwd -W)
    svc=$(powershell.exe -NoProfile -Command "(Get-CimInstance Win32_Service | Where-Object { \$_.PathName -like '*$(basename "$base")*' } | Select-Object -First 1).Name" | tr -d '\r')
    if [ -n "$svc" ]; then
      win_admin "Start-Service '$svc'"
    else
      # winget은 MySQL 프로그램만 설치하고 서버 초기화·서비스 등록은 하지 않는다. root는 비밀번호 없이 만들어진다
      ver=$(basename "$base" | grep -o '[0-9]*\.[0-9]*' | tr -d .)
      data="C:/ProgramData/MySQL/$(basename "$base")"
      win_admin "\$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force '$data' | Out-Null
Set-Content -Encoding ascii '$data/my.ini' \"[mysqld]\`r\`nbasedir=$base\`r\`ndatadir=$data/Data\`r\`nport=$DB_PORT\"
& '$base/bin/mysqld.exe' --defaults-file='$data/my.ini' --initialize-insecure
& '$base/bin/mysqld.exe' --install MySQL$ver --defaults-file='$data/my.ini'
Start-Service MySQL$ver"
      fresh_root=1
      ok "MySQL 서버 초기화, 서비스 MySQL$ver 등록"
    fi
  fi
  wait_mysql || die "MySQL 서버가 응답하지 않습니다(포트 $DB_PORT). 서버 상태를 확인한 뒤 다시 실행해 주세요."
fi
skip "MySQL 서버 (127.0.0.1:$DB_PORT)"

# .env.local의 값 하나를 읽는다. 쓸 때 역슬래시를 두 번 썼으므로 되돌린다
env_get() { [ -f .env.local ] && sed -n "s/^$1=//p" .env.local | sed 's/\\\\/\\/g' || true; }

db_pass=$(env_get DB_PASSWORD)
if [ -n "$db_pass" ] && db_sql "$db_pass" "$DB_USER" -e "SELECT 1" "$DB_NAME" >/dev/null 2>&1; then
  skip "DB $DB_NAME / 계정 $DB_USER"
else
  while :; do
    read -rsp "  DB 계정 $DB_USER 에 사용할 비밀번호를 정해 입력하세요: " db_pass; echo
    [ -n "$db_pass" ] || continue
    read -rsp "  한 번 더 입력하세요: " again; echo
    [ "$db_pass" = "$again" ] && break
    echo "  두 비밀번호가 다릅니다. 다시 입력해 주세요."
  done
  if [ "$fresh_root" = 1 ]; then
    root_pass=""
  else
    read -rsp "  MySQL root 비밀번호를 입력하세요(없으면 엔터, 저장하지 않습니다): " root_pass; echo
  fi
  db_sql "$root_pass" root -e "SELECT 1" >/dev/null 2>&1 || die "root로 접속하지 못했습니다. 비밀번호를 확인한 뒤 다시 실행해 주세요."
  if [ "$fresh_root" = 1 ]; then
    read -rsp "  새로 만든 MySQL의 root 비밀번호를 정해 입력하세요(엔터면 비워 둡니다): " new_root; echo
  fi
  # SQL 문자열 안에서 역슬래시와 작은따옴표를 이스케이프한다
  q=${db_pass//\\/\\\\}; q=${q//\'/\'\'}
  {
    printf 'CREATE DATABASE IF NOT EXISTS `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;\n' "$DB_NAME"
    for host in localhost 127.0.0.1; do
      printf "CREATE USER IF NOT EXISTS '%s'@'%s' IDENTIFIED BY '%s';\n" "$DB_USER" "$host" "$q"
      printf "ALTER USER '%s'@'%s' IDENTIFIED BY '%s';\n" "$DB_USER" "$host" "$q"
      printf "GRANT ALL PRIVILEGES ON \`%s\`.* TO '%s'@'%s';\n" "$DB_NAME" "$DB_USER" "$host"
    done
    if [ -n "${new_root:-}" ]; then
      r=${new_root//\\/\\\\}; r=${r//\'/\'\'}
      printf "ALTER USER 'root'@'localhost' IDENTIFIED BY '%s';\n" "$r"
    fi
  } | db_sql "$root_pass" root
  unset root_pass new_root q r again
  ok "DB $DB_NAME / 계정 $DB_USER"
fi

# Properties 형식이라 역슬래시를 두 번 쓴다. heredoc 안에서는 치환 결과가 달라지므로 밖에서 만든다
pass_prop=${db_pass//\\/\\\\}
umask 077
cat > .env.local <<EOF
# setup.sh가 만든 로컬 DB 접속 정보. git에 올리지 않는다. 같은 이름의 환경변수가 있으면 환경변수가 우선한다
DB_URL=jdbc:mysql://127.0.0.1:$DB_PORT/$DB_NAME
DB_USERNAME=$DB_USER
DB_PASSWORD=$pass_prop
EOF
unset pass_prop
umask 022
ok ".env.local (DB 접속 정보)"

if [ "$RESET_DB" = 1 ]; then
  read -rp "  $DB_NAME 의 모든 테이블과 데이터를 지우고 db/의 SQL을 처음부터 다시 실행합니다. 계속하시겠습니까? [y/N] " answer
  case "$answer" in [yY]*) ;; *) die "DB 초기화를 취소했습니다." ;; esac
  db_sql "$db_pass" "$DB_USER" -e "DROP DATABASE \`$DB_NAME\`; CREATE DATABASE \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
  ok "DB $DB_NAME 초기화"
fi

# db/의 SQL은 파일 이름(YYYYMMDD_설명.sql) 순서로 한 번씩만 실행한다. 실행한 파일 이름은 db/.applied(git 제외)에 남긴다
applied=db/.applied
tables=$(db_sql "$db_pass" "$DB_USER" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()" "$DB_NAME")
if [ "$tables" = 0 ]; then
  : > "$applied"
elif [ ! -f "$applied" ]; then
  # 기록 없이 테이블만 있으면 첫 SQL의 DROP이 데이터를 지우므로, 지금 파일들은 이미 반영된 것으로 본다
  ls db/*.sql 2>/dev/null | xargs -n1 basename > "$applied" || true
  todo "테이블 ${tables}개가 이미 있어 db/의 SQL을 실행하지 않았습니다. 처음부터 만들려면 --reset-db 를 붙여 다시 실행해 주세요"
fi
ran=0
for f in db/*.sql; do
  [ -e "$f" ] || continue
  name=$(basename "$f")
  grep -qxF "$name" "$applied" && continue
  db_sql "$db_pass" "$DB_USER" "$DB_NAME" < "$f" || die "$name 실행 중 오류가 났습니다. 위 메시지를 확인해 주세요."
  echo "$name" >> "$applied"
  ok "SQL $name"
  ran=$((ran + 1))
done
tables=$(db_sql "$db_pass" "$DB_USER" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()" "$DB_NAME")
[ "$ran" = 0 ] && skip "db/ SQL (새 파일 없음, 테이블 ${tables}개)"
[ "$ran" -gt 0 ] && ok "테이블 ${tables}개"

step "확인"
./gradlew build -q && ok "./gradlew build"

# 서버가 .env.local을 읽고 뜨는지 보고 끈다. 포트는 비어 있는 아무 포트(0)를 쓴다
jar=$(ls build/libs/*.jar | grep -v -- '-plain\.jar$' | head -1)
log=$(mktemp)
"$have_jdk/bin/java" -jar "$jar" --server.port=0 > "$log" 2>&1 &
pid=$!
for _ in $(seq 1 120); do
  grep -q 'Started .* in ' "$log" && break
  kill -0 "$pid" 2>/dev/null || break
  sleep 1
done
started=$(grep -o 'Started .* in [0-9.]* seconds' "$log" || true)
kill "$pid" 2>/dev/null || true
wait "$pid" 2>/dev/null || true
if [ -z "$started" ]; then
  tail -40 "$log"
  rm -f "$log"
  die "서버가 뜨지 않았습니다. 위 로그를 확인해 주세요."
fi
rm -f "$log"
ok "서버 기동 (${started#Started })"

printf '\n설치가 끝났습니다. 직접 할 일 %d개(위 [직접] 줄).\n' "$TODO"
