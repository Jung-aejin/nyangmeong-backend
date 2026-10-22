# LJS 핸드오프

## 지금 하는 것

SCR-AIM-008 영수증 저장 (2차: 확인 후 저장) — 끝 (2026-10-22). 검증 2회차 `통과`(1회차 `실패`는 기획 밖 변경 두 건: start 스킬 수정을 추가-1로 기록, 컨트롤러 분리를 유지로 결정), 사람 확인 완료.
`GET /api/receipts/{recptSn}/image`(원본), `POST /api/receipts/{recptSn}/save-check`(가계부 중복·앞뒤 7일 미연결 병원 일정, 저장 안 함), `POST /api/receipts/{recptSn}/save`(201 `{ recptSn, ckupRcds:[{petSn, ckupRcdSn}] }`, 같은 `idmptDmndId` 재전송은 200 같은 응답). 저장은 `expense/service/ReceiptSaveService`가 한 트랜잭션으로(상태 저장완료로 먼저 변경 → 탭마다 검진기록, `expenseYn`·금액 1원 이상이면 지출). 검진기록·일정은 `CheckupRecordFake`(JAJ), 지출은 `ExpenseFake`(SJM)로 가짜.
기획과 달라진 것: `save-check`·`save`는 새 `ReceiptSaveController`(기존 `ReceiptControllerTest`를 안 건드리려고 분리, 경로는 같음). 남의 영수증 이미지는 403이 아니라 404 `RECEIPT_NOT_FOUND`. `global/idempotency`는 가져오지 않았다(`RECPT.IDMPT_DMND_ID` 유일 키로 충분). `ErrorCode.RECEIPT_NOT_SAVABLE`(409) 추가. 추가-1: `start` 스킬에 원격 최신화 1-1단계(프론트와 같은 문구).

## 다음에 할 것

- SCR-AIM-008 2차 연결 남음: health 검진기록·일정 등록(JAJ)이 나오면 `expense/service/CheckupRecordFake`를 지우고 그 service로 바꾼다(같은 날 병원 일정 연결·새 일정 생성·앞뒤 7일 미연결 일정 조회·처방약 저장, 처방약은 `CARE_RFLT_YN='N'`). 멱등 재전송 응답용 `remember`·`findByReceipt`도 DB 조회로 바꾼다
- SCR-AIM-008 2차 연결 남음: 가계부 지출 생성·중복 확인(SJM)이 나오면 `expense/service/ExpenseFake`를 지우고 그 service로 바꾼다(카테고리 병원, 입력방식 OCR, 월 한도 알림은 지출 생성 쪽)
- 프론트에 알린다: 영수증 저장 `POST /api/receipts/{recptSn}/save-check` `{ ckupYmd, pets:[{ petSn, amt, expenseYn }] }` → `{ duplicateExpenses, nearbySchedules }`, `POST /api/receipts/{recptSn}/save` `{ idmptDmndId, hsptlNm, ckupYmd, pets:[{ petSn, ckupTypeCd, ckupCn, ckupMemoCn, amt, expenseYn, schdlSn?, drugs:[{ mdcnNm, mdcnBgngYmd, mdcnEndYmd, dayMdcnNmtm, prscSttsCd }] }] }` → 201, 재전송 200. 오류: `RECEIPT_NOT_SAVABLE`·`RECEIPT_ALREADY_SAVED`·`RECEIPT_ALREADY_CANCELED`·`RECEIPT_ANALYZING`·`ALREADY_PROCESSED`(409), `RECEIPT_NOT_FOUND`(404). 중복 확인에서 [아니오]를 고른 탭은 `expenseYn=false`로 다시 보낸다. 원본 이미지는 `GET /api/receipts/{recptSn}/image`
- `.claude/skills/start/SKILL.md`의 1-1단계(추가-1)는 공용 하네스 변경이라 코드 커밋에서 빼고 따로 PR로 올린다
- `docs/api/receipt.md`에 저장 API(`save-check`·`save`·`image`)와 `RECEIPT_NOT_SAVABLE`을 추가한다(아래 업로드 항목과 함께)
- Windows 터미널에서 로그 한글이 깨진다(JVM은 UTF-8, 터미널은 cp949). 임시로 `chcp 65001`. `build.gradle` `bootRun`에 `-Dstdout.encoding=UTF-8`을 넣을지 팀과 정한다
- SCR-AIM-008 연결 남음: pet 연동 반려동물 목록(SJM)이 나오면 `expense/service/CaregiverPetFake`(빈 목록)를 지우고 그 service로 바꾼다. 그 전에는 구역 이름 자동 연결(FN-AIM-009-17)이 동작하지 않는다
- SCR-AIM-008 연결 남음: OCR 외부 API 규격이 정해지면 `OcrClient` 실제 구현을 두고 `app.ocr.client`를 바꾼다(지금은 `FakeOcrClient`, 값별 결과는 `OcrProperties` 주석)
- SJM에게 확인: 가계부 모달의 영수증 저장을 `ExpenseService`가 직접 하는지. 영수증 쪽이 하라면 `receipt`(4층) 분리를 다시 검토한다(`docs/OPEN_ISSUES.md` 영수증 행)
- `docs/api/receipt.md` 초안을 구현에 맞춘다: 업로드 요청은 `ocrDmndSrcCd` 대신 `petSn`, `POST /receipts/{recptSn}/analyze` 추가, 응답에 `failRsn`·`dstrbDscntAmt`·`sectionAmt`·`ckupCn`
- 프론트에 알린다: 영수증 업로드는 실패도 201이고 `ocrPrcsSttsCd`(AIMG01)·`failRsn`으로 문구를 고른다. 오류: `FILE_TYPE_NOT_ALLOWED`·`FILE_TOO_LARGE`(400), `RECEIPT_ANALYZING`·`RECEIPT_ALREADY_CANCELED`·`ALREADY_PROCESSED`(409)
- 로컬 DB(`nyangmeong_local`, `application-local.properties`) 확인 데이터 정리: RECPT 1~3과 그 RECPT_STTS_HSTRY, 그 ATCH_FILE(`SELECT ATCH_FILE_SN FROM RECPT`로 찾는다)과 `uploads/` 아래 파일. 확인용 회원 1·PET 4~6·BRUP_REL·SYS_STNG는 다음 확인에 쓰도록 둔다
- SCR-AIM-006 연결 남음: health 병원 일정 조회(JAJ)가 나오면 `scheduler/HospitalScheduleFake`를, pet 연동 양육자 조회(SJM, 기능 분배 3.1절 6번)가 나오면 `scheduler/CaregiverFake`를 지우고 그 service로 바꾼다
- SCR-AIM-006 연결 남음: 팀이 SMTP 계정을 정하면 `spring-boot-starter-mail`을 추가하고 `MailSender`의 SMTP 구현을 둔다. 그 전까지 메일은 로그만 남는다
- SCR-AIM-006 연결 남음: FN-AIM-006-11·13 화면 확인과 FN-AIM-006-01·02·20 실제 조회는 위 두 가짜를 연결한 뒤 공용 DB 병원 일정으로 확인한다
- 디스코드 공유 `application.properties`에 `app.front-base-url=http://localhost:5173`을 추가한다(운영은 프론트 실제 주소)
- 팀에 알린다: 여러 도메인 배치 패키지 이름이 `job`에서 `scheduler`로 바뀌었다(AGENTS.md 층 표·`ArchitectureTest`). 기능 분배 문서(정본)는 아직 `job`과 AIM-006 담당 YAY로 적혀 있어, 정본을 고칠 때 10-19 담당 조정과 함께 맞춘다
- 팀에 알린다: 케어 일정 사전 알림은 `EventPublisher.publish(new CareScheduleAlarmEvent(...))`, 메일 발송은 `global/mail/MailSender`(지금은 로그만). 배치 스케줄러는 `global/batch/SchedulingConfig`의 `taskScheduler`

- SCR-MYP-011 연결 남음: 포인트 조회·차감(SJM, FN-MYP-020)이 나오면 `pet/service/PointFake`를 지우고 그 service로 바꾼다. 구매의 `idmptDmndId`를 포인트 원장에 넘긴다
- 프론트에 알린다: 상점 API `GET /api/pixel-characters/{pixelCharSn}/shop-items?itemRegnCd=` → `{ hldPnt, items: [{ itemSn, itemNm, itemRegnCd, needPnt, hldYn }] }`, 구매 `POST /api/pixel-characters/{pixelCharSn}/held-items` `{ idmptDmndId, itemSns[] }` → 201 `PixelCharacterResponse`. 오류: `POINT_NOT_ENOUGH`(400), `ITEM_ALREADY_HELD`(409), `ITEM_NOT_FOUND`(404), `PET_PRIMARY_ONLY`(403)
- HLD_ITEM 유일 키 `UK_HLD_ITEM_1 (PIXEL_CHAR_SN, ITEM_SN)`에 DEL_YN이 없다. 보유 아이템 소프트 삭제 기능이 생기면 재구매가 막히므로 그때 키를 고칠지 정한다

- 프론트에 알린다: 알림 설정 API `GET·PUT /api/notifications/settings` 필드는 `maskedAcntEml`, `emlNotiRcptnYn`, `inappNotiRcptnYn`, `careSchdlNotiRcptnYn`, `leadTimes`(`TODAY`·`D1`·`D3`·`D7`), `mtchDlngNotiRcptnYn`, `cmntNotiRcptnYn`, `lostExpryNotiRcptnYn`, `expndLimNotiRcptnYn`, `expndLimSetYn`(GET만). 케어 일정 ON에 `leadTimes`가 비면 400 `INVALID_INPUT_VALUE`
- 로컬 DB 회원 1의 이메일을 지금 `app.crypto.*` 키로 `ljs_test@example.com`으로 다시 암호화했다(원래 암호문은 개인 키라 풀리지 않았다). 팀 키를 정하면 다시 맞춘다
- SCR-MYP-010 연결 남음: FN-MYP-010-24 1:1 문의 작성(SCR-CSC-003, KSJ)이 `PixelCharacterService.receiveErrorInquiry(mbrSn, pixelCharSn, otoInqrySn)`를 부르고, 돌려받은 결과 ATCH_FILE_SN을 문의에 첨부한다
- SCR-MYP-010 연결 남음: FN-MYP-010-23 관리자 확인 화면(담당 미정)이 `finishErrorCheck(pixelCharSn, regenerate)`를 부른다
- FN-MYP-010-19 규격 미확보 시 베이스 캐릭터 선택 방식 전환: 캐릭터 생성 외부 API가 정해지면 `CharacterGenerator` 실제 구현과 함께 판단한다. 기본 캐릭터 이미지(`resources/character/default-*.png`)도 디자인 이미지로 바꾼다
- 잘못된 Content-Type(multipart 아님 등)이 415가 아니라 500으로 나간다. `GlobalExceptionHandler`에 `HttpMediaTypeNotSupportedException` 처리를 넣을지 정한다
- 팀에 알린다: 반려동물 권한은 `global/auth/PetAuthorizer`(`checkCaregiver`·`checkPrimary`), 파일은 `global/file/FileStore`(`store`·`load`), 공통코드는 `GET /api/common-codes?prntComCd=`. 범용 파일 조회 API는 두지 않는다(`docs/OPEN_ISSUES.md`)
- 로컬 DB 테스트 데이터 정리: RGTR_ID·MDFR_ID `LJS`인 PET 4~6·BRUP_REL, PIXEL_CHAR 2와 그 작업·사진·이력, ITEM 1~3·HLD_ITEM, SCR-MYP-011 확인용 ITEM 6·7(`확인용` 이름)과 그 HLD_ITEM, ATCH_FILE 1~5와 `uploads/` 아래 파일
- SCR-COM-004 연결 남음: FN-COM-004-08 공동양육 초대 수락·거절(SJM) API가 나오면 알림 행 버튼과 연결하고, 초대 알림 `mvmnPath` 형식을 SJM과 맞춘다
- 팀에 알린다: 알림은 `EventPublisher.publish(new NotificationEvent(mbrSn, NotificationType.X, 제목, 내용, mvmnPath))`로 발행한다(@Transactional 안). 프론트는 읽음 처리를 `POST /api/notifications/{inappNotiSn}/read`로 부른다
- 팀에 알린다: 개인 로컬 DB는 `application-local.properties`(git 제외)에 `spring.datasource.*` 세 줄을 두면 공용 설정을 덮어쓴다. 로컬 DB는 `nyangmeong_` 접두어 스키마에 `db/` SQL 두 개를 실행해 만든다
- 잘못된 tab 400 응답의 `data[].message`에 Java 변환 실패 문구가 그대로 나온다. 공통 처리(GlobalExceptionHandler)에서 사용자 문구로 바꿀지 정한다
- 공용 DB 정리: 검증·사람 확인 때 생긴 RFRSH_TKN 테스트 행(MBR_SN 1)을 지운다 `DELETE FROM RFRSH_TKN WHERE MBR_SN = 1 AND REG_DT >= '2026-10-21'`
- 팀 안건: application.properties를 스타터처럼 커밋하는 일반 설정 + git 제외 `application-secret.properties`로 나눌지 `docs/OPEN_ISSUES.md`에 올린다
- YAY에게 알린다: 로그인·재발급·로그아웃은 `auth` 도메인(`auth/controller/AuthController`, 접두어 `/api`)에 만들고 `LoginSessionManager`의 `issue`·`rotate`·`revoke`를 쓴다. 새로 여는 경로(로그인 상태 확인·보안코드 등)는 `app.security.permit-all-paths`에 추가한다. 로그인이 생기면 `DevTokenController`를 지운다. 스타터 분석(캡챠는 LGN_ATMPT_LMT, 회원 조회는 ACNT_EML_HMAC, 소셜은 SNS_ACNT_LINK)도 함께 넘긴다
- SCR-COM-002 연결 남음: `/auth/me`의 프로필 이미지 URL은 `FileStore`·파일 조회 API(SCR-MYP-010) 뒤 연결. 지금은 `atchFileSn`만 준다
- YAY 소셜 로그인 때 nct-backend의 `global/security/handler`(OAuth2 성공·실패 핸들러)와 `CustomOAuth2UserService`를 가져올 수 있다. 위치(`auth` 도메인 또는 `global/security`)는 `docs/OPEN_ISSUES.md`에서 정한다
- `global/validation`(공통 검증, `PhoneNumberNormalizer` 등)은 내 마일스톤에 쓰는 화면이 없다. 처음 필요한 YAY(닉네임·휴대전화·비밀번호 검증)가 요청하면 그때 만든다
- 디스코드 공유 `application.properties`를 맞춘다: 팀 공용 `app.crypto.*`·`app.jwt.secret-base64` 키를 정해 넣고, `mybatis.configuration.map-underscore-to-camel-case`는 넣지 않는다(추가-12, resultMap 방식)
- 프론트엔드 저장소 변경(`src/api/axios.js` baseURL `/api`, `docs/convention/{api,pages}.md`, `scripts/spec-extract.sh`, start 스킬)을 따로 커밋한다
