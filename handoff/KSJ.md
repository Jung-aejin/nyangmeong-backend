# KSJ 핸드오프

## 지금 하는 것

SCR-CSC-003 1:1 문의 — 끝 (검증 통과 1회차, 사람 확인 완료 2026-10-26)

기준: 고객센터_기능명세서_v0.4_20261019, 고객센터_화면설계서_v0.4_20261025, docs/api/support.md, OTO_INQRY·INQRY_ATCH_PHOTO·OTO_INQRY_STTS_HSTRY(db/20261012_create_tables_v0.9.sql)

만들 파일(`support` 도메인, 3층):
- controller `InquiryController` (`/api/inquiries`)
- service `InquiryService`
- dto `InquiryCreateRequest`·`InquiryUpdateRequest`·`InquiryResponse`·`InquirySummary`·`InquirySearchCond`·`ImageItem`
- mapper `InquiryMapper` + `mapper/support/InquiryMapper.xml`
- domain `Inquiry`(OTO_INQRY)·`InquiryPhoto`(INQRY_ATCH_PHOTO)·`InquiryStatus` enum(CSCC0012 접수, CSCC0013 답변 완료)·`InquiryType` enum(CSCC0005~0011)
- 테스트 `InquiryServiceTest`·`InquiryControllerTest`

기능별:
- FN-CSC-003-01·17 목록·페이지네이션: `GET /inquiries` → `Page<InquirySummary>`, 본인 것만 `REG_DT` 내림차순 10건. 완료 조건 명령: `InquiryServiceTest` 목록이 본인 문의만 최신순으로 돌려준다
- FN-CSC-003-02 상태 필터: `GET /inquiries?inqryPrcsSttsCd=` (없으면 전체). 완료 조건 명령: 필터 테스트. 화면: 미답변 칩에서 답변 완료 문의가 빠진다
- FN-CSC-003-06 유형 선택: `inqryTypeCd`는 CSCG02 하위 코드여야 한다(아니면 400). 완료 조건 명령: 잘못된 코드 400 테스트
- FN-CSC-003-08 제목·내용: 제목 최대 50자, 내용 최대 2,000자, 공백만 입력하면 400. **초안(200자·4,000자)과 다르다.** 명세 v0.4(2026-10-19)가 50자·2,000자로 바꿨다. 구현 전에 docs/api/support.md의 `InquiryCreateRequest`·`InquiryUpdateRequest`를 고친다
- FN-CSC-003-09 이미지 첨부: **초안의 `atchFileSns`(List<Long>) 대신 multipart `images`(List<MultipartFile>)로 받는다.** 이유: 첨부를 먼저 올려 번호를 받는 공용 업로드 API가 없다. 캐릭터 생성(`PixelCharacterCreateRequest`)도 multipart로 받는다. 저장은 `FileStore.store()`가 한다(형식·크기 검증 포함). 10개를 넘으면 `INQUIRY_ATTACHMENT_LIMIT_EXCEEDED`. 구현 전에 초안을 고친다. 완료 조건 명령: 11개 첨부 400 테스트
- FN-CSC-003-10 캐릭터 오류 연동: `pixelCharSn`이 있으면 유형이 CSCC0006(픽셀 캐릭터)인지 확인한다. 문의를 저장한 뒤 같은 트랜잭션에서 `PixelCharacterService.receiveErrorInquiry(mbrSn, pixelCharSn, otoInqrySn)`(LJS 구현, pet 1층)를 부른다. `charPhotoSn`은 초안대로 받아 저장한다. 완료 조건 명령: 캐릭터 문의 작성 시 receiveErrorInquiry가 호출되고, 이미 접수된 캐릭터면 409로 문의도 저장되지 않는다
- FN-CSC-003-11 제출: `POST /inquiries` (multipart) → 201 `InquiryResponse`. 상태는 CSCC0012(접수 = 화면 「미답변」). OTO_INQRY_STTS_HSTRY에 (null → 접수) 이력을 남긴다. 완료 조건 명령: 작성 후 상태가 접수이고 이력 1건
- FN-CSC-003-12·13 상세·답변 노출: `GET /inquiries/{otoInqrySn}` → `InquiryResponse`(답변 전 `ansCn`·`ansDt` null, 첨부 `imgs`는 DEL_YN='N'만 SORT_SEQ 순서). 완료 조건 명령: 상세 테스트
- FN-CSC-003-14 답변 알림: 관리자 답변 등록 API는 이 마일스톤에 없다(관리자 화면, 담당 미정). 대신 `InquiryService.answer(otoInqrySn, prcsMbrSn, ansCn)`를 만들어 둔다. 이 메서드는 상태를 답변 완료로 바꾸고 이력을 남기며, `EventPublisher.publish(NotificationEvent(…, INQUIRY_ANSWER, …, "/support/inquiries/{otoInqrySn}"))`로 알림을 발행한다. 관리자 쪽이 이것을 부르면 된다. 완료 조건 명령: answer 호출 시 이벤트 1건 발행, 이미 답변 완료면 `ALREADY_PROCESSED`
- FN-CSC-003-15 수정: `PATCH /inquiries/{otoInqrySn}` (multipart). 접수 상태에서만 가능하고 아니면 `INQUIRY_NOT_EDITABLE`. 첨부는 남길 첨부 `keepAtchFileSns` + 새 `images`로 받는다. 빠진 기존 첨부는 `DEL_YN='Y'`로 바꾸고, 합계가 10개를 넘으면 400이다. **초안의 "atchFileSns 목록 교체"와 다르다.** 위 multipart와 같은 이유이며 초안을 같이 고친다. 완료 조건 명령: 답변 완료 문의 수정 400, 첨부 하나 삭제 + 하나 추가 후 상세에 반영. 화면: 수정 뒤 상세로 돌아가고 상태는 미답변 유지
- FN-CSC-003-16 본인 접근 제어: 상세·수정에서 남의 문의면 403 `FORBIDDEN`(기존 ErrorCode), 없는 번호면 `INQUIRY_NOT_FOUND`. 완료 조건 명령: 남의 문의 403 테스트

전체 완료 조건 명령: `./gradlew build -q` 통과(ArchitectureTest 포함: support → pet service만 사용)
화면(Postman, dev-token 1번 회원): 작성 → 목록 → 상세 → 수정 → 다른 회원(2번)으로 상세 403

새 의존성: 없음

### 추가 요청
| 번호 | 바꿀 파일 | 완료 조건 | 근거 |
| --- | --- | --- | --- |
| 추가-1 | `support/service/InquiryService.java`, `support/service/InquiryServiceTest.java`, `docs/api/support.md` | 명령: 캐릭터 오류 문의를 작성하면 `receiveErrorInquiry`가 돌려준 캐릭터 이미지가 첨부 첫 번째(sortSeq 0)로 붙고, 사용자가 올린 이미지는 그 뒤에 붙는다. 첨부 합계는 10개를 넘을 수 없다 | 요청 2026-10-26 14:29 "캐릭터 오류 문의에서 receiveErrorInquiry가 돌려준 캐릭터 이미지를 문의 첨부로 붙여줘" |

## 다음에 할 것
- 연결 남음: FN-CSC-003-14 관리자 답변 등록 API가 `InquiryService.answer()`를 부르도록 연결 (관리자 화면, 담당 미정)
- 공용 DB에 검증하며 만든 확인용 문의 OTO_INQRY_SN=201(첨부 3건, 상태 이력 1건)을 확인 쿼리로 대상 확인 뒤 지운다(자식 행부터)
- 다음 마일스톤: SCR-CMU-001 펫이야기
