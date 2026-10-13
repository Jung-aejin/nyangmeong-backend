# 냥멍케어 백엔드 컨벤션: API

`AGENTS.md`에서 이어지는 문서다. 장 번호는 `AGENTS.md`와 이어진다.

## 5. API 규칙

- 접두어는 `/api/v1`이다. 리소스는 복수형 kebab-case로 쓴다. 식별자는 경로에, 필터·정렬·페이지는 쿼리에 둔다.
  - 예: `/members/me`, `/pets/{petSn}/checkups`, `/places?plcClsfCd={공통코드}&keyword=병원`
- 로그인한 회원 자신의 자원은 `me`를 쓴다(`/members/me`). 경로에 회원 일련번호를 넣지 않는다.
- 반려동물에 속한 자원은 `/pets/{petSn}/...` 아래에 둔다(예: `/pets/{petSn}/checkups/{ckupRcdSn}`). `PetAuthorizer`가 `petSn`으로 권한을 검사하기 때문이다.
- 댓글이나 첨부처럼 부모 없이 존재할 수 없는 자원은 부모 아래에 한 단계까지 중첩할 수 있다(예: `/posts/{pstSn}/comments`). 그 밖의 자원은 중첩하지 않고 쿼리로 거른다.
- 쿼리 파라미터는 camelCase로 쓴다. 컬럼에 해당하는 값은 컬럼 이름을 그대로 쓰고(`plcClsfCd`), 검색어는 `keyword`로 통일한다.
- 경로 변수는 그 테이블의 일련번호 컬럼 이름을 camelCase로 쓴다. `{id}`를 쓰지 않는다. 예: `/posts/{pstSn}`, `/pets/{petSn}/checkups/{ckupRcdSn}`
- 상태를 바꾸는 동작은 동사 하위 경로로 쓴다. 예: `POST /care-matches/{petPrtcMtchSn}/accept`

| 동작 | 메서드 | 상태 코드 |
| --- | --- | --- |
| 조회 | GET | 200 |
| 생성 | POST | 201 |
| 수정 | PATCH | 200 |
| 목록·설정 전체 저장 | PUT | 200 |
| 삭제 | DELETE | 200 |
| 상태 전이 | POST | 200 |

- 204는 쓰지 않는다. 삭제도 `ApiResponse`를 반환한다.
- 수정은 PATCH다. 보낸 필드만 바뀌고 보내지 않은 필드는 그대로다. `null`을 보낸 필드도 보내지 않은 것과 같이 그대로 둔다. 값을 비워야 하는 필드는 빈 문자열(`""`)을 보낸다.
- 생성과 수정은 바뀐 자원을 `XxxResponse`로 돌려준다. 삭제와 상태 변경은 `data`가 `null`이다.
- 단건 조회에서 대상이 없으면 404다. 목록 조회에서 결과가 없으면 200에 빈 `content`를 돌려준다.
- PUT은 목록이나 설정 전체를 한 번에 저장하는 API에만 쓴다(예: 케어리스트 순서 저장, 알림 수신 설정 일괄 저장). 요청에 없는 항목은 지워진다. 그 밖의 수정에는 PUT을 쓰지 않는다.

오류의 상태 코드는 `ErrorCode`를 추가할 때 아래에서 고른다.

| 상황 | 상태 코드 | 예 |
| --- | --- | --- |
| 입력값이 틀렸거나 업무 규칙에 어긋남 | 400 | `PET_LIMIT_EXCEEDED` |
| 토큰이 없거나 만료됨(이 경우에만) | 401 | `UNAUTHORIZED` |
| 권한이 없음, 정지·탈퇴 회원 | 403 | `PET_PRIMARY_ONLY`, `MEMBER_INACTIVE` |
| 대상이 없음 | 404 | `NOT_FOUND` |
| 이미 있음 | 409 | `EMAIL_ALREADY_REGISTERED` |
| 로그인 5회 실패로 보안코드 확인이 필요함 | 429 | `LOGIN_CAPTCHA_REQUIRED` |

보호 파일 다운로드(파일 자체를 돌려준다)를 제외한 모든 응답은 아래 모양이다. 프론트는 이 모양에 맞춰 `data`를 꺼내고 `error.code`로 분기한다. 이 형식은 프론트와의 합의가 남아 있으므로(`docs/OPEN_ISSUES.md`), 바꿀 때는 프론트 저장소의 `docs/convention/api.md`와 같이 고친다.

```json
{"success":true,"data":{ ... }}
{"success":false,"error":{"code":"MEMBER_NOT_FOUND","message":"일치하는 회원정보를 찾을 수 없습니다.","fields":null}}
```

- `error.fields`는 입력값 검증 오류일 때만 `[{"field":"...","message":"..."}]`로 채운다.
- 상태를 바꾸는 요청(GET·HEAD·OPTIONS가 아닌 요청)은 `X-Requested-With: XMLHttpRequest` 헤더가 없으면 403이 된다. 프론트는 모든 요청에 이 헤더를 붙인다. curl이나 Swagger UI로 시험할 때 403이 나오면 이 헤더부터 확인한다.

날짜와 시간은 아래 형식으로만 주고받는다.

- 일시(`LocalDateTime`)는 `"2026-10-12T09:30:00"`, 날짜(`LocalDate`)는 `"2026-10-12"`다. Spring Boot의 기본 형식(ISO 8601)이므로 `@JsonFormat`을 붙이지 않는다.
- 시간대 표기(`Z`, `+09:00`)는 붙이지 않는다. 값은 모두 한국 시간(`Asia/Seoul`)이다.
- DTO의 일시·날짜 필드는 `String`이 아니라 `LocalDateTime`·`LocalDate`로 둔다.
- 쿼리 파라미터의 날짜도 같은 형식으로 받는다. 공통 설정(`spring.mvc.format.date: iso`, `date-time: iso`)이 변환하므로 `@DateTimeFormat`을 붙이지 않는다.

목록 API는 요청과 응답의 모양이 모두 같다.

- 요청: `?page=1&size=20&sort=...`. `page`는 1부터 시작한다. `size`는 최대 50이다.
- `size`를 생략하면 20건이다. 게시판 10건, 실종 카드 12건처럼 화면설계서가 정한 값이 있으면 서비스가 `pq.sizeOr(10)`처럼 그 값을 기본값으로 넘긴다.
- 응답의 `data`: `content`(목록), `page`, `size`, `totalElements`, `totalPages`, `hasNext`.
