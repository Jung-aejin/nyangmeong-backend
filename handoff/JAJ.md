# JAJ 핸드오프

## 지금 하는 것

진행 중: SCR-HLT-003 케어 캘린더 (일정) — 검증 통과(3회차), 사람 확인 남음 (승인 2026-10-26 12:46, 시작 2026-10-26 12:44)

기준: 건강기록_기능명세서_v1.1_20261023, 건강기록_화면설계서_v1.1_20261026. API는 `docs/api/health.md` 초안 그대로(경로 앞 `/api`).

새로 만들 파일(`com.nyangmeong.health`):
- `controller/CareScheduleController` — `@RequestMapping("/api/pets/{petSn}/care-schedules")`
- `service/CareScheduleService` — `@Transactional`, `PetAuthorizer.checkCaregiver`로 권한, 종류별 필수값 검증
- `dto/` CareScheduleCreateRequest·CareScheduleUpdateRequest·CareScheduleResponse·CareScheduleSummary·CareScheduleSearchCond
- `domain/Schedule`(SCHDL VO), `domain/ScheduleKind`(HLTG19: HLTC0057 병원·0058 미용·목욕·0059 케어·투약·0060 기타), `domain/ScheduleSource`(HLTG22: HLTC0065 직접·0066 검진기록·0067 AI 생성)
- `mapper/CareScheduleMapper` + `resources/mapper/health/CareScheduleMapper.xml` — 모든 조회에 `DEL_YN = 'N'`, 삭제는 `DEL_YN = 'Y'` UPDATE
- `ErrorCode`에 일정 없음(`SCHEDULE_NOT_FOUND`)만 추가(공유 파일, `docs/convention/shared-files.md` 따름)
- 테스트: `CareScheduleServiceTest`, `CareScheduleControllerTest`(`@WithMockLoginMember`)

기능 ID별:
- FN-HLT-003-29 월간 캘린더: `GET /api/pets/{petSn}/care-schedules?fromYmd=&toYmd=` → `List<CareScheduleSummary>`. 통합 캘린더는 화면이 고른 반려동물 수만큼 부르고 합친다(초안 그대로, issues 공통레이아웃·메인 8번은 열어 둠). 명령: `./gradlew build -q`(서비스 테스트: 기간 밖·다른 반려동물·삭제 일정 제외). 화면: 이번 달 일정이 날짜 칸에 나온다
- FN-HLT-003-30 일정 종류 태그: Summary의 `schdlKndCdNm`·`ckupTypeCdNm`(COM_CD 조인). 명령: 매퍼 결과에 코드명이 채워짐(컨트롤러 테스트 JSON). 화면: "나비 · 정기검진"
- FN-HLT-003-31 날짜 인라인 상세: 같은 GET을 하루 범위로. 정렬은 날짜 → 시간 입력 건(`HR_INPT_YN='Y'`) 먼저 시각 순 → 미입력 건 등록 순. `ckupRcdCnt` = 연결 CKUP_RCD 건수(검진기록 있음/없음 뱃지). 명령: 정렬 테스트. 화면: 같은 날 3건 순서
- FN-HLT-003-32 날짜·시간: `hrInptYn=false`면 시각을 00:00으로 저장. 명령: 저장값 테스트
- FN-HLT-003-33 하단 버튼: `DELETE /api/pets/{petSn}/care-schedules/{schdlSn}` → DEL_YN='Y', 연결 검진기록은 그대로. 없는·삭제된·다른 반려동물 일정이면 `SCHEDULE_NOT_FOUND`. 명령: 삭제 뒤 목록에서 빠짐
- FN-HLT-003-34 일정 종류 / 003-35 종류별 추가 필드: `POST`·`PATCH` 요청을 서버에서 재검증 — 병원이면 `ckupTypeCd`(HLTG01)·`hsptlNm`(≤80) 필수, 그 외는 `schdlNm`(≤40) 필수, 종류에 맞지 않는 필드는 null로 저장(이전 종류 값 유지 안 함). 실패는 `INVALID_INPUT_VALUE` 400. 명령: 종류별 400/201 테스트
- FN-HLT-003-36 일정 메모·뱃지 색상: `memoCn` ≤500, `cldrBadgeColrCd`는 HLTG21 하위 코드만 허용, 미지정 null(기본 색상은 화면이 종류·검진유형으로 정함). HLTG21 하위 코드가 아직 없어 지금은 null만 저장된다 — 코드 확정은 "다음에 할 것". 명령: 없는 색상 코드 400
- FN-HLT-003-37 반려동물 선택: `PATCH`의 `petSn`이 바뀌면 새 반려동물도 `checkCaregiver`, SCHDL.PET_SN 변경, 이 일정에 연결된 검진기록의 PRSC·REPT_PRVNT_MNG에서 생긴 CARE_ARTCL은 PET_SN 변경 + MAIN_EXPSR_YN='N', REPT_PRVNT_MNG.PET_SN 변경(검진기록·완료 기록은 PET_SN이 없어 따라온다). 확인 창은 화면 몫. 명령: 이동 테스트(서비스: 매퍼 호출 검증)
- FN-HLT-003-39 표시 반려동물 필터: 서버 동작 없음(화면이 반려동물별 GET 결과를 거른다). 반려동물 목록은 SJM `pet` 목록 API — 아직 없으면 화면에서 가짜 목록으로 먼저. 화면: 칩 해제 시 그 반려동물 일정이 빠짐
- FN-HLT-003-40 다가오는 케어 일정: 같은 GET에 `fromYmd=오늘`, `size=3`(초안 쿼리에 없는 `size`를 추가 — 초안 표 설명에 `&size=` 한 단어를 같은 PR에서 보탬). 명령: 오늘 포함 이후 3건, 지난 일정 제외
- FN-AIM-012-13·14 AI 대화 일정 저장(화면은 SCR-AIM-012, LJS): 같은 `POST`에 `schdlSrcCd=HLTC0067`. 미지정이면 HLTC0065(직접). HLTG22 밖 값은 400. 명령: 출처 저장 테스트

완료 조건(마일스톤): 병원 일정에 검진유형·병원명이 없으면 400, 월 범위 조회가 삭제 일정을 뺀다. `./gradlew build -q` 0(ArchitectureTest·MapperXmlRuleTest 포함).

새 의존성: 없음

### 추가 요청

| 추가-1 | 프론트 `nyangmeong-frontend` `src/features/health/api.js`(일정 조회·등록·수정·삭제를 `/api/pets/{petSn}/care-schedules`로, 필드를 schdlKndCd·schdlKndCdNm·ckupTypeCdNm·schdlDt·hrInptYn·memoCn·ckupRcdCnt에 맞춤, 반려동물 목록은 가짜 유지)와 그 필드를 쓰는 일정 화면 컴포넌트 | 명령: 프론트 저장소 빌드·린트 0. 화면: `http://localhost:5173/health/calendar`에서 API로 등록한 일정이 월간 캘린더·날짜 상세에 코드명·시간 순으로 나온다 | 요청 2026-10-26 12:56 "1번으로 프론트 연결 추가 요청" |

## 다음에 할 것

- 사람 확인 남음(SCR-HLT-003 일정): Postman으로 일정 등록·수정·삭제, 병원 일정 검진유형·병원명 누락 400 확인 뒤 `/end`로 `끝` 판정
- 프론트 연결: 추가-1(프론트 일정 API 연결)은 프론트 저장소 마일스톤 "SCR-HLT-003 케어 캘린더 · 일정 API 연결"로 넘김. 프론트 파일은 이 저장소 커밋에 넣지 않았다
- 연결 남음 예정: FN-HLT-003-39 반려동물 목록 (SJM), FN-AIM-012-13·14 AI 대화 화면 (LJS)
- 참고: 기획의 `SCHEDULE_NOT_FOUND`는 기존 `CARE_SCHEDULE_NOT_FOUND`를 그대로 썼다(ErrorCode 추가 없음)
- HLTG21(캘린더 뱃지 색상) 하위 코드가 `db/` 공통코드에 없다. 코드 값을 정해 `db/migration`에 넣는 것을 팀과 맞춘다(그 전엔 색상 저장 불가)
- 통합 캘린더 호출 방식(issues 공통레이아웃·메인 8번): 지금은 반려동물별 호출. 다건 조회 API가 필요하면 다시 정한다
- LJS `CareScheduleAlarmService`(AIM-006)·`CheckupRecordFake`(AIM-009)를 health service로 연결하는 것은 LJS와 맞춘다
- `application.properties`에 빠진 키: `app.front-base-url`, `app.seed.password` (디스코드의 최신 파일로 받는다)
