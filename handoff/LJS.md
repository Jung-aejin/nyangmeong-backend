# LJS 핸드오프

## 지금 하는 것

SCR-COM-004 알림 드롭다운 — 끝 (2026-10-21). 검증 1회차(연결 남음만), 사람 확인(로컬 DB에서 Postman 개수·탭 목록·읽음, 서비스·발행기 테스트) 완료.
`global/notification`(EventPublisher·NotificationEvent·MailEvent·NotificationType·NotificationTab), `notification` 도메인(커밋 뒤 REQUIRES_NEW 저장 리스너, 수신 OFF 미생성, 미읽음 개수·탭별 최근 5건·읽음 API). 기획과 달라진 것(읽음은 POST, 목록은 NotificationSummary, 탈퇴 회원은 MBR_STTS_CD로 거름)은 커밋 메시지 본문에 있다.

## 다음에 할 것

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
- SCR-AIM-008 때 nct-backend의 `global/idempotency`(멱등 요청 인터셉터, `RequestFingerprintMapper`)를 가져와 영수증 저장에 쓴다
- 디스코드 공유 `application.properties`를 맞춘다: 팀 공용 `app.crypto.*`·`app.jwt.secret-base64` 키를 정해 넣고, `mybatis.configuration.map-underscore-to-camel-case`는 넣지 않는다(추가-12, resultMap 방식)
- 공용 DB의 테스트 회원(MBR_SN 1, `ljs_test`, RGTR_ID `LJS`)은 개인 키로 암호화돼 있다. 팀 키를 정한 뒤 지우거나 다시 만든다
- 프론트엔드 저장소 변경(`src/api/axios.js` baseURL `/api`, `docs/convention/{api,pages}.md`, `scripts/spec-extract.sh`, start 스킬)을 따로 커밋한다
