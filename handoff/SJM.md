# SJM 핸드오프

## 지금 하는 것
SCR-MYP-003 반려동물 등록/수정 — 기획 (시작 2026-10-26 14:48)
- 기준: 마이페이지_기능명세서_v0.4_20261025, 마이페이지_화면설계서_v0.6_20261025 (DMS)
- 결정1 경로: 기존 컨트롤러와 같이 `/api` 앞에 붙임. `GET /api/pets/{petSn}`, `POST /api/pets`, `PATCH /api/pets/{petSn}`, `POST /api/pets/{petSn}/representative`
- 결정2 체중 잠금(docs/api/issues.md 마이페이지 2번): `pet`(1층)은 `health`(2층)를 못 읽으므로 `db/migration/20261026_01_add_pet_last_bdwg_rcd_dt.sql`로 `PET.LAST_BDWG_RCD_DT datetime NULL`(최근체중기록일시)을 추가하고 기준 파일 `db/20261012_create_tables_v0.9.sql`에도 반영. `bdwgEditableYn = LAST_BDWG_RCD_DT IS NULL`, `latestBdwgRcdDt = LAST_BDWG_RCD_DT`. 값은 건강 일지(JAJ)가 `PetService.syncWeight(petSn, bdwg, rcdDt)`를 불러 채운다(남은 기록이 없으면 `bdwg`·`rcdDt` 모두 null → 체중 유지, 일시만 비움). issues.md 2번에 이 결정을 적는다
- 결정3 수정 권한: `PATCH`·대표 지정은 주양육자만(`PetAuthorizer.checkPrimary`, `PET_PRIMARY_ONLY`), 상세 조회는 양육자 전원(`checkCaregiver`). 상세의 `caregivers`는 주양육자에게 전원, 공동양육자에게 본인 1건(`CaregiverSummary` 설명대로)
- 결정4 첫 등록 대표: 등록 시 내 유효한 대표(BRUP_REL `RPRS_PET_YN='Y'`·`USE_YN='Y'`)가 없으면 새 동물을 대표로. 첫 등록이 이 경우에 들고, 대표가 모두 삭제돼 "대표 없음"인 뒤의 등록도 대표가 된다
- 결정5 털색 검증: `colrCd`가 PETG03 하위 코드인지만 서버가 `CommonCodeService.search("PETG03")`로 확인(성별 PETG02·구분 PETG01도 같은 방식). 종별 선택지 제한은 COM_CD에 종 컬럼이 없어 프론트가 한다. `PETC0017`(직접 입력)이면 `colrDrctInptNm` 필수, 아니면 null로 저장. **명세의 털색 목록(황색·블랙탄·파티·세이블·멀, 젖소·카오스·포인트)과 시드 PETG03(검정&황갈색·얼룩 등)이 다르다 — 시드 수정은 공용 파일이라 팀에 넘기고 이번엔 손대지 않는다**
- 결정6 품종 검증: `vrtySn`과 `vrtyDrctInptNm` 중 정확히 하나. `vrtySn`은 `PetMapper`에서 VRTY `USE_YN='Y'`·`PET_SE_CD = 요청 구분`으로 확인, 없으면 `BREED_NOT_FOUND`. 수정에서 구분만 바꾸면 저장된 품종과 다시 맞춰 본다
- 결정7 PATCH: 보낸 필드만 바뀐다. 문자열 `""`는 null로 비운다. `goalBdwg`를 비우는 방법은 이번에 만들지 않는다(초안이 미정으로 둠). 체중 범위는 저장 후 값 기준 `min < max`
- 결정8 알레르기·기저질환: 쉼표로 나눠 앞뒤 공백 제거·빈 값 버리고 `,`로 다시 붙여 저장, 남는 게 없으면 null
- FN-MYP-003-01 등록/수정: 바꿀 파일 `pet/controller/PetController`, `pet/service/PetService`(`searchPet`·`createPet`·`updatePet`), `pet/mapper/PetMapper` + `mapper/pet/PetMapper.xml`(PET insert·동적 update, BRUP_REL 주양육자 insert, 상세 select `DEL_YN = 'N'`, 양육자 목록 JOIN MBR), `pet/domain/Pet`(VO), `pet/dto/PetCreateRequest`·`PetUpdateRequest`·`PetResponse`·`CaregiverSummary`(아래 DTO 그대로). 부를 API: `GET /pets/{petSn}`·`POST /pets`·`PATCH /pets/{petSn}` (SJM). 완료 조건 — 명령: `./gradlew build -q`(`PetControllerTest` 이름 21자·체중 0·소수 넷째 자리 400, `PetServiceTest` 등록 시 PET+BRUP_REL(PCG) 저장·공동양육자 수정 `PET_PRIMARY_ONLY`·체중 기록 있을 때 체중 수정 `PET_WEIGHT_LOCKED`). 화면: 등록한 동물이 `GET /api/pets`에 주양육자로 나오고, `GET /api/pets/{petSn}`가 모달을 채울 값과 `bdwgEditableYn`을 준다
- FN-MYP-003-02 품종 선택: 결정6. 목록 `GET /breeds`는 KSJ(아직 없음 — 서버 쪽은 VRTY 존재 검증만이라 가짜를 두지 않는다). 완료 조건 — 명령: `PetServiceTest` 둘 다 보냄·둘 다 없음 400, 다른 구분 품종 `BREED_NOT_FOUND`. 화면: 품종 마스터가 없어도 직접 입력으로 등록된다
- FN-MYP-003-03 털색 선택: 결정5. 목록 `GET /common-codes?prntComCd=PETG03`(LJS, 이미 있음). 완료 조건 — 명령: `PetServiceTest` 없는 털색 코드 400, 직접 입력인데 이름 없음 400. 화면: `PETC0017`+`colrDrctInptNm`로 등록·조회된다
- FN-MYP-003-04 목표체중·정상범위: `minBdwg < maxBdwg`(수정은 저장 후 값 기준), 0보다 큼·소수 셋째 자리(`@Positive`·`@Digits(integer=5, fraction=3)`, DECIMAL(8,3)). 완료 조건 — 명령: `PetServiceTest` min ≥ max 400, 수정에서 min만 보내 기존 max 이상이면 400. 화면: `goalBdwg` 없이 등록된다
- FN-MYP-003-05 인식칩: `@Pattern("^[0-9]{1,20}$")`, 빈 문자열은 null. 완료 조건 — 명령: `PetControllerTest` 문자 섞인 칩 번호 400. 화면: 칩 없이 등록하면 `chipNo` null
- FN-MYP-003-06 중성화 수정: `PATCH`의 `nutrYn` → `NUTR_YN` Y/N. 완료 조건 — 명령: `PetServiceTest` `nutrYn=false` 수정 반영. 화면: 수정 후 상세의 `nutrYn`이 바뀐다
- FN-MYP-003-07 알레르기·기저질환: 결정8. 완료 조건 — 명령: `PetServiceTest` `" 새우, ,닭고기 "` → `"새우,닭고기"`, `" , "` → null. 화면: 저장 후 상세에 정리된 값
- FN-MYP-002-01 등록 상한 10마리: 등록 전 내 주양육(PETC0018)·`USE_YN='Y'`·PET `DEL_YN='N'` 건수가 10이면 `PET_LIMIT_EXCEEDED`. 공동양육은 세지 않는다. 완료 조건 — 명령: `PetServiceTest` 주양육 10마리면 막힘, 주양육 9+공동양육 3이면 등록됨. 화면: 11번째 `POST /api/pets`가 400
- FN-MYP-002-04 수정·프로필 조회: `GET /pets/{petSn}`(결정3)·`PATCH`. 체중은 결정2: `LAST_BDWG_RCD_DT`가 있으면 `petBdwg`를 보내면 `PET_WEIGHT_LOCKED`. 프로필 체중 동기화 `syncWeight`는 health가 부를 메서드로만 준비. 완료 조건 — 명령: `PetServiceTest` `syncWeight` 값 있음→체중·일시 갱신, null→체중 유지·일시 비움. 화면: 상세의 `bdwgEditableYn`·`latestBdwgRcdDt`
- FN-MYP-010-05 대표 지정: `POST /pets/{petSn}/representative`(SJM), 응답 본문 없음. 주양육자만(공동양육 동물은 `PET_PRIMARY_ONLY`). 한 트랜잭션에서 내 기존 대표를 `N`으로 바꾼 뒤 대상 `Y`(UK `RPRS_MBR_SN` 때문에 순서 지킴). 완료 조건 — 명령: `PetServiceTest` 기존 대표 해제 후 지정 순서, 공동양육 동물 거부; `PetControllerTest` 200. 화면: 지정 뒤 `GET /api/pets`의 `rprsPetYn`이 한 마리만 true
- PetCreateRequest·PetUpdateRequest·PetResponse·CaregiverSummary: docs/api/pet.md 초안 그대로(`PetResponse.latestBdwgRcdDt`의 근거만 `PET.LAST_BDWG_RCD_DT`로 고친다 — 구현 전에 초안 수정)
- 하지 않는 것: 반려동물 삭제와 대표 승계(SCR-MYP-002), 식사량·음수량 범위(issues 4번), `GET /breeds`(KSJ), PETG03 시드 수정(팀)
- 새 의존성: 없음

## 다음에 할 것
- 명세 털색 목록과 시드 PETG03이 다르다 — 팀에 알리고 시드 수정 담당 정하기
- 건강 일지(JAJ)가 체중 기록 추가·수정·삭제 때 `PetService.syncWeight`를 부르도록 연결 (JAJ)
- LJS의 `CaregiverPetFake`·`CaregiverFake`를 `PetService.searchMyPets`로 바꾸는 연결은 LJS의 "연결 남음"
