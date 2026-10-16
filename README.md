# 냥멍케어 백엔드

반려동물 케어 웹서비스 냥멍케어의 백엔드. Java 25, Spring Boot 4.1, MyBatis, MySQL 8.

코드를 쓰기 전에 `AGENTS.md`를 읽는다. 나머지 규칙은 `docs/convention/`에 있다. 아직 정하지 않은 것은 `docs/OPEN_ISSUES.md`에 있다.

## 실행

처음 받으면 setup을 실행한다. 여러 번 실행해도 된다.

| OS | 실행 |
| --- | --- |
| Windows | cmd `setup.bat`, PowerShell `.\setup.bat`, 또는 탐색기에서 더블클릭. `bash setup.sh`는 WSL이 실행되므로 쓰지 않는다 |
| 맥 | 터미널에서 `bash setup.sh` |

setup이 하는 일:

| 항목 | 내용 |
| --- | --- |
| Git | 이 저장소의 git 사용자 설정, 이니셜 브랜치 만들기·올리기 |
| AI 도구 | Claude Code·Codex 중 설치된 것을 감지. 둘 다 없으면 Claude Code 설치(Codex를 쓰려면 먼저 설치) |
| IDE | 처음에 VS Code·STS·둘 다 중 고른다. 다시 고르려면 `setup.bat --ide` |
| JDK 25 | 없으면 설치. 표준 위치 → `JAVA_HOME` → `~/.jdks` 순서로 찾는다 |
| VS Code | 확장(Java Pack, Spring Boot, Claude Code). 자동 감지되지 않는 JDK만 사용자 설정 `java.configuration.runtimes`에 등록 |
| STS | 없으면 설치(Windows `%LOCALAPPDATA%\Programs`, 맥 brew). Lombok, 프로젝트 설정(공용 포맷·저장 시 포맷·UTF-8·LF) |
| `.env.local` | 있는지 확인 |
| MCP | `dms`(요구사항정의서·화면설계서·기능명세서). 같은 claude.ai 커넥터가 이미 있으면 Claude Code에는 등록하지 않는다 |
| 확인 | 빌드, 공용 DB 접속, 서버 기동 후 종료. DB에 접속하지 못하면(학원 밖 등) 할 일로 알려 준다 |

직접 할 일:

- `.env.local`을 디스코드에서 받아 `setup.sh` 옆에 둔다. 이름이 `env.local`이나 `.env.local.txt`로 바뀌지 않았는지 확인한다. git에 올리지 않는다.
- STS는 처음 한 번 File > Import > Gradle > Existing Gradle Project로 `gradlew`가 있는 `backend` 폴더를 가져온다. 상위 폴더를 고르면 Gradle Wrapper가 없어 동기화가 실패한다.
- MCP 인증은 아래 "처음 한 번: MCP 인증"을 따른다. 감지한 도구 모두에 `dms`가 이미 있으면 `DMS_MCP_URL`은 비워 둬도 된다.

`.env.local`은 Java properties 형식이다(`키=값`). Spring이 이 파일을 읽고, 같은 이름의 환경변수가 있으면 환경변수가 우선한다. 값에 역슬래시(`\`)가 있으면 두 번 쓴다.

| 키 | 예 |
| --- | --- |
| `DB_URL` | `jdbc:mysql://호스트:3306/PAWEVER` |
| `DB_USERNAME` | |
| `DB_PASSWORD` | |
| `DMS_MCP_URL` | DMS MCP 서버 주소 |

`db/`의 SQL은 setup이 실행하지 않는다. 테이블을 바꾸는 PR의 작성자가 병합 직후 공용 DB에 직접 실행한다(`docs/convention/shared-files.md`).

```
./gradlew bootRun
```

API 명세는 `http://localhost:8080/swagger-ui.html`에서 본다.

## AI 도구로 작업하기

setup을 먼저 실행한다. 아래 세 가지 중 편한 것을 쓴다. 셋 다 같은 설정(`~/.claude`의 MCP, 이 저장소의 `.claude/skills`)을 쓰므로 `/start`·`/verify`·`/done`이 똑같이 동작한다.

| | 설치 | 이 저장소에서 시작 |
| --- | --- | --- |
| 데스크탑 앱 | https://claude.com/download | 로그인 → Code 탭 → + New session(Ctrl+N) → 환경 Local, 이 저장소 폴더 선택 |
| VS Code 확장 | 확장 `anthropic.claude-code`(Anthropic의 Claude Code). IDE로 VS Code를 고르면 setup이 설치한다. STS만 쓰면 VS Code에서 직접 설치한다 | 이 저장소 폴더를 열고 왼쪽 막대의 Claude 아이콘 또는 상태 표시줄의 ✻ Claude Code |
| 터미널 | setup이 설치한다 | 이 저장소 폴더에서 `claude` |

setup을 실행한 뒤에는 앱을 다시 켠다. 새로 설치한 프로그램과 PATH는 다시 켜야 잡힌다.

### 처음 한 번: MCP 인증

claude.ai 계정으로 로그인하면 claude.ai에서 연결한 커넥터(`claude.ai dms`, `claude.ai Figma`)는 따로 인증하지 않아도 된다. setup이 등록한 서버 중 인증이 필요한 것은 터미널·VS Code에서 `/mcp`를 입력해 서버를 고르고 Authenticate 한다. 데스크탑 앱은 Connectors 화면에서 한다(확인 필요). Codex는 setup이 등록할 때 로그인 창이 열린다(놓쳤으면 `codex mcp login <이름>`).

### 작업 흐름

1. `/start` — 할 작업을 고르고 기획을 핸드오프에 쓴 뒤 멈춘다. 특정 마일스톤은 화면 ID로 `/start COM-003`.
   - 마일스톤 파일(`milestones/<이니셜>.md`)이 없으면 DMS 기능명세서로 먼저 만들고 이어서 기획한다. 이때는 시간이 더 걸린다.
2. 기획을 확인한다. 고칠 점은 답하고, 그대로 가려면 `승인`이라고 답한다. 구현은 기획에 적은 파일만 고친다.
3. 검증 명령이 통과했다고 안내하면 `/verify` — 구현한 대화를 보지 않는 별도 에이전트(Opus)가 저장소만 보고 판정한다.
4. 판정이 통과면 `/done` — 핸드오프와 마일스톤 상태를 갱신한다. 실패면 고친 뒤 다시 `/verify`.
5. 새 대화를 열고 다음 작업을 `/start`로 시작한다. 이전 대화를 이어 쓰면 그만큼 토큰을 계속 쓴다.

새 대화는 터미널에서 `/clear`, 데스크탑 앱에서 + New session(Ctrl+N), VS Code 확장에서 `/clear`(확인 필요. 안 되면 명령 팔레트의 Open in New Tab)로 연다.

모델은 바꾸지 않는다. 프로젝트 기본 모델이 Sonnet(`.claude/settings.json`)이고, `/start`·`/verify`만 그동안 Opus로 돈다. 세션에서 모델을 직접 바꿨다면 Sonnet으로 되돌린다. VS Code 확장은 터미널보다 쓸 수 있는 명령이 적다. 스킬이 `/` 목록에 없으면 새 대화를 연다.

## PR 전 확인

```
./gradlew build
```

테스트는 DB에 붙지 않는다.
