# SJM 핸드오프

## 지금 하는 것
SCR-MYP-003 반려동물 등록/수정 — 기획 (시작 2026-10-26 14:48)
- 기준: 마이페이지_기능명세서_v0.4_20261025, 마이페이지_화면설계서_v0.6_20261025 (DMS)
- 결정1 경로: 기존 컨트롤러와 같이 `/api` 앞에 붙임. `GET /api/pets/{petSn}`, `POST /api/pets`, `PATCH /api/pets/{petSn}`, `POST /api/pets/{petSn}/representative`
- 결정2 체중 잠금(docs/api/issues.md 마이페이지 2번): DB 컬럼을 추가하지 않는다. 체중 기록과 기록일시는 `DAILY_HLTH_RCD`(지표 `HLTC0014`, `RCD_DT`)에 이미 있고, 프론트가 `GET /pets/{petSn}/daily-health-records?dailyRcdIdctCd=HLTC0014&size=1`(JAJ)로 받아 체중 칸 잠금과 최근 기록값·기록일 표시를 한다. 서버는 `PATCH`의 `petBdwg`를 막지 않는다(화면에서만 잠금). 같은 정보를 PET에 두 벌 두지 않아 동기화가 필요 없다. 구현 전에 docs/api/pet.md `PetResponse`에서 `bdwgEditableYn`·`latestBdwgRcdDt`를 빼고, issues.md 2번에 이 결정을 적는다
- 결정3 수정 권한: 상세 조회와 `PATCH`는 양육자 전원(`PetAuthorizer.checkCaregiver`, 양육자가 아니면 `PET_ACCESS_DENIED`). 명세 FN-MYP-002-02의 ⋮ 메뉴가 공동양육자에게도 [반려동물 정보 수정]을 준다. 주양육자 전용은 픽셀 캐릭터 꾸미기·구매(LJS)와 삭제(SCR-MYP-002)다. 대표 지정은 FN-MYP-010-05 "공동양육 동물은 대표로 지정할 수 없음"에 따라 주양육 동물만(`checkPrimary`, `PET_PRIMARY_ONLY`). 상세의 `caregivers`는 주양육자에게 전원, 공동양육자에게 본인 1건(`CaregiverSummary` 설명대로)
- 결정4 첫 등록 대표(FN-MYP-010-05 명세대로): 등록 시 내 주양육 동물(BRUP_REL `MBR_SN = 나`·`BRUP_ROLE_CD = PETC0018`·`USE_YN = 'Y'` JOIN PET `DEL_YN = 'N'`)이 0마리일 때만 새 동물을 대표로 한다. 공동양육 동물만 있는 회원의 첫 주양육 등록도 대표가 된다. 주양육 동물이 있는데 대표가 없는 경우는 대표 승계(SCR-MYP-002)가 맡고, 등록은 대표를 바꾸지 않는다
- 결정5 털색 검증: `colrCd`가 PETG03 하위 코드인지만 서버가 `CommonCodeService.search("PETG03")`로 확인(성별 PETG02·구분 PETG01도 같은 방식). 종별 선택지 제한과 표시 순서는 COM_CD에 종 컬럼이 없어 프론트가 한다. `PETC0017`(직접 입력)이면 `colrDrctInptNm` 필수, 아니면 null로 저장
- 결정6 품종 검증: `vrtySn`과 `vrtyDrctInptNm` 중 정확히 하나. `vrtySn`은 `PetMapper`에서 VRTY `USE_YN='Y'`·`PET_SE_CD = 요청 구분`으로 확인, 없으면 `BREED_NOT_FOUND`. 수정에서 구분만 바꾸면 저장된 품종과 다시 맞춰 본다
- 결정7 PATCH: 보낸 필드만 바뀐다. 문자열 `""`는 null로 비운다. `goalBdwg`를 비우는 방법은 이번에 만들지 않는다(초안이 미정으로 둠). 체중 범위는 저장 후 값 기준 `min < max`
- 결정8 알레르기·기저질환: 쉼표로 나눠 앞뒤 공백 제거·빈 값 버리고 `,`로 다시 붙여 저장, 남는 게 없으면 null
- 결정9 털색 공통코드: 기준 파일 `db/20261012_insert_com_cd_v0.9.sql`은 고치지 않고 `db/migration/20261026_01_update_petg03_colors.sql`을 추가한다. `PETC0010` 검정&황갈색 → 블랙탄(코드값 유지), 새 코드 `PETC0028` 황색·`PETC0029` 파티·`PETC0030` 세이블·`PETC0031` 멀(강아지 전용), `PETC0032` 젖소·`PETC0033` 카오스·`PETC0034` 포인트(고양이 전용), SORT_SEQ 130~190, 직접 입력 `PETC0017` SORT_SEQ 130 → 200. 얼룩(`PETC0011`)은 파티와 다른 색이라 그대로 둔다. 부모는 `COM_CD = 'PETG03'`로 찾는다(번호를 직접 쓰지 않음). 맨 위 주석에 근거(FN-MYP-003-03)와 되돌리기 SQL을 적는다. PR 본문에 "DB 변경 있음"과 파일 이름, 병합 직후 공용 DB에 실행
- FN-MYP-003-01 등록/수정: 바꿀 파일 `pet/controller/PetController`, `pet/service/PetService`(`searchPet`·`createPet`·`updatePet`), `pet/mapper/PetMapper` + `mapper/pet/PetMapper.xml`(PET insert·동적 update, BRUP_REL 주양육자 insert, 상세 select `DEL_YN = 'N'`, 양육자 목록 JOIN MBR), `pet/domain/Pet`(VO), `pet/dto/PetCreateRequest`·`PetUpdateRequest`·`PetResponse`·`CaregiverSummary`(아래 DTO 그대로). 부를 API: `GET /pets/{petSn}`·`POST /pets`·`PATCH /pets/{petSn}` (SJM). 완료 조건 — 명령: `./gradlew build -q`(`PetControllerTest` 이름 21자·체중 0·소수 넷째 자리 400, `PetServiceTest` 등록 시 PET+BRUP_REL(PCG) 저장·공동양육자 수정 반영·양육자 아닌 회원 수정 `PET_ACCESS_DENIED`). 화면: 등록한 동물이 `GET /api/pets`에 주양육자로 나오고, `GET /api/pets/{petSn}`가 모달을 채울 값을 준다
- FN-MYP-003-02 품종 선택: 결정6. 목록 `GET /breeds`는 KSJ(아직 없음 — 서버 쪽은 VRTY 존재 검증만이라 가짜를 두지 않는다). 완료 조건 — 명령: `PetServiceTest` 둘 다 보냄·둘 다 없음 400, 다른 구분 품종 `BREED_NOT_FOUND`. 화면: 품종 마스터가 없어도 직접 입력으로 등록된다
- FN-MYP-003-03 털색 선택: 결정5·9. 목록 `GET /common-codes?prntComCd=PETG03`(LJS, 이미 있음). 완료 조건 — 명령: `PetServiceTest` 없는 털색 코드 400, 직접 입력인데 이름 없음 400. 화면: `PETC0017`+`colrDrctInptNm`로 등록·조회된다. 공용 DB에 마이그레이션을 실행한 뒤 `GET /api/common-codes?prntComCd=PETG03`에 블랙탄과 새 코드 7개가 나오고 직접 입력이 맨 끝이다
- FN-MYP-003-04 목표체중·정상범위: `minBdwg < maxBdwg`(수정은 저장 후 값 기준), 0보다 큼·소수 셋째 자리(`@Positive`·`@Digits(integer=5, fraction=3)`, DECIMAL(8,3)). 완료 조건 — 명령: `PetServiceTest` min ≥ max 400, 수정에서 min만 보내 기존 max 이상이면 400. 화면: `goalBdwg` 없이 등록된다
- FN-MYP-003-05 인식칩: `@Pattern("^[0-9]{1,20}$")`, 빈 문자열은 null. 완료 조건 — 명령: `PetControllerTest` 문자 섞인 칩 번호 400. 화면: 칩 없이 등록하면 `chipNo` null
- FN-MYP-003-06 중성화 수정: `PATCH`의 `nutrYn` → `NUTR_YN` Y/N. 완료 조건 — 명령: `PetServiceTest` `nutrYn=false` 수정 반영. 화면: 수정 후 상세의 `nutrYn`이 바뀐다
- FN-MYP-003-07 알레르기·기저질환: 결정8. 완료 조건 — 명령: `PetServiceTest` `" 새우, ,닭고기 "` → `"새우,닭고기"`, `" , "` → null. 화면: 저장 후 상세에 정리된 값
- FN-MYP-002-01 등록 상한 10마리: 등록 전 내 주양육(PETC0018)·`USE_YN='Y'`·PET `DEL_YN='N'` 건수가 10이면 `PET_LIMIT_EXCEEDED`. 공동양육은 세지 않는다. 완료 조건 — 명령: `PetServiceTest` 주양육 10마리면 막힘, 주양육 9+공동양육 3이면 등록됨. 화면: 11번째 `POST /api/pets`가 400
- FN-MYP-002-04 수정·프로필 조회: `GET /pets/{petSn}`(결정3)·`PATCH`. 체중은 결정2대로 서버가 막지 않는다. 완료 조건 — 명령: `PetServiceTest` `petBdwg` 수정 반영, `PetControllerTest` 상세 응답에 `bdwgEditableYn`·`latestBdwgRcdDt`가 없다. 화면: 상세가 모달 값을 채우고 `PATCH`로 체중이 바뀐다
- FN-MYP-010-05 대표 지정: `POST /pets/{petSn}/representative`(SJM), 응답 본문 없음. 주양육 동물만(공동양육 동물은 `PET_PRIMARY_ONLY`, 결정3). 한 트랜잭션에서 내 기존 대표를 `N`으로 바꾼 뒤 대상 `Y`(UK `RPRS_MBR_SN` 때문에 순서 지킴). 완료 조건 — 명령: `PetServiceTest` 기존 대표 해제 후 지정 순서, 주양육 0마리에서 등록하면 대표·1마리 이상이면 대표 아님(결정4), 공동양육 동물 거부; `PetControllerTest` 200. 화면: 지정 뒤 `GET /api/pets`의 `rprsPetYn`이 한 마리만 true
- PetCreateRequest·PetUpdateRequest·PetResponse·CaregiverSummary: docs/api/pet.md 초안 그대로. 단 `PetResponse`에서 `bdwgEditableYn`·`latestBdwgRcdDt`를 빼고, API 표 `PATCH /pets/{petSn}` 설명의 "(체중은 건강 일지 기록이 없을 때만)"을 "(양육자 전원, 체중 잠금은 화면에서)"로 고친다(결정2·3, 구현 전에 초안 수정)
- 하지 않는 것: 반려동물 삭제와 대표 승계(SCR-MYP-002), `GET /breeds`(KSJ), 털색 드롭다운 설명(프론트 작업 때)
- 새 의존성: 없음

## 다음에 할 것
- LJS의 `CaregiverPetFake`·`CaregiverFake`를 `PetService.searchMyPets`로 바꾸는 연결은 LJS의 "연결 남음"
