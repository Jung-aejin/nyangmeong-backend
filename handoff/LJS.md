# LJS 핸드오프

## 지금 하는 것

없음

이전: 하네스 테스트(415와 사람이 읽는 변환 오류 문구) — 끝 (2026-10-22). `ErrorCode.UNSUPPORTED_MEDIA_TYPE`과 `HttpMediaTypeNotSupportedException` 핸들러, 타입 변환 실패의 `data[].message`를 "형식이 올바르지 않습니다."로. 검증 1회차 통과, 사람 확인. 새 스크립트 start→impl→verify→end를 처음 끝까지 돌림. 하네스 스크립트화 변경(`scripts/`·스킬)은 아직 커밋 전.

## 다음에 할 것

- 하네스 스크립트화는 415 수정과 같은 PR로 올렸다(2026-10-22). 남은 것: 프론트 저장소의 `scripts/verify-loop.sh`에도 `scripts` 보호 줄을 맞춘다. 테스트에서 본 것 두 가지를 고친다: 검증 에이전트가 verify 스킬 6번대로 `verify-loop.sh clear`를 부르다 권한 분류기에 막혔다(AGENTS.md는 `clear`를 사람·`/end`만 한다고 하므로 verify 스킬 6번을 맞춘다). `pr-prep.sh`는 커밋하지 않은 파일이 있으면 멈춰서, 따로 올릴 하네스 변경이 남아 있으면 PR을 못 만든다
- 공용 DB `RFRSH_TKN`에 하네스 테스트 검증 때 생긴 회원 1 dev-tokens 행(2026-10-22)을 정리할지 정한다(사람)
- ERDCloud `EXPND.IDMPT_DMND_ID`를 `EXPND_YMD` 뒤로 옮긴다(화면에서 끌기, 사람)
- 팀에 알린다(2026-10-22): `docs/convention/api.md` 새 규칙 4가지, `docs/api/README.md`(구현하며 달라지면 같은 PR에서 초안도 고친다), `/start`가 `docs/api`를 기능 ID로 찾아 쓴다, 테이블 변경 SQL은 `db/migration/`에(공용 DB에는 이것만 실행)
- YAY에게 알린다: 로그인·SNS 로그인 응답에 토큰을 넣지 않는다(`TokenResponse` 삭제, `/auth/refresh`는 `data` 없음). SNS 흐름을 정할 때 `issues.md` 로그인 2번 2026-10-22 추가 4가지를 함께 정한다
- SJM에게 알린다: 지출 다중 삭제는 `DELETE /expenses?expndSn=`와 `POST /expenses/bulk-delete`(`expectedCnt` 확인, LJS가 더한 안전장치), 지출 등록은 줄마다 `idmptDmndId`
- KSJ에게 알린다: 채팅 메시지 커서 조회, 실시간 STOMP 제안과 다중 로그인 다섯 가지(`docs/api/community.md` "실시간"). 세션 폐기 때 소켓을 끊는 이벤트는 `LoginSessionManager.revoke`에서 같이 정한다. 좋아요는 멱등
- JAJ에게 알린다: 케어 완료 중복은 `UK_CARE_CMPTN_HSTRY_1`로 200 기존 기록, 검사결과지 분석은 동기+제한 시간과 `anlysRslt`(제안, 확인 필요)
- 프론트에 알린다: 알림·상점·영수증 API는 `docs/api/notification.md`·`pet.md`·`receipt.md`(구현에 맞춤). 알림 설정 경로는 `/api/members/me/notification-settings`로 바뀌었다. 영수증 업로드는 실패도 201이고 `ocrPrcsSttsCd`·`failRsn`으로 문구를 고른다. 중복 확인에서 [아니오]를 고른 탭은 `expenseYn=false`
- 프론트 `start` 스킬에도 `docs/api`(백엔드 저장소) 기준 줄을 넣을지 정한다
- 정하기: 휴대폰 일정 가져오기(기획에 없음, JAJ 영역). 넣는다면 QR 짝짓기 `.ics` 업로드, iPhone 단축어, 웹 → 휴대폰은 `webcal://` 구독 QR
- 정하기: 파일 저장소 운영(지금 로컬 디스크 `uploads/`. 서버 여러 대·재배포 대비 영구 볼륨 또는 S3), 영상 업로드 형식·용량
- SCR-AIM-008 연결 남음: health 검진기록·일정 등록(JAJ)이 나오면 `expense/service/CheckupRecordFake`를 그 service로(같은 날 병원 일정 연결·새 일정·앞뒤 7일 미연결 일정·처방약 `CARE_RFLT_YN='N'`), 멱등 재전송용 `remember`·`findByReceipt`도 DB 조회로
- SCR-AIM-008 연결 남음: 가계부 지출 생성·중복 확인(SJM)이 나오면 `ExpenseFake`를, pet 연동 반려동물 목록(SJM)이 나오면 `CaregiverPetFake`를 그 service로(그 전에는 FN-AIM-009-17 구역 자동 연결이 안 된다). 지출 생성에 줄마다 `idmptDmndId`를 넘긴다
- SCR-AIM-008 연결 남음: OCR 외부 API 규격이 정해지면 `OcrClient` 실제 구현을 두고 `app.ocr.client`를 바꾼다(지금 `FakeOcrClient`)
- SJM에게 확인: 가계부 모달의 영수증 저장을 `ExpenseService`가 직접 하는지. 영수증 쪽이 하라면 `receipt`(4층) 분리를 다시 검토한다(`docs/OPEN_ISSUES.md` 영수증 행)
- SCR-AIM-006 연결 남음: health 병원 일정 조회(JAJ)가 나오면 `scheduler/HospitalScheduleFake`를, pet 연동 양육자 조회(SJM)가 나오면 `scheduler/CaregiverFake`를 그 service로. 그 뒤 FN-AIM-006-11·13 화면과 01·02·20 실제 조회를 공용 DB 병원 일정으로 확인한다
- SCR-AIM-006 연결 남음: 팀이 SMTP 계정을 정하면 `spring-boot-starter-mail`과 `MailSender` SMTP 구현. 그 전까지 메일은 로그만
- SCR-MYP-011 연결 남음: 포인트 조회·차감(SJM, FN-MYP-020)이 나오면 `pet/service/PointFake`를 그 service로. 구매의 `idmptDmndId`를 포인트 원장에 넘긴다
- SCR-MYP-010 연결 남음: FN-MYP-010-24 1:1 문의 작성(SCR-CSC-003, KSJ)이 `PixelCharacterService.receiveErrorInquiry(mbrSn, pixelCharSn, otoInqrySn)`를 부르고 결과 ATCH_FILE_SN을 첨부한다
- SCR-MYP-010 연결 남음: FN-MYP-010-23 관리자 확인 화면(담당 미정)이 `finishErrorCheck(pixelCharSn, regenerate)`를 부른다
- FN-MYP-010-19: 캐릭터 생성 외부 API가 정해지면 `CharacterGenerator` 실제 구현과 베이스 캐릭터 선택 방식을 판단한다. 기본 캐릭터 이미지(`resources/character/default-*.png`)도 디자인 이미지로
- SCR-COM-004 연결 남음: FN-COM-004-08 공동양육 초대 수락·거절(SJM) API가 나오면 알림 행 버튼과 연결하고 초대 알림 `mvmnPath` 형식을 SJM과 맞춘다
- SCR-COM-002 연결 남음: `/auth/me`의 프로필 이미지 URL은 `FileStore`·파일 조회 API 뒤 연결. 지금은 `atchFileSn`만
- 정하기: HLD_ITEM 유일 키 `UK_HLD_ITEM_1`에 DEL_YN이 없다. 보유 아이템 소프트 삭제가 생기면 재구매가 막히므로 그때 키를 고친다
- 정하기: Windows 터미널 로그 한글 깨짐(임시 `chcp 65001`). `bootRun`에 `-Dstdout.encoding=UTF-8`을 넣을지
- 팀에 알린다: 배치 패키지 `job` → `scheduler`(기능 분배 정본은 아직 `job`과 AIM-006 담당 YAY), 케어 일정 사전 알림 `CareScheduleAlarmEvent`, 메일 `global/mail/MailSender`(로그만), 스케줄러 `global/batch/SchedulingConfig`
- 팀에 알린다: 알림은 `EventPublisher.publish(new NotificationEvent(...))`(@Transactional 안), 반려동물 권한 `global/auth/PetAuthorizer`, 파일 `global/file/FileStore`, 공통코드 `GET /api/common-codes?prntComCd=`. 범용 파일 조회 API는 두지 않는다
- 팀에 알린다: 개인 로컬 DB는 `application-local.properties`(git 제외)에 `spring.datasource.*` 세 줄. `nyangmeong_` 접두어 스키마에 `db/` 기준 SQL 두 개와 `db/migration/`을 차례로 실행해 만든다
- YAY에게 알린다: 로그인·재발급·로그아웃은 `auth/controller/AuthController`에 `LoginSessionManager`의 `issue`·`rotate`·`revoke`로. 새 공개 경로는 `app.security.permit-all-paths`에. 로그인이 생기면 `DevTokenController` 삭제. 스타터 분석(캡챠 LGN_ATMPT_LMT, 회원 조회 ACNT_EML_HMAC, 소셜 SNS_ACNT_LINK)과 nct-backend OAuth2 핸들러·`CustomOAuth2UserService`(위치는 `docs/OPEN_ISSUES.md`)도 넘긴다
- `global/validation`은 처음 필요한 YAY(닉네임·휴대전화·비밀번호 검증)가 요청하면 만든다
- 디스코드 공유 `application.properties`: `app.front-base-url=http://localhost:5173` 추가, 팀 공용 `app.crypto.*`·`app.jwt.secret-base64` 키를 정해 넣고 `mybatis.configuration.map-underscore-to-camel-case`는 넣지 않는다. 키가 정해지면 로컬 DB 회원 1 이메일(`ljs_test@example.com`)도 다시 암호화한다
- 팀 안건: application.properties를 일반 설정(커밋) + git 제외 `application-secret.properties`로 나눌지 `docs/OPEN_ISSUES.md`에 올린다
- 로컬 DB 정리(`nyangmeong_local`): RECPT 1~3·그 RECPT_STTS_HSTRY·ATCH_FILE, RGTR_ID `LJS`인 PET 4~6·BRUP_REL, PIXEL_CHAR 2와 작업·사진·이력, ITEM 1~3·6·7과 HLD_ITEM, ATCH_FILE 1~5, `uploads/` 아래 파일, RFRSH_TKN 회원 1 테스트 행(`REG_DT >= '2026-10-22'`). 확인용 회원 1·SYS_STNG는 둔다
- 공용 DB 정리: RFRSH_TKN 테스트 행 `DELETE FROM RFRSH_TKN WHERE MBR_SN = 1 AND REG_DT >= '2026-10-21'`
- 프론트엔드 저장소 변경(`src/api/axios.js` baseURL `/api`, `docs/convention/{api,pages}.md`, `scripts/spec-extract.sh`, start 스킬)을 따로 커밋한다
