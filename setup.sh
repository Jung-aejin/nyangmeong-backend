#!/usr/bin/env bash
# 냥멍케어 백엔드 개발 환경 설치. 맥은 터미널에서 `bash setup.sh`, Windows는 cmd·PowerShell에서 `setup.bat`(또는 Git Bash에서 `bash setup.sh`)로 실행한다.
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

# JDK: build.gradle의 toolchain 버전. 배포판(Temurin, Oracle, Microsoft 등)은 가리지 않는다.
# 버전은 폴더 이름이 아니라 JDK의 release 파일로 확인한다
want_jdk=$(grep -o 'JavaLanguageVersion.of([0-9]*)' build.gradle | grep -o '[0-9][0-9]*')
jdk_major() { sed -n 's/^JAVA_VERSION="\([0-9]*\).*/\1/p' "$1/release" 2>/dev/null; }
have_jdk=""
jdk_auto=n  # VS Code Java 확장이 자동으로 찾는 위치(표준 설치 위치, JAVA_HOME)에 있는가
if [ "$OS" = mac ]; then
  d=$(/usr/libexec/java_home -v "$want_jdk" 2>/dev/null || true)
  [ -n "$d" ] && [ "$(jdk_major "$d")" = "$want_jdk" ] && { have_jdk=$d; jdk_auto=y; }
else
  for d in "/c/Program Files/"{"Eclipse Adoptium",Java,Microsoft,Zulu,"Amazon Corretto",BellSoft}/*; do
    [ "$(jdk_major "$d")" = "$want_jdk" ] && { have_jdk=$d; jdk_auto=y; }
  done
fi
if [ -z "$have_jdk" ] && [ -n "${JAVA_HOME:-}" ]; then
  d=$JAVA_HOME
  [ "$OS" = win ] && d=$(cygpath -u "$d")
  [ "$(jdk_major "$d")" = "$want_jdk" ] && { have_jdk=$d; jdk_auto=y; }
fi
# IntelliJ가 내려받은 JDK(~/.jdks)는 VS Code가 자동으로 찾지 못할 수 있다
if [ -z "$have_jdk" ]; then
  for d in "$HOME"/.jdks/*; do
    [ "$(jdk_major "$d")" = "$want_jdk" ] && have_jdk=$d
  done
fi
if [ -n "$have_jdk" ]; then
  skip "JDK $want_jdk ($have_jdk)"
else
  pkg "temurin@$want_jdk" cask "EclipseAdoptium.Temurin.$want_jdk.JDK"
  ok "JDK $want_jdk"
fi

# VS Code: 1차는 Java 확장의 자동 감지에 맡긴다. 자동 감지 위치가 아닌 JDK만 사용자 설정의 java.configuration.runtimes에 넣는다.
# 경로가 PC마다 다르므로 저장소의 .vscode/settings.json이 아니라 사용자 설정에 쓴다
if [ -n "$have_jdk" ] && [ "$jdk_auto" = y ]; then
  skip "VS Code JDK $want_jdk (자동 감지)"
elif [ -n "$have_jdk" ]; then
  if [ "$OS" = mac ]; then
    user_settings="$HOME/Library/Application Support/Code/User/settings.json"
    jdk_path=$have_jdk
  else
    user_settings="$(cygpath -u "$APPDATA")/Code/User/settings.json"
    jdk_path=$(cygpath -m "$have_jdk")
  fi
  if [ -f "$user_settings" ] && grep -q '"java.configuration.runtimes"' "$user_settings"; then
    # 사용자가 직접 관리하는 값이므로 고치지 않는다
    if grep -qF "\"$jdk_path\"" "$user_settings"; then
      skip "VS Code JDK $want_jdk (java.configuration.runtimes)"
    else
      todo "VS Code 사용자 설정의 java.configuration.runtimes 에 { \"name\": \"JavaSE-$want_jdk\", \"path\": \"$jdk_path\" } 를 추가해 주세요"
    fi
  else
    entry="  \"java.configuration.runtimes\": [{ \"name\": \"JavaSE-$want_jdk\", \"path\": \"$jdk_path\" }]"
    mkdir -p "$(dirname "$user_settings")"
    if [ -s "$user_settings" ]; then
      # 첫 번째 { 바로 뒤에 한 줄을 넣는다. VS Code 설정은 끝에 붙은 쉼표를 허용한다
      awk -v e="$entry," '!done && /\{/ { sub(/\{/, "{\n" e); done = 1 } { print }' "$user_settings" > "$user_settings.tmp" \
        && mv "$user_settings.tmp" "$user_settings"
    else
      printf '{\n%s\n}\n' "$entry" > "$user_settings"
    fi
    ok "VS Code JDK $want_jdk (자동 감지 위치가 아니라 java.configuration.runtimes 에 등록)"
  fi
fi

if [ "$NEED_RESTART" = 1 ]; then
  printf '\n[다시 실행] 새로 설치한 프로그램은 새 창에서만 인식됩니다. 창을 닫았다가 새로 열고 같은 명령(setup.bat 또는 bash setup.sh)을 한 번 더 실행해 주세요.\n'
  exit 2
fi

# DB: 팀 공용 DB를 쓴다. 접속 정보는 디스코드로 공유하는 .env.local(git 제외)을 이 폴더에 넣어 쓰고 Spring이 읽는다(application.yml의 spring.config.import).
# 공개 저장소이므로 서버 주소도 저장소에 두지 않는다
has_env=n
if [ ! -f .env.local ]; then
  todo "디스코드에서 받은 .env.local 을 이 폴더($(basename "$PWD"))에 넣고 다시 실행해 주세요. 내려받을 때 이름이 env.local 이나 .env.local.txt 로 바뀌지 않았는지 확인해 주세요"
else
  missing=""
  for key in DB_URL DB_USERNAME DB_PASSWORD; do
    grep -q "^$key=." .env.local || missing="$missing $key"
  done
  if [ -n "$missing" ]; then
    todo ".env.local 에 값이 없는 키가 있습니다:$missing. 디스코드의 최신 파일로 바꿔 주세요"
  else
    has_env=y
    skip ".env.local (DB 접속 정보)"
  fi
fi

step "확인"
./gradlew build -q && ok "./gradlew build"

# DB 접속: 서버는 첫 쿼리 때 DB에 붙으므로 기동만으로는 확인되지 않는다.
# Spring과 같은 방식(Properties)으로 .env.local을 읽어 JDBC로 직접 접속해 본다. 드라이버는 Gradle이 받아 둔 것을 쓴다
# 경로 중간에 해시 폴더가 있으므로 버전 폴더 이름으로 정렬해 가장 새 것을 고른다
driver=$(find "${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1/com.mysql/mysql-connector-j" -name 'mysql-connector-j-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' 2>/dev/null \
  | awk -F/ '{ print $(NF-2) "\t" $0 }' | sort -V | tail -1 | cut -f2)
check=$(mktemp -d)
cat > "$check/DbCheck.java" <<'JAVA'
import java.io.FileReader;
import java.sql.DriverManager;
import java.util.Properties;

public class DbCheck {
  public static void main(String[] args) throws Exception {
    Properties env = new Properties();
    try (FileReader r = new FileReader(args[0])) { env.load(r); }
    DriverManager.setLoginTimeout(5);
    try (var c = DriverManager.getConnection(env.getProperty("DB_URL"), env.getProperty("DB_USERNAME"), env.getProperty("DB_PASSWORD"));
         var rs = c.createStatement().executeQuery("SELECT DATABASE(), (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE())")) {
      rs.next();
      System.out.println(rs.getString(1) + ", 테이블 " + rs.getInt(2) + "개");
    } catch (Exception e) {
      System.out.println(e.getMessage().lines().findFirst().orElse(e.toString()));
      System.exit(1);
    }
  }
}
JAVA
to_native() { if [ "$OS" = win ]; then cygpath -w "$1"; else printf '%s' "$1"; fi; }
if [ "$has_env" = n ]; then
  :  # .env.local이 없거나 비어 있다는 안내는 위에서 했다
elif [ -z "$driver" ]; then
  todo "MySQL 드라이버를 찾지 못해 DB 접속을 확인하지 못했습니다"
elif result=$("$have_jdk/bin/java" -Dstdout.encoding=UTF-8 -cp "$(to_native "$driver")" "$(to_native "$check/DbCheck.java")" "$(to_native "$PWD/.env.local")"); then
  ok "DB 접속 ($result)"
else
  todo "공용 DB에 접속하지 못했습니다: $result. 네트워크(학원 밖이면 접속이 막힐 수 있음)와 접속 정보를 확인한 뒤 다시 실행해 주세요"
fi
rm -rf "$check"

# 서버가 .env.local을 읽고 뜨는지 보고 끈다. 포트는 비어 있는 아무 포트(0)를 쓴다
if [ "$has_env" = y ]; then
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
fi

printf '\n설치가 끝났습니다. 직접 할 일 %d개(위 [직접] 줄).\n' "$TODO"
# 직접 할 일이 남으면 0이 아닌 코드로 끝낸다. setup.bat이 이 코드를 보고 창을 닫지 않고 멈춘다
[ "$TODO" = 0 ] || exit 3
