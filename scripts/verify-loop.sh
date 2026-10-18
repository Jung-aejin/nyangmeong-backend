#!/bin/sh
# /build 검증 루프의 기준 파일 잠금. /verify 첫 실패에 기준 파일 지문을 남기고, 잠긴 동안 그 파일을 고치지 못하게 한다.
# 루프가 기획·검증 명령·테스트를 고쳐 통과하는 것을 막는다. 회차와 지문은 /verify(별도 에이전트)만 남긴다.
# 백엔드 저장소에 같은 파일이 있다. 고칠 때 둘 다 고친다.
# 사용: sh scripts/verify-loop.sh fail "<실패 항목>" | check | status | clear | guard(훅 전용, stdin JSON)
cd "$(git rev-parse --show-toplevel 2>/dev/null || echo .)" || exit 0
LOCK=.claude/verify-loop.lock
TAB=$(printf '\t')
# 잠그는 기준 파일. 없는 경로는 무시된다(프론트·백엔드 공용 목록)
PROTECTED="handoff milestones AGENTS.md CLAUDE.md .claude .agents .codex scripts/verify-loop.sh package.json yarn.lock eslint.config.js vite.config.js build.gradle settings.gradle gradle src/test"

snapshot() {
  mkdir -p .claude
  {
    printf 'round\t0\n'
    # shellcheck disable=SC2086
    git ls-files -co --exclude-standard -- $PROTECTED | while IFS= read -r f; do
      [ -f "$f" ] && printf 'hash\t%s\t%s\n' "$(git hash-object "$f")" "$f"
    done
  } > "$LOCK"
}

changed() {
  [ -f "$LOCK" ] || return 0
  grep "^hash$TAB" "$LOCK" | while IFS="$TAB" read -r _ oid f; do
    now=$(git hash-object "$f" 2>/dev/null || echo deleted)
    [ "$now" = "$oid" ] || echo "$f"
  done
}

block() {
  echo "검증 루프 중에는 기준 파일($1)을 고치지 않습니다. 이 파일을 고쳐야 통과한다면 멈추고 사람에게 이유를 말하세요. 사람이 /start 를 다시 실행하면 잠금이 풀립니다." >&2
  exit 2
}

case "$1" in
  fail)
    [ -f "$LOCK" ] || snapshot
    round=$(( $(grep "^round$TAB" "$LOCK" | cut -f2) + 1 ))
    item=$(printf '%s' "$2" | tr '\t\n' '  ')
    { printf 'round\t%s\n' "$round"; grep -v "^round$TAB" "$LOCK"; printf 'fail\t%s\t%s\n' "$round" "$item"; } > "$LOCK.tmp" && mv "$LOCK.tmp" "$LOCK"
    echo "회차 $round"
    ;;
  check)
    out=$(changed)
    [ -z "$out" ] && exit 0
    echo "$out"
    exit 1
    ;;
  status)
    [ -f "$LOCK" ] || { echo "잠금 없음"; exit 0; }
    echo "회차 $(grep "^round$TAB" "$LOCK" | cut -f2)"
    grep "^fail$TAB" "$LOCK" | while IFS="$TAB" read -r _ n item; do echo "- ${n}회차: $item"; done
    ;;
  clear)
    rm -f "$LOCK"
    ;;
  guard)
    [ -f "$LOCK" ] || exit 0
    json=$(cat)
    tool=$(printf '%s' "$json" | grep -o '"tool_name"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*"\([^"]*\)"$/\1/')
    if [ "$tool" = Bash ]; then
      case "$json" in *verify-loop.lock*) block "$LOCK" ;; esac
      exit 0
    fi
    # Claude: Edit·Write·MultiEdit 는 file_path, NotebookEdit 는 notebook_path. Codex: apply_patch 본문의 "*** Update File: <경로>" 줄
    # JSON 의 \\(Windows 경로 구분자)를 먼저 / 로 바꿔, 남은 \n 이 경로 끝이 되게 한다
    norm=$(printf '%s' "$json" | sed 's/\\\\/\//g')
    paths=$( { printf '%s' "$norm" | grep -oE '"(file|notebook)_path"[[:space:]]*:[[:space:]]*"[^"]*"' |
        sed -e 's/^"[a-z_]*"[[:space:]]*:[[:space:]]*"//' -e 's/"$//'
      printf '%s' "$norm" | grep -oE '\*\*\* (Add File|Update File|Delete File|Move to): [^\\"]*' | sed 's/^\*\*\* [A-Za-z ]*: //'
    } | tr 'A-Z' 'a-z')
    [ -n "$paths" ] || exit 0
    root=$(pwd -W 2>/dev/null || pwd)
    root=$(printf '%s' "$root" | tr 'A-Z\\' 'a-z/')
    printf '%s\n' "$paths" | while IFS= read -r path; do
      case "$path" in
        "$root"/*) rel=${path#"$root"/} ;;
        /*|?:/*) continue ;;
        *) rel=${path#./} ;;
      esac
      case "$rel" in
        handoff/*|milestones/*|.claude/*|.agents/*|.codex/*|agents.md|claude.md) block "$rel" ;;
      esac
      if grep "^hash$TAB" "$LOCK" | cut -f3 | tr 'A-Z' 'a-z' | grep -qxF "$rel"; then block "$rel"; fi
    done || exit 2
    exit 0
    ;;
  *)
    echo "사용: sh scripts/verify-loop.sh fail \"<실패 항목>\" | check | status | clear | guard" >&2
    exit 2
    ;;
esac
