# 냥멍케어 백엔드

반려동물 케어 웹서비스 냥멍케어의 백엔드. Java 25, Spring Boot 4.1, MyBatis, MySQL 8.

코드를 쓰기 전에 `AGENTS.md`를 읽는다. 나머지 규칙은 `docs/convention/`에 있다. 아직 정하지 않은 것은 `docs/OPEN_ISSUES.md`에 있다.

## 실행

처음 받으면 맥은 터미널에서 `bash setup.sh`, Windows는 cmd에서 `setup.bat`, PowerShell에서 `.\setup.bat`(탐색기에서 더블클릭해도 된다)로 필요한 도구를 설치한다. PowerShell·cmd에서 `bash setup.sh`를 치면 Git Bash가 아니라 WSL이 실행되므로 `setup.bat`를 쓴다. 여러 번 실행해도 된다.

JDK 25와 로컬 MySQL 8이 필요하다. DB 접속 정보는 환경변수로 넣는다.

| 환경변수 | 예 |
| --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/스키마이름` |
| `DB_USERNAME` | |
| `DB_PASSWORD` | |

```
./gradlew bootRun
```

API 명세는 `http://localhost:8080/swagger-ui.html`에서 본다.

## PR 전 확인

```
./gradlew build
```

테스트는 DB에 붙지 않는다.
