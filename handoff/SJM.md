# SJM 핸드오프

## 지금 하는 것
SCR-MYP-001 마이페이지 홈 (반려동물 목록) — 구현 중 (승인 2026-10-26 02:06, 시작 2026-10-26 10:51)
- 기준: 마이페이지_기능명세서_v0.4_20261025, 마이페이지_화면설계서_v0.6_20261025 (DMS)
- 결정: `GET /api/pets`(경로 앞부분은 기존 `pet` 컨트롤러와 같은 `/api`). 정렬은 내 양육관계 등록순(`BRUP_REL.REG_DT`, `BRUP_REL_SN`)으로 두고 docs/api/issues.md 마이페이지 1번 결정 전까지 이 순서를 쓴다. 주양육 동물은 PET 등록 시각과 같아 대표 승계 기준과 어긋나지 않는다
- 초안 수정(구현 전): docs/api/pet.md `PetSummary`에 `bscCharSbstYn`(boolean, PIXEL_CHAR.BSC_CHAR_SBST_YN, 캐릭터가 없으면 false) 추가. 이유: FN-MYP-001-01 "오류 확인 중" 배지를 목록에서 그려야 하는데 초안에 필드가 없다. 같은 `pet` 도메인 테이블이라 층 규칙 문제가 없다
- FN-MYP-001-01 내 반려동물 카드 조회: 바꿀 파일 `pet/controller/PetController`, `pet/service/PetService`(`searchMyPets(mbrSn)`), `pet/mapper/PetMapper` + `mapper/pet/PetMapper.xml`(BRUP_REL `MBR_SN = 나`·`USE_YN = 'Y'` JOIN PET `DEL_YN = 'N'`, LEFT JOIN VRTY, LEFT JOIN PIXEL_CHAR `DEL_YN = 'N'`, 공동양육 인원 = 같은 반려동물의 `USE_YN = 'Y'`·공동양육자(PETC0019) BRUP_REL 수), `pet/dto/PetSummary`(아래 DTO + `bscCharSbstYn`). 부를 API: `GET /pets` (SJM). 캐릭터 얼굴 이미지는 `GET /pets/{petSn}/character-image?part=face`(LJS, 이미 있음). 마릿수·6마리 제한·나이는 프론트가 계산. 완료 조건 — 명령: `./gradlew build -q`(`PetControllerTest` 쿠키 없음 401·로그인 시 목록 필드, `PetServiceTest` 역할·대표·인원 매핑, `MapperXmlRuleTest` DEL_YN 검사 통과). 화면: 로그인 후 `GET /api/pets`가 내 주양육·공동양육 동물만 주고, `DEL_YN = 'Y'` 반려동물과 `USE_YN = 'N'` 관계는 빠진다
- FN-MYP-002-02 반려동물 슬라이드(화면은 SCR-MYP-002): 같은 `GET /pets` 응답의 종·성별·체중·털색 코드·특징·알레르기·기저질환을 쓴다. 바꿀 파일 없음(위와 같음). 털색·성별 이름은 프론트가 공통코드(`GET /common-codes`, LJS)로 바꾼다. 완료 조건 — 명령: `PetControllerTest`가 `algyCn`·`dssCn`·`colrDrctInptNm` 값을 그대로 돌려주는지 확인. 화면: 품종 직접 입력 동물은 `vrtySn`·`vrtyNm`이 null이고 `vrtyDrctInptNm`이 채워진다
- FN-MYP-002-03 인식칩 번호 표시(화면은 SCR-MYP-002): 같은 응답의 `chipNo`. 미등록이면 null. 완료 조건 — 명령: `PetServiceTest`에서 칩 없는 동물의 `chipNo`가 null. 화면: 응답에 `chipNo` 키가 있다
- PetSummary(초안 그대로 + 1개): petSn Long Y, petNm String Y, petSeCd String Y, vrtySn Long N, vrtyNm String N, vrtyDrctInptNm String N, petGndrCd String Y, petBrdt LocalDate Y, petBdwg BigDecimal Y, colrCd String Y, colrDrctInptNm String N, chipNo String N, chrcCn String N, algyCn String N, dssCn String N, brupRoleCd String Y(BRUP_REL.BRUP_ROLE_CD), rprsPetYn boolean Y(BRUP_REL.RPRS_PET_YN), caregiverCount Integer Y(BRUP_REL 건수), bscCharSbstYn boolean Y(추가)
- 하지 않는 것: LJS의 `CaregiverPetFake`·`CaregiverFake` 교체는 LJS의 "연결 남음"이라 손대지 않는다(service 메서드만 준비)
- 새 의존성: 없음

## 다음에 할 것
