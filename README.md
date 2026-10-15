# 냥멍케어 백엔드

반려동물 케어 웹서비스 냥멍케어의 백엔드. Java 25, Spring Boot 4.1, MyBatis, MySQL 8.

코드를 쓰기 전에 `AGENTS.md`를 읽는다. 나머지 규칙은 `docs/convention/`에 있다. 아직 정하지 않은 것은 `docs/OPEN_ISSUES.md`에 있다.

## 실행

처음 받으면 맥은 터미널에서 `bash setup.sh`, Windows는 cmd에서 `setup.bat`, PowerShell에서 `.\setup.bat`(탐색기에서 더블클릭해도 된다)로 개발 환경을 만든다. PowerShell·cmd에서 `bash setup.sh`를 치면 Git Bash가 아니라 WSL이 실행되므로 `setup.bat`를 쓴다. 여러 번 실행해도 된다.

setup이 하는 일:

- JDK 25와 MySQL 8을 설치한다. Windows에서 MySQL을 새로 설치하면 서버 초기화와 서비스 등록까지 한다(관리자 권한 창이 한 번 뜬다).
- 로컬 MySQL에 스키마 `PAWEVER`와 계정 `nyangmeong`을 만든다. 실행 중에 계정 비밀번호를 정하고 MySQL root 비밀번호를 입력한다. root 비밀번호는 저장하지 않는다.
- `db/`의 SQL을 파일 이름 순서로 실행한다. 다시 실행하면 아직 실행하지 않은 새 파일만 실행한다. 실행한 파일은 `db/.applied`(git 제외)에 기록된다.
- 접속 정보를 `.env.local`(git 제외)에 쓴다. Spring이 이 파일을 읽는다. 같은 이름의 환경변수가 있으면 환경변수가 우선한다.
- 빌드하고 서버가 뜨는지 확인한 뒤 끈다.

로컬 DB를 지우고 `db/`의 SQL을 처음부터 다시 실행하려면 `bash setup.sh --reset-db`(Windows는 `setup.bat --reset-db`)를 실행한다.

```
./gradlew bootRun
```

API 명세는 `http://localhost:8080/swagger-ui.html`에서 본다.

## PR 전 확인

```
./gradlew build
```

테스트는 DB에 붙지 않는다.
