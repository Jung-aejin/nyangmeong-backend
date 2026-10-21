# LJS 핸드오프

## 지금 하는 것

SCR-ACC-002 로그인 준비 (토큰 세션 기반) — 끝 (2026-10-21 11:30). 검증 통과, 사람 확인(Postman·jwt.io·세션 폐기 401) 완료.
JWT Nimbus 교체(sid·typ), `LoginSessionManager`(issue·rotate·revoke), 필터 세션 확인, `PasswordConfig`, 스타터 방식 화이트리스트(`app.security.permit-all-paths`). 기능 ID와 추가 요청 1~2는 이 마일스톤 커밋 메시지 본문에 있다.

## 다음에 할 것

- 공용 DB 정리: 검증·사람 확인 때 생긴 RFRSH_TKN 테스트 행(MBR_SN 1)을 지운다 `DELETE FROM RFRSH_TKN WHERE MBR_SN = 1 AND REG_DT >= '2026-10-21'`
- 디스코드 공유 `application.properties`에 `app.security.permit-all-paths` 줄을 추가한다(example과 같은 값)
- 팀 안건: application.properties를 스타터처럼 커밋하는 일반 설정 + git 제외 `application-secret.properties`로 나눌지 `docs/OPEN_ISSUES.md`에 올린다
- YAY에게 알린다: 로그인·재발급·로그아웃은 `auth` 도메인(`auth/controller/AuthController`, 접두어 `/api`)에 만들고 `LoginSessionManager`의 `issue`·`rotate`·`revoke`를 쓴다. 새로 여는 경로(로그인 상태 확인·보안코드 등)는 `app.security.permit-all-paths`에 추가한다. 로그인이 생기면 `DevTokenController`를 지운다. 스타터 분석(캡챠는 LGN_ATMPT_LMT, 회원 조회는 ACNT_EML_HMAC, 소셜은 SNS_ACNT_LINK)도 함께 넘긴다
- SCR-COM-002 연결 남음: `/auth/me`의 프로필 이미지 URL은 `FileStore`·파일 조회 API(SCR-MYP-010) 뒤 연결. 지금은 `atchFileSn`만 준다
- YAY 소셜 로그인 때 nct-backend의 `global/security/handler`(OAuth2 성공·실패 핸들러)와 `CustomOAuth2UserService`를 가져올 수 있다. 위치(`auth` 도메인 또는 `global/security`)는 `docs/OPEN_ISSUES.md`에서 정한다
- SCR-AIM-008 때 nct-backend의 `global/idempotency`(멱등 요청 인터셉터, `RequestFingerprintMapper`)를 가져와 영수증 저장에 쓴다
- 디스코드 공유 `application.properties`를 맞춘다: 팀 공용 `app.crypto.*`·`app.jwt.secret-base64` 키를 정해 넣고, `mybatis.configuration.map-underscore-to-camel-case`는 넣지 않는다(추가-12, resultMap 방식)
- 공용 DB의 테스트 회원(MBR_SN 1, `ljs_test`, RGTR_ID `LJS`)은 개인 키로 암호화돼 있다. 팀 키를 정한 뒤 지우거나 다시 만든다
- 프론트엔드 저장소 변경(`src/api/axios.js` baseURL `/api`, `docs/convention/{api,pages}.md`, `scripts/spec-extract.sh`, start 스킬)을 따로 커밋한다
