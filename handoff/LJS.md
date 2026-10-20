# LJS 핸드오프

## 지금 하는 것

SCR-COM-002 기본 레이아웃 (공통 기반 + JWT 인증 기반 + 회원 요약) — 끝 (2026-10-20 16:21). 검증 통과, 사람 확인(Postman) 완료.
기능 ID와 추가 요청 1~13은 이 마일스톤 커밋 메시지 본문에 있다.

## 다음에 할 것

- SCR-COM-002 연결 남음: YAY가 `POST /api/auth/login`·`logout`·`refresh`를 만들면 `DevTokenController`를 지운다. 로그인은 `JwtTokenProvider`·`TokenCookies`·`TokenHashUtil`을 쓰고 Refresh 토큰 해시를 RFRSH_TKN에 저장한다. 재발급은 `TOKEN_EXPIRED` 뒤 프론트가 부른다
- SCR-COM-002 연결 남음: `/auth/me`의 프로필 이미지 URL은 `FileStore`·파일 조회 API(SCR-MYP-010) 뒤 연결. 지금은 `atchFileSn`만 준다
- YAY 소셜 로그인 때 nct-backend의 `global/security/handler`(OAuth2 성공·실패 핸들러)와 `CustomOAuth2UserService`를 가져올 수 있다. 위치(`auth` 도메인 또는 `global/security`)는 `docs/OPEN_ISSUES.md`에서 정한다
- SCR-AIM-008 때 nct-backend의 `global/idempotency`(멱등 요청 인터셉터, `RequestFingerprintMapper`)를 가져와 영수증 저장에 쓴다
- 이미 있던 `ArchitectureTest`·`MapperXmlRuleTest`·`NyangmeongBackendApplicationTests`의 주석을 명사형 규칙(`docs/convention/common.md`)에 맞춰 고친다. 검증 루프 중에는 기준 파일이라 손대지 않았다
- 메서드 주석 3개의 괄호 조건을 지울지 정한다: `FieldCryptoService` "(실패 시 기동 중단)", `TokenCookies` "(없으면 null)", `MemberAuthority` "(미지정은 ROLE_USER)". 새 주석 규칙("메서드는 역할 한 줄, 조건·결과 금지")과 어긋날 수 있다고 검증이 권장함
- 디스코드 공유 `application.properties`를 맞춘다: 팀 공용 `app.crypto.*`·`app.jwt.secret-base64` 키를 정해 넣고, `mybatis.configuration.map-underscore-to-camel-case`는 넣지 않는다(추가-12, resultMap 방식)
- 공용 DB의 테스트 회원(MBR_SN 1, `ljs_test`, RGTR_ID `LJS`)은 개인 키로 암호화돼 있다. 팀 키를 정한 뒤 지우거나 다시 만든다
- YAY에게 알린다: 로그인·로그아웃·재발급 API는 `auth` 도메인(`auth/controller/AuthController`)에, 접두어는 `/api`(추가-9·10)
- 프론트엔드 저장소 변경(`src/api/axios.js` baseURL `/api`, `docs/convention/{api,pages}.md`, `scripts/spec-extract.sh`, start 스킬)을 따로 커밋한다
