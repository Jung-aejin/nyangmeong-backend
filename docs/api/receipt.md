# 영수증(도메인 미정) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

영수증 저장 위치(`expense` 또는 AI 서버)가 정해지지 않아 따로 둔다(`docs/OPEN_ISSUES.md`).

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| POST | /receipts | 영수증 이미지 업로드(multipart, 형식·용량 검증) → 서버 내부 OCR 호출(Google Vision, LJS 호출부) → 상태·추출 결과 반환 | AIM-008-04~06·09, AIM-009-04·16·20·21, AIM-010-01·05 | YAY | ReceiptCreateRequest | ReceiptResponse |
| POST | /receipts/{recptSn}/cancel | 분석 취소(영수증 상태 '취소') | AIM-008-07 | YAY | - | - |
| POST | /receipts/{recptSn}/checkups | 영수증 결과 일괄 저장 — 반려동물별 검진기록·병원 일정 연결/생성·처방약·가계부 지출을 한 트랜잭션, 멱등 ID, 영수증 상태 저장완료 | AIM-009-07~11·24·26·27, AIM-010-06, HLT-002-31·35·37~41·45 | YAY | ReceiptCheckupCreateRequest | List<CheckupResponse> |

## DTO

### ReceiptCheckupCreateRequest
쓰는 API: POST /receipts/{recptSn}/checkups (응답은 List<CheckupResponse>)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| idmptDmndId | String | Y | 최대 100자. 중복 클릭·재전송 차단 | RECPT.IDMPT_DMND_ID (AIM-009-24) |
| hsptlNm | String | Y | 1~50자(AIM-009-26. 컬럼은 80자, issues 11) | CKUP_RCD.HSPTL_NM (AIM-009-07) |
| ckupYmd | LocalDate | Y | 모든 반려동물에 공통 | AIM-009-07 (CKUP_RCD에 검진일 컬럼 없음, issues 1) |
| pets | List<ReceiptCheckupPetRequest> | Y | 1건 이상('저장 안 함' 탭은 보내지 않음). 같은 petSn 중복 불가 | AIM-009-09 ('저장 안 함' 탭은 보내지 않음) |

### ReceiptCheckupPetRequest
쓰는 API: POST /receipts/{recptSn}/checkups (ReceiptCheckupCreateRequest.pets의 요소. 중첩 record)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | Y | 로그인 회원과 연동된 반려동물(AIM-009-17) | PET.PET_SN |
| ckupTypeCd | String | Y | HLTG01 공통코드 | CKUP_RCD.CKUP_TYPE_CD |
| ckupCn | String | N | 최대 500자 | CKUP_RCD.CKUP_CN (AIM-009-21) |
| ckupMemoCn | String | N | 최대 200자 | CKUP_RCD.CKUP_MEMO_CN |
| schdlSn | Long | N | [옮겨 연결하기]를 고른 반려동물만 | CKUP_RCD.SCHDL_SN (가까운 예정 일정 옮겨 연결, HLT-002-46) |
| expndRcdYn | Boolean | Y | 중복 확인에서 [건너뛰기]한 반려동물은 false | AIM-009-22·23 (가계부 기록 여부, 중복 건너뜀 포함) |
| expndAmt | Long | N | expndRcdYn=true면 필수, 1원 이상 | EXPND.EXPND_AMT (expndRcdYn이면 1원 이상) |
| prescriptions | List<ReceiptPrescriptionRequest> | N | 0건 허용. 주기 체크 OFF로 저장 | AIM-009-03 (0건 허용) |

### ReceiptCreateRequest
쓰는 API: POST /receipts (multipart/form-data)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| file | MultipartFile | Y | jpg·jpeg·png·webp, 1건당 10MB 이하, 확장자·실제 형식 이중 검증. PDF 불가 | RECPT.ATCH_FILE_SN (jpg·jpeg·png·webp, 10MB, AIM-008-05) |
| ocrDmndSrcCd | String | Y | AIMG07 공통코드 | RECPT.OCR_DMND_SRC_CD (검진 경로) |

### ReceiptPrescriptionItem
쓰는 API: POST /receipts (ReceiptSectionResponse.prescriptions의 요소. 중첩 record)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mdcnNm | String | N | 인식 실패 칸은 null | AIM-009-03·04 (PRSC.MDCN_NM에 저장될 값) |
| mdcnBgngYmd | LocalDate | N | 검진일 | AIM-009-04 |
| mdcnEndYmd | LocalDate | N | 검진일 + 일수 − 1 | AIM-009-04 |
| dayMdcnNmtm | Integer | N | 용법의 n회 | AIM-009-04 |
| prscSttsCd | String | N | HLTG06. 종료일이 오늘 이전이면 종료, 아니면 복용중 | AIM-009-04 |
| mdcnAmt | Long | N | 약품 금액. 대조용, 저장 안 함 | AIM-009-03·13 |

### ReceiptPrescriptionRequest
쓰는 API: POST /receipts/{recptSn}/checkups (ReceiptCheckupPetRequest.prescriptions의 요소. 중첩 record)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mdcnNm | String | Y | 1~40자(AIM-009-26) | PRSC.MDCN_NM |
| mdcnBgngYmd | LocalDate | Y | | PRSC.MDCN_BGNG_YMD |
| mdcnEndYmd | LocalDate | Y | 시작일 이후(같은 날 허용, AIM-009-07) | PRSC.MDCN_END_YMD |
| dayMdcnNmtm | Integer | Y | 최대 4자리 | PRSC.DAY_MDCN_NMTM |
| prscSttsCd | String | Y | HLTG06 공통코드 | PRSC.PRSC_STTS_CD |

### ReceiptResponse
쓰는 API: POST /receipts
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| recptSn | Long | Y | | RECPT.RECPT_SN |
| ocrPrcsSttsCd | String | Y | AIMG01 | RECPT.OCR_PRCS_STTS_CD (인식 실패·일시 오류 구분, AIM-010-01) |
| hsptlNm | String | N | | AIM-009-02 (추출 병원명) |
| ckupYmd | LocalDate | N | | AIM-009-02 (추출 검진일) |
| recptAmt | Long | N | | AIM-009-02 (영수증 최종 결제 금액) |
| sections | List<ReceiptSectionResponse> | N | | AIM-009-16 (반려동물 구역별 구역명·진료 품목·금액·약품 행) |

### ReceiptSectionItem
쓰는 API: POST /receipts (ReceiptSectionResponse.items의 요소. 중첩 record)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| itemNm | String | Y | 진료 품목명(검진 내용 자동 채움 재료) | AIM-009-21, HLT-002-38 |
| itemAmt | Long | N | 품목 금액(배정 필요 목록의 탭 금액 합산) | AIM-009-19, HLT-002-43 |

### ReceiptSectionResponse
쓰는 API: POST /receipts (ReceiptResponse.sections의 요소. 중첩 record, 모양은 issues 9)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| sectionNm | String | N | 구역의 반려동물 이름(예: 미르). 구역 없는 영수증은 null인 구역 1개 | AIM-009-16·17 |
| petSn | Long | N | 구역 이름이 회원의 반려동물 이름과 공백 제외 일치하면 자동 연결 | PET.PET_SN, AIM-009-17 |
| sbttlAmt | Long | N | 구역 소계 | HLT-002-39, AIM-009-20 |
| dscntAmt | Long | N | 구역 할인(하단 할인을 비율 배분한 몫 포함) | AIM-009-20, HLT-002-42 |
| items | List<ReceiptSectionItem> | Y | 진료 품목(약품 제외). 0건이면 빈 목록 | AIM-009-19·21 |
| prescriptions | List<ReceiptPrescriptionItem> | Y | 약품 행. 0건이면 빈 목록 | AIM-009-03·04 |
