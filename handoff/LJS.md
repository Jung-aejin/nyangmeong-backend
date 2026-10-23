# LJS 핸드오프

## 지금 하는 것

공용 DB 시드 데이터 생성기 — 구현 (시작 2026-10-23 14:40, 대화에서 기획·승인)

- 목표: 빈 공용 DB(기준 SQL 2개 + `db/migration/`)에 명령 하나로 화면 확인용 데이터를 넣는다. 기본 회원 200 / 반려동물 1,000 / 게시글 1,000, `-Pscale=N`으로 배수. 선생님 방침으로 공용 DB가 대상이고, 개발 검증은 로컬 DB(`-Ptarget=local`)로 한다
- 바꿀 파일: `build.gradle`(소스 묶음 `seed`, 태스크 `seedData`·`seedFiles`, `check`가 `seedClasses`를 컴파일), `src/seed/java/com/nyangmeong/seed/**`, `README.md`(실행 방법·고정 계정), `docs/convention/shared-files.md`(공용 DB 시드 규칙)
- 방식: 메모리에서 행을 만들고 한 트랜잭션으로 넣는다(FK 검사 끄고 넣은 뒤 커밋 전에 FK·공통코드 그룹·업무 규칙 검사, 실패하면 롤백). PK는 1부터 직접 매긴다. 난수 시드 고정, 날짜는 실행일 기준. 암호화는 `FieldCryptoService`, 비밀번호는 bcrypt(모든 계정 같은 개발용 값)
- 안전장치: 시드 대상 테이블에 행이 있으면 거절. 공용 DB는 DB 이름을 입력(`-Pconfirm=<DB이름>`)해야 진행, `-Preset` 금지. 로컬은 `localhost`·`127.0.0.1`만, `-Preset`으로 비우고 다시 생성
- 넣지 않는 테이블: `COM_CD`(기준 SQL), `RFRSH_TKN`·`LGN_ATMPT_LMT`·`EML_CERT`·`NOTI_SNDNG_LOG`(실행 중 쌓이는 기록), `NOTI_TMPLT`(없으면 코드 기본 문구를 쓴다. 넣으면 동작이 바뀐다)
- 이미지: 생성기가 png를 만들어 `app.file.root-dir`에 쓰고 `ATCH_FILE`(RGTR_ID `SEED`)에 연결. 다른 PC는 `./gradlew seedFiles`로 같은 파일을 다시 만든다
- 완료 조건
  - 명령: `./gradlew build -q` 통과
  - 명령: 빈 로컬 DB에 기준 SQL + migration 후 `./gradlew seedData -Ptarget=local` 종료 0. 다시 실행하면 거절, `-Preset`으로 다시 만들면 테이블별 행 수가 같다
  - 명령: 커밋 전 검사(FK 위반 0, 공통코드 그룹 일치, 회원당 반려동물 ≤ `PET_MAX_CNT`, 반려동물당 양육자 ≤ `JNT_BRUP_MAX_NOPE`, 대표 반려동물 1마리, `MBR.HLD_PNT` = 포인트 원장 잔액, `PST.RCMDTN_CNT` = 좋아요 수)
  - 명령: `-Ptarget=shared`에서 DB 이름이 틀리면 아무것도 넣지 않고 종료, `-Preset`과 함께 쓰면 거절
  - 화면: `bootRun` 후 dev-token 1~4번 계정으로 `/auth/me`·`/notifications`·`/pixel-characters`·`/members/me/notification-settings`가 데이터를 돌려주고 `/items/{sn}/image`가 200
- 새 의존성: 없음
- 결과(2026-10-23): 구현 끝, 사람 확인 전. 로컬 테스트 DB `nyangmeong_seedtest`(기준 SQL + 공통코드 + migration)에서 151,430행·이미지 4,060개·약 25초, 커밋 전 검사 통과. `-Preset` 두 번 연속 행 수 같음, `-Pscale=2` 통과(305,604행·60초), `seedFiles` 8,168개 재생성. 거절 경로 4개(데이터 있음·공용 DB 이름 틀림·공용 reset·배율 0) 확인. 8082 서버를 테스트 DB에 붙여 1~4번 계정으로 `/auth/me`·알림·알림 설정·픽셀 캐릭터·캐릭터 이미지·아이템 이미지 200(암호화 이메일 복호화 확인). `./gradlew build -q` 통과
- 구현 중 고친 것: 공동양육자와 대표 반려동물이 겹쳐 `UK_DAY_MSN_1` 중복(반려동물마다 한 번만), 날짜 함수가 분기에 따라 난수를 다르게 뽑아 실행마다 달라짐(항상 같은 개수), 반려동물 없는 회원에게 캐릭터·케어 알림
- 남은 일
  - 사람 확인: README "시드 데이터"의 고정 계정으로 화면 확인
  - 공용 DB 실제 실행: 끝(2026-10-23, 사람이 `seedClean` → `seedData`). 회원 200·반려동물 1,000·게시글 1,000·시드 이미지 4,060, `RFRSH_TKN` 0. 팀원에게 `./gradlew seedFiles` 한 번 실행하라고 알린다
  - KSJ에게 확인: 실종 제보·펫돌봄 모집 글의 `PST.BBS_CD` 규칙이 없어 시드는 펫일상으로 넣었다. 펫이야기 목록에서 빼는 방식이 정해지면 `CommunitySeeder`를 맞춘다
  - 로컬 테스트 DB `nyangmeong_seedtest`는 확인용으로 남겨 둠. 필요 없으면 `DROP DATABASE nyangmeong_seedtest`

### 추가 요청

| 번호 | 바꿀 파일 | 완료 조건 | 요청 |
| --- | --- | --- | --- |
| 추가-1 | `build.gradle`(태스크 `seedClean`), `src/seed/.../SeedDataApplication.java`·`SeedOptions.java`, `README.md`, `docs/convention/shared-files.md` | 명령: `./gradlew build -q` 통과. 테스트 DB에서 `seedClean -Ptarget=local` 후 시드 대상·기록 테이블 0행, `COM_CD` 그대로, `uploads/seed` 삭제, 이어서 `seedData`가 다시 들어감. 공용은 DB 이름이 틀리면 아무것도 지우지 않음 | 요청 2026-10-23 15:30 "시드 데이터 한 번에 제거하는 스크립트(수정 필요할 수도 있어서)". 방식은 시드 대상 테이블 전체 비우기로 선택 |
| 추가-2 | `src/seed/resources/images/**`(사진·출처 목록), `src/seed/.../core/SeedImages.java`, 사진을 고르는 Seeder, `README.md` | 명령: `./gradlew build -q` 통과. 테스트 DB에서 `seedData` 후 반려동물 원본 사진이 그 종(개·고양이)에 맞는 실제 사진, 게시글·채팅 등은 실제 사진, `seedFiles`가 같은 파일을 만든다(크기 일치). 사진마다 출처·라이선스 기록, 저장소 증가 5MB 이하. 화면: `/pets/{petSn}/character-image`·`/receipts/{sn}/image` 200 | 요청 2026-10-23 16:20 "지금 이미지들 말고 실제처럼 이미지 교체 가능?" 출처는 무료 사진 사이트로 선택 |
| 추가-3 | `README.md` | 명령: `./gradlew build -q` 통과. 화면: README에 브라우저 dev-token 로그인과 프론트 연동(8080·5173, 5174면 CORS 403, `application-local.properties`면 공용 DB가 안 보임) 절이 있고, 기존 내용은 빠짐없이 짧아짐. "사람 확인"·"시드 데이터" 제목 유지(AGENTS.md·verify 스킬·코드가 참조) | 요청 2026-10-23 16:50 "브라우저 로그인과 포트·CORS 주의 작성하고 리드미에 길게 작성된 문장들 요약" |
| 추가-4 | `src/seed/.../core/Seed.java`·`SeedSettings.java`, `src/main/resources/application.properties.example`, `README.md`, 로컬 `application.properties`(git 제외) | 명령: `./gradlew build -q` 통과. 저장소 파일(커밋 대상)에 시드 비밀번호 값이 없다(`git grep`). `app.seed.password`가 없으면 `seedData`가 키 이름을 알려 주며 멈추고, 있으면 그 값의 bcrypt로 들어간다 | 요청 2026-10-23 17:05 "리드미에 비밀번호 올라가있는데 환경 변수 파일에 작성해두는 건 어때?" |

- 추가-4 결과: 시드 비밀번호를 `Seed.java` 상수·README에서 빼고 `application.properties`의 `app.seed.password`로 옮겼다(예시 파일에는 빈 키). 키가 없으면 `-Preset`으로 비우기 전에 키 이름을 알리고 멈춘다(테스트 DB 200행 그대로 확인), 있으면 그 값의 bcrypt로 들어간다(`matches` true). 커밋 대상 파일에 값 없음(`git grep`), `./gradlew build -q` 통과. 이 PC의 `application.properties`에 기존 값으로 키를 더했다. 디스코드의 `application.properties`에도 키를 더해야 한다(사람)
- 추가-3 결과: README에 "로그인 (dev-token)"(Postman·브라우저, 30분 만료)과 "프론트와 같이 띄우기"(8080·5173, 5174면 CORS 403) 절을 더하고, setup·MCP·시드 데이터·작업 흐름·사람 확인의 긴 문장을 줄였다. 처음 받았을 때 흐름에 `seedFiles`, application.properties에 `application-local.properties` 주의를 넣었다. 제목 "사람 확인"·"시드 데이터" 유지, `./gradlew build -q` 통과
- 추가-2 결과: Pexels·Unsplash는 API 키가 필요해(401) Wikimedia Commons의 CC0 사진(Pixabay 출처)으로 대신했다. 강아지 22·고양이 22장, 480px 재저장 1.7MB, 출처는 `src/seed/resources/images/SOURCES.md`(표에 있는 사진만 씀). 캐릭터 원본 사진은 종에 맞게(개 452·고양이 314 전부 일치), 영수증·혈액검사 결과지는 한글 문서로 그림. `seedFiles`로 다시 만든 4,060개 크기가 DB `FILE_SZ`와 전부 같음. 캐릭터·영수증·아이템 이미지 API 200, `./gradlew build -q` 통과. 공용 DB는 예전 그림이 들어 있어 `seedClean` → `seedData`를 다시 해야 바뀐다(사람)
- 추가-1 결과: 테스트 DB에서 DB 이름이 틀리면 거절, 맞으면 80개 테이블 151,414행 삭제(`COM_CD` 329행 유지)·이 PC `uploads/seed` 삭제, 이어서 `seedData` 다시 들어감. Gradle `seedClean -Ptarget=shared -Pconfirm=wrong_db` 거절 확인. 같이 고친 것: `-Preset`이 이미지 폴더도 비운다(이전 실행 이미지가 쌓였음). `./gradlew build -q` 통과

이전: 하네스 테스트(415와 사람이 읽는 변환 오류 문구) — 끝 (2026-10-22). `ErrorCode.UNSUPPORTED_MEDIA_TYPE`과 `HttpMediaTypeNotSupportedException` 핸들러, 타입 변환 실패의 `data[].message`를 "형식이 올바르지 않습니다."로. 검증 1회차 통과, 사람 확인. 새 스크립트 start→impl→verify→end를 처음 끝까지 돌림. 하네스 스크립트화 변경(`scripts/`·스킬)은 아직 커밋 전.

## 다음에 할 것

- 하네스 테스트 후속(2026-10-22, 백엔드 PR #23에 포함): verify 스킬 6번은 통과 시 `clear` 대신 `pause`, `pr-prep.sh`는 커밋하지 않은 변경이 있으면 종료 코드 4로 묻고 `--allow-dirty`로 진행. 프론트 저장소에도 `verify-loop.sh`의 `scripts` 보호와 verify·end 스킬 같은 수정을 했다(커밋 전, 프론트 PR 필요). 아직 못 돌려 본 것: `milestone-status`·`dms-fetch`·`api-row`(화면 작업), 검증 실패 뒤 잠금·3회 실패 넘기기, `pr-prep --allow-dirty`
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
