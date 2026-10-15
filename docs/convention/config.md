# 설정과 비밀값

설정 파일, 비밀값, 환경변수를 다룰 때와 핸드오프를 쓸 때 연다.

**이 저장소는 공개다. 한 번 커밋된 값은 지워도 이력에 남는다.**

- 공용 DB 접속 정보(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)는 디스코드로 공유하는 `.env.local`(git 제외)을 저장소 루트에 둔다. Spring이 `spring.config.import`로 읽는다. 서버 주소도 공개 저장소에 적지 않는다.
- DMS MCP 서버 주소(`DMS_MCP_URL`)도 같은 `.env.local`에 둔다. `setup.sh`가 읽어 감지한 AI 도구(Claude Code·Codex)에 등록한다.
- 그 밖의 비밀값은 모두 환경변수로만 넣는다. 예: `JWT_SECRET`, `DB_PASSWORD`, `ENC_KEY`, `HMAC_KEY`, `MAIL_PASSWORD`, 소셜 로그인 클라이언트 시크릿, 외부 API 키(AI, OCR, 지도).
- 설정 파일에는 `${DB_PASSWORD}`처럼 키 이름만 둔다.
- 설정 파일은 `application.yml`, `application-local.yml`, `application-prod.yml`로 나눈다.
- 로그에 비밀번호와 토큰을 남기지 않는다.
- 비밀값이 든 파일(`.env` 등)과 `CLAUDE.local.md`는 git에 올리지 않는다.
- 작업 기록은 `handoff/<이름>.md`에 사람마다 한 파일로 두고 커밋한다. 다른 PC에서도 이어서 작업하기 위해서다. 공개 저장소이므로 비밀값과 개인 PC 경로를 쓰지 않는다.
