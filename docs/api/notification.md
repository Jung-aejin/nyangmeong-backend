# 알림(notification) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /members/me/notification-settings | 알림 수신 설정 조회(채널·유형·사전 알림 시점·한도 설정 여부) | COM-008-02·03·05~10 | LJS | - | NotificationSettingResponse |
| GET | /notifications | 내 인앱 알림 목록(최신순, 유형 필터, 7일 경과 숨김 제외, 조회만으로 읽음 처리 안 함) `?notiTypeCd=&size=5` | COM-004-03~05·11·12 | LJS | - | Page<NotificationSummary> |
| GET | /notifications/unread-count | 미읽음 알림 수(헤더 배지·드롭다운 헤더) | COM-002-09, COM-004-02 | LJS | - | NotificationUnreadCountResponse |
| POST | /notifications/{inappNotiSn}/read | 알림 개별 읽음 처리 | COM-004-06 | LJS | - | - |
| PUT | /members/me/notification-settings | 알림 수신 설정 일괄 저장 | COM-008-02·05~09·14·20 | LJS | NotificationSettingSaveRequest | NotificationSettingResponse |

## DTO

### NotificationSettingResponse
쓰는 API: GET /members/me/notification-settings; PUT /members/me/notification-settings
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| acntEml | String | Y | 복호화한 가입 이메일, 변경 불가 표시 | MBR.ACNT_EML_ENC, COM-008-03 |
| emlNotiRcptnYn | boolean | Y | | MBR.EML_NOTI_RCPTN_YN, COM-008-02 |
| inappNotiRcptnYn | boolean | Y | | MBR.INAPP_NOTI_RCPTN_YN, COM-008-02 |
| careSchdlNotiRcptnYn | boolean | Y | | MBR.CARE_SCHDL_NOTI_RCPTN_YN, COM-008-05 |
| thdyBfhdNotiYn | boolean | Y | | MBR.THDY_BFHD_NOTI_YN, COM-008-06 |
| d1BfhdNotiYn | boolean | Y | | MBR.D1_BFHD_NOTI_YN, COM-008-06 |
| d3BfhdNotiYn | boolean | Y | | MBR.D3_BFHD_NOTI_YN, COM-008-06 |
| d7BfhdNotiYn | boolean | Y | | MBR.D7_BFHD_NOTI_YN, COM-008-06 |
| mtchDlngNotiRcptnYn | boolean | Y | | MBR.MTCH_DLNG_NOTI_RCPTN_YN, COM-008-07 |
| cmntNotiRcptnYn | boolean | Y | | MBR.CMNT_NOTI_RCPTN_YN, COM-008-08 |
| lostExpryNotiRcptnYn | boolean | Y | | MBR.LOST_EXPRY_NOTI_RCPTN_YN, COM-008-08 |
| expndLimNotiRcptnYn | boolean | Y | | MBR.EXPND_LIM_NOTI_RCPTN_YN, COM-008-09 |
| expndLimSetYn | boolean | Y | 월 지출 한도 설정 여부(계산) | MBR.MM_EXPND_LIM_AMT 존재 여부, COM-008-10 |

### NotificationSettingSaveRequest
쓰는 API: PUT /members/me/notification-settings
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| emlNotiRcptnYn | Boolean | Y | | MBR.EML_NOTI_RCPTN_YN, COM-008-02 |
| inappNotiRcptnYn | Boolean | Y | | MBR.INAPP_NOTI_RCPTN_YN, COM-008-02 |
| careSchdlNotiRcptnYn | Boolean | Y | | MBR.CARE_SCHDL_NOTI_RCPTN_YN, COM-008-05 |
| thdyBfhdNotiYn | Boolean | Y | 사전 알림 시점 복수 선택 | MBR.THDY_BFHD_NOTI_YN, COM-008-06 |
| d1BfhdNotiYn | Boolean | Y | 사전 알림 시점 복수 선택 | MBR.D1_BFHD_NOTI_YN, COM-008-06 |
| d3BfhdNotiYn | Boolean | Y | 사전 알림 시점 복수 선택 | MBR.D3_BFHD_NOTI_YN, COM-008-06 |
| d7BfhdNotiYn | Boolean | Y | 사전 알림 시점 복수 선택 | MBR.D7_BFHD_NOTI_YN, COM-008-06 |
| mtchDlngNotiRcptnYn | Boolean | Y | | MBR.MTCH_DLNG_NOTI_RCPTN_YN, COM-008-07 |
| cmntNotiRcptnYn | Boolean | Y | | MBR.CMNT_NOTI_RCPTN_YN, COM-008-08 |
| lostExpryNotiRcptnYn | Boolean | Y | | MBR.LOST_EXPRY_NOTI_RCPTN_YN, COM-008-08 |
| expndLimNotiRcptnYn | Boolean | Y | | MBR.EXPND_LIM_NOTI_RCPTN_YN, COM-008-09 |

### NotificationSummary
쓰는 API: GET /notifications (목록 행. 최신순, 7일 경과 숨김 제외)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| inappNotiSn | Long | Y | | INAPP_NOTI.INAPP_NOTI_SN |
| notiTypeCd | String | Y | AIMG02 공통코드 | INAPP_NOTI.NOTI_TYPE_CD, COM-004-04 |
| notiTypeCdNm | String | Y | 행의 [유형] 라벨 | COM_CD.COM_CD_NM, COM-004-04 (추가) |
| notiTtl | String | Y | | INAPP_NOTI.NOTI_TTL |
| notiCn | String | Y | | INAPP_NOTI.NOTI_CN |
| mvmnPath | String | N | 유형별 이동 대상 | INAPP_NOTI.MVMN_PATH, COM-004-07 |
| prslDt | LocalDateTime | N | null이면 미읽음 | INAPP_NOTI.PRSL_DT, COM-004-06·11 |

### NotificationUnreadCountResponse
쓰는 API: GET /notifications/unread-count
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| unreadCount | Integer | Y | 헤더 배지(0이면 숨김, 99 초과 '99+')·드롭다운 헤더 | INAPP_NOTI.PRSL_DT IS NULL 건수, COM-002-09, COM-004-02 |
