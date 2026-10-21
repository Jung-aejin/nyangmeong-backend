# LJS 핸드오프

## 지금 하는 것

SCR-COM-008 설정 (알림) — 끝 (2026-10-21). 검증 2회차 통과(1회차는 로컬 DB 회원 1 이메일 복호화 실패로 화면 확인 못 함), 사람 확인 완료.
`GET·PUT /api/notifications/settings`(notification 도메인): 채널·케어 일정·사전 알림 시점(TODAY·D1·D3·D7)·펫돌봄 거래·커뮤니티·가계부 수신 여부, 가린 가입 이메일, 월 지출 한도 설정 여부(MBR.MM_EXPND_LIM_AMT, SJM API 대신). 기획과 달라진 것: 요청·응답 필드 이름을 DTO 규칙대로 컬럼식(`emlNotiRcptnYn` 등)으로 씀.

## 다음에 할 것

- OAuth 소셜 로그인을 따로 기획한다(`/start`). nct-backend의 `global/security/handler`(OAuth2 성공·실패 핸들러)·`CustomOAuth2UserService`, `spring-boot-starter-oauth2-client` 의존성, `application.properties.example` 키, `SecurityConfig` 연결. 소셜 제공자와 YAY 담당 여부를 먼저 정한다
- 프론트에 알린다: 알림 설정 API `GET·PUT /api/notifications/settings` 필드는 `maskedAcntEml`, `emlNotiRcptnYn`, `inappNotiRcptnYn`, `careSchdlNotiRcptnYn`, `leadTimes`(`TODAY`·`D1`·`D3`·`D7`), `mtchDlngNotiRcptnYn`, `cmntNotiRcptnYn`, `lostExpryNotiRcptnYn`, `expndLimNotiRcptnYn`, `expndLimSetYn`(GET만). 케어 일정 ON에 `leadTimes`가 비면 400 `INVALID_INPUT_VALUE`
- 로컬 DB 회원 1의 이메일을 지금 `app.crypto.*` 키로 `ljs_test@example.com`으로 다시 암호화했다(원래 암호문은 개인 키라 풀리지 않았다). 팀 키를 정하면 다시 맞춘다
- SCR-MYP-010 연결 남음: FN-MYP-010-24 1:1 문의 작성(SCR-CSC-003, KSJ)이 `PixelCharacterService.receiveErrorInquiry(mbrSn, pixelCharSn, otoInqrySn)`를 부르고, 돌려받은 결과 ATCH_FILE_SN을 문의에 첨부한다
- SCR-MYP-010 연결 남음: FN-MYP-010-23 관리자 확인 화면(담당 미정)이 `finishErrorCheck(pixelCharSn, regenerate)`를 부른다
- FN-MYP-010-19 규격 미확보 시 베이스 캐릭터 선택 방식 전환: 캐릭터 생성 외부 API가 정해지면 `CharacterGenerator` 실제 구현과 함께 판단한다. 기본 캐릭터 이미지(`resources/character/default-*.png`)도 디자인 이미지로 바꾼다
- 잘못된 Content-Type(multipart 아님 등)이 415가 아니라 500으로 나간다. `GlobalExceptionHandler`에 `HttpMediaTypeNotSupportedException` 처리를 넣을지 정한다
- 디스코드 공유 `application.properties`에 `app.file.root-dir=uploads`, `spring.servlet.multipart.max-file-size=10MB`, `spring.servlet.multipart.max-request-size=10MB`, `app.character.generator=fake`를 추가한다
- 팀에 알린다: 반려동물 권한은 `global/auth/PetAuthorizer`(`checkCaregiver`·`checkPrimary`), 파일은 `global/file/FileStore`(`store`·`load`), 공통코드는 `GET /api/common-codes?prntComCd=`. 범용 파일 조회 API는 두지 않는다(`docs/OPEN_ISSUES.md`)
- 로컬 DB 테스트 데이터 정리: RGTR_ID·MDFR_ID `LJS`인 PET 4~6·BRUP_REL, PIXEL_CHAR 2와 그 작업·사진·이력, ITEM 1~3·HLD_ITEM, ATCH_FILE 1~5와 `uploads/` 아래 파일
- SCR-COM-004 연결 남음: FN-COM-004-08 공동양육 초대 수락·거절(SJM) API가 나오면 알림 행 버튼과 연결하고, 초대 알림 `mvmnPath` 형식을 SJM과 맞춘다
- 팀에 알린다: 알림은 `EventPublisher.publish(new NotificationEvent(mbrSn, NotificationType.X, 제목, 내용, mvmnPath))`로 발행한다(@Transactional 안). 프론트는 읽음 처리를 `POST /api/notifications/{inappNotiSn}/read`로 부른다
- 팀에 알린다: 개인 로컬 DB는 `application-local.properties`(git 제외)에 `spring.datasource.*` 세 줄을 두면 공용 설정을 덮어쓴다. 로컬 DB는 `nyangmeong_` 접두어 스키마에 `db/` SQL 두 개를 실행해 만든다
- 잘못된 tab 400 응답의 `data[].message`에 Java 변환 실패 문구가 그대로 나온다. 공통 처리(GlobalExceptionHandler)에서 사용자 문구로 바꿀지 정한다
- 공용 DB 정리: 검증·사람 확인 때 생긴 RFRSH_TKN 테스트 행(MBR_SN 1)을 지운다 `DELETE FROM RFRSH_TKN WHERE MBR_SN = 1 AND REG_DT >= '2026-10-21'`
- 디스코드 공유 `application.properties`에 `app.security.permit-all-paths` 줄을 추가한다(example과 같은 값)
- 팀 안건: application.properties를 스타터처럼 커밋하는 일반 설정 + git 제외 `application-secret.properties`로 나눌지 `docs/OPEN_ISSUES.md`에 올린다
- YAY에게 알린다: 로그인·재발급·로그아웃은 `auth` 도메인(`auth/controller/AuthController`, 접두어 `/api`)에 만들고 `LoginSessionManager`의 `issue`·`rotate`·`revoke`를 쓴다. 새로 여는 경로(로그인 상태 확인·보안코드 등)는 `app.security.permit-all-paths`에 추가한다. 로그인이 생기면 `DevTokenController`를 지운다. 스타터 분석(캡챠는 LGN_ATMPT_LMT, 회원 조회는 ACNT_EML_HMAC, 소셜은 SNS_ACNT_LINK)도 함께 넘긴다
- SCR-COM-002 연결 남음: `/auth/me`의 프로필 이미지 URL은 `FileStore`·파일 조회 API(SCR-MYP-010) 뒤 연결. 지금은 `atchFileSn`만 준다
- YAY 소셜 로그인 때 nct-backend의 `global/security/handler`(OAuth2 성공·실패 핸들러)와 `CustomOAuth2UserService`를 가져올 수 있다. 위치(`auth` 도메인 또는 `global/security`)는 `docs/OPEN_ISSUES.md`에서 정한다
- `global/validation`(공통 검증, `PhoneNumberNormalizer` 등)은 내 마일스톤에 쓰는 화면이 없다. 처음 필요한 YAY(닉네임·휴대전화·비밀번호 검증)가 요청하면 그때 만든다
- SCR-AIM-008 때 nct-backend의 `global/idempotency`(멱등 요청 인터셉터, `RequestFingerprintMapper`)를 가져와 영수증 저장에 쓴다
- 디스코드 공유 `application.properties`를 맞춘다: 팀 공용 `app.crypto.*`·`app.jwt.secret-base64` 키를 정해 넣고, `mybatis.configuration.map-underscore-to-camel-case`는 넣지 않는다(추가-12, resultMap 방식)
- 공용 DB의 테스트 회원(MBR_SN 1, `ljs_test`, RGTR_ID `LJS`)은 개인 키로 암호화돼 있다. 팀 키를 정한 뒤 지우거나 다시 만든다
- 프론트엔드 저장소 변경(`src/api/axios.js` baseURL `/api`, `docs/convention/{api,pages}.md`, `scripts/spec-extract.sh`, start 스킬)을 따로 커밋한다
