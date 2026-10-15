# 냥멍케어 백엔드

반려동물 케어 웹서비스 냥멍케어의 백엔드. Java 25, Spring Boot 4.1, MyBatis, MySQL 8.

코드를 쓰기 전에 `AGENTS.md`를 읽는다. 나머지 규칙은 `docs/convention/`에 있다. 아직 정하지 않은 것은 `docs/OPEN_ISSUES.md`에 있다.

## 실행

처음 받으면 맥은 터미널에서 `bash setup.sh`, Windows는 cmd에서 `setup.bat`, PowerShell에서 `.\setup.bat`(탐색기에서 더블클릭해도 된다)로 개발 환경을 만든다. PowerShell·cmd에서 `bash setup.sh`를 치면 Git Bash가 아니라 WSL이 실행되므로 `setup.bat`를 쓴다. 여러 번 실행해도 된다.

setup이 하는 일:

- 처음 실행할 때 쓸 IDE(VS Code, STS, 둘 다)를 묻고 고른 IDE만 세팅한다. 고른 값은 기억하며, 다시 고르려면 `--ide`를 붙여 실행한다(`setup.bat --ide`).
- JDK 25를 설치한다. 이미 있으면 표준 설치 위치, `JAVA_HOME`, `~/.jdks` 순서로 찾는다.
- VS Code: 확장(Java Pack, Spring Boot)을 설치한다. JDK는 1차로 Java 확장의 자동 감지에 맡기고, 자동 감지 위치가 아닌 곳(예: `~/.jdks`)에서 찾은 JDK만 사용자 설정의 `java.configuration.runtimes`에 등록한다. 그 설정이 이미 있으면 고치지 않고 직접 할 일로 알려 준다.
- STS: 없으면 설치하고(Windows는 공식 zip을 `%LOCALAPPDATA%\Programs`에 풀고, 맥은 brew), Lombok을 넣는다. 실행 파일은 그 폴더의 `SpringToolsForEclipse.exe`다. 프로젝트 설정(`.settings/`, git 제외)에 공용 포맷(`eclipse-formatter.xml`), 저장 시 포맷, UTF-8, LF를 넣는다. 프로젝트 가져오기(File > Import > Gradle > Existing Gradle Project)는 처음 한 번 직접 한다. 이때 상위 폴더가 아니라 `gradlew`가 있는 `backend` 폴더를 고른다. 상위 폴더를 고르면 Gradle Wrapper가 없어 STS가 Java 25를 지원하지 않는 Gradle로 동기화하다 실패한다.
- 팀 공용 DB 접속 정보가 든 `.env.local`이 있는지 확인한다. 이 파일은 디스코드로 공유하므로, 받아서 저장소 루트(`setup.sh` 옆)에 넣는다. 내려받을 때 이름이 `env.local`이나 `.env.local.txt`로 바뀌지 않았는지 확인한다. git에는 올리지 않는다. 공개 저장소이므로 서버 주소도 저장소에 두지 않는다.
- 빌드하고, 공용 DB에 접속되는지와 서버가 뜨는지 확인한 뒤 끈다. DB 접속이 안 되면(학원 밖 네트워크 등) 멈추지 않고 직접 할 일로 알려 준다.

`.env.local`은 Java properties 형식이다(`키=값`). Spring이 이 파일을 읽고, 같은 이름의 환경변수가 있으면 환경변수가 우선한다. 값에 역슬래시(`\`)가 있으면 두 번 쓴다.

| 키 | 예 |
| --- | --- |
| `DB_URL` | `jdbc:mysql://호스트:3306/PAWEVER` |
| `DB_USERNAME` | |
| `DB_PASSWORD` | |

`db/`의 SQL은 setup이 실행하지 않는다. 테이블을 바꾸는 PR의 작성자가 병합 직후 공용 DB에 직접 실행한다(`AGENTS.md`).

```
./gradlew bootRun
```

API 명세는 `http://localhost:8080/swagger-ui.html`에서 본다.

## PR 전 확인

```
./gradlew build
```

테스트는 DB에 붙지 않는다.
