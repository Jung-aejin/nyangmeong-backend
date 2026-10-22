#!/bin/sh
# /end에서 사람이 PR이라고 답한 뒤의 기계적인 단계: 원격 develop 합치기, 검증 명령, 푸시.
# PR 제목·본문과 gh pr create는 /end가 한다. 병합은 사람이 GitHub에서 한다.
# merge로 합치고 rebase하지 않는다. 충돌이 나면 merge --abort 하지 않고 그대로 멈춘다(docs/convention/git.md).
# 사용: sh scripts/pr-prep.sh
cd "$(git rev-parse --show-toplevel)" || exit 1
branch=$(git branch --show-current)
case "$branch" in
  develop | main | "") echo "자기 이니셜 브랜치에서만 씁니다(지금: ${branch:-브랜치 없음})"; exit 1 ;;
esac
[ -z "$(git status --porcelain)" ] || { echo "커밋하지 않은 변경이 있습니다. 커밋한 뒤 다시 하세요"; git status -s; exit 1; }

git fetch -q origin || { echo "fetch 실패"; exit 1; }
n=$(git rev-list --count HEAD..origin/develop)
if [ "$n" != 0 ]; then
  echo "origin/develop에서 커밋 ${n}개를 합칩니다"
  if ! git merge -q --no-edit origin/develop; then
    echo "충돌이 났습니다. merge --abort 하지 않고 그대로 둡니다. 충돌 파일:"
    git diff --name-only --diff-filter=U | sed 's/^/- /'
    exit 1
  fi
fi

# AGENTS.md의 검증 명령. 바꾸면 둘 다 고친다
out=$(./gradlew build -q 2>&1)
code=$?
echo "./gradlew build -q → $code"
[ "$code" = 0 ] || { printf '%s\n' "$out" | grep -v '^[[:space:]]*$' | head -60; exit 1; }

git push -q origin "$branch" || { echo "푸시 실패"; exit 1; }
echo "푸시함: origin/$branch"
git log --oneline origin/develop..HEAD
