# 냥멍케어 백엔드 컨벤션: 모든 코드에 공통

`AGENTS.md`에서 이어지는 문서다.

### 로그와 예외

- 로그는 `@Slf4j`의 `log`로만 남긴다. `System.out.println`과 `e.printStackTrace()`를 쓰지 않는다.
- 이메일, 전화번호, 비밀번호, 토큰을 로그에 남기지 않는다.
- 예외를 잡았으면 다시 던지거나 `BusinessException`으로 바꿔 던진다. `catch` 블록을 비워 두지 않는다.
- 예상하지 못한 예외의 로그는 `GlobalExceptionHandler`가 남긴다. 도메인 코드에서 같은 예외를 또 로그로 남기지 않는다.

### 주석

- 주석은 한글로 쓴다.
- 코드를 읽으면 아는 것은 적지 않는다. 왜 이렇게 했는지, 놓치기 쉬운 조건만 적는다.
- AI 도구가 붙인 주석은 그대로 커밋하지 않는다. 코드를 말로 옮긴 주석, 고친 과정을 적은 주석("~로 변경", "기존 로직 유지"), 구분선과 이모지는 지운다.

```java
// 정지 회원은 재발급하지 않고 바로 막는다
// NG: // 회원을 조회한다         -- 코드와 같은 말
// NG: // 수정: null 체크 추가    -- 고친 과정
```

## 6. 이름 짓기

| 대상 | 규칙 | 예 |
| --- | --- | --- |
| 클래스와 파일 | PascalCase. 명사 또는 동사+명사로 짓고, 파일 이름은 클래스 이름과 같게 한다 | `PetService`, `PetAuthorizer` |
| 패키지 | 소문자 단수 | `member`, `pet` |
| 요청 DTO | `XxxRequest` | `PetUpdateRequest` |
| 생성 요청 DTO | `XxxCreateRequest`. `Register`, `Add`를 같은 뜻으로 쓰지 않는다 | `PetCreateRequest` |
| 전체 저장 요청 DTO | `XxxSaveRequest`. PUT API에만 쓴다 | `CareOrderSaveRequest` |
| 응답 DTO | `XxxResponse` | `PetResponse` |
| 검색 조건 | `XxxSearchCond` | `PostSearchCond` |
| 목록용 응답 DTO | `XxxSummary`. DTO 접미어는 `Request`·`Response`·`Summary`·`SearchCond`만 쓴다. `Item`, `Result`, `Dto`는 쓰지 않는다 | `PostSummary` |
| 메서드 | camelCase. 동사로 시작한다 | `normalizeEmail`, `checkOwner` |
| Service 메서드 | 단건 조회는 `get`, 목록 조회는 `search`, 생성·수정·삭제는 `create`·`update`·`delete`, 전체 저장은 `save`로 시작한다. 조회에 `find`를 쓰지 않는다. 상태를 바꾸는 동작은 그 동작의 동사를 쓴다 | `getPost`, `search`, `create`, `accept` |
| Controller 메서드 | 호출하는 Service 메서드와 같은 이름을 쓴다 | `create`, `delete` |
| Mapper 메서드 | `select`·`insert`·`update`·`delete`·`upsert`·`count`·`exists`로 시작한다. `find`를 쓰지 않는다. `upsert`는 `INSERT ... ON DUPLICATE KEY UPDATE`에만 쓴다 | `selectList`, `existsPrimary` |
| 검사 메서드 | 조건이 맞지 않으면 예외를 던지는 메서드는 `check`로 시작한다. boolean을 돌려주는 메서드는 `is`·`has`·`can`·`exists`로 시작한다 | `checkOwner`, `existsPrimary` |
| 변수 | camelCase 명사. 컬럼에 없는 이름은 약어를 쓰지 않는다. `data1`처럼 번호를 붙이지 않는다 | `totalAmount` |
| 상수 | `UPPER_SNAKE_CASE` | `MAX_PET_COUNT` |
| boolean 변수·메서드 | `is`·`has`·`can`으로 시작한다 | `isOwner`, `hasNext` |
| 관리자 컨트롤러 | `AdminXxxController` | `AdminPetController` |
| Mapper XML | `mapper/{도메인}/{이름}Mapper.xml`. 인터페이스 이름과 같게 | `mapper/pet/PetMapper.xml` |

생성·수정·저장·삭제의 동사는 뜻으로 구분한다. Service 메서드, 요청 DTO, HTTP 메서드가 한 줄씩 대응한다.

| 동사 | 뜻 | HTTP | 요청 DTO |
| --- | --- | --- | --- |
| `create` | 새 데이터 1건을 만든다 | POST, 201 | `XxxCreateRequest` |
| `update` | 보낸 필드만 수정한다 | PATCH | `XxxUpdateRequest` |
| `save` | 목록·설정 전체를 한 번에 저장한다. 있으면 바꾸고 없으면 만든다 | PUT | `XxxSaveRequest` |
| `delete` | 삭제한다 | DELETE | 없음 |

**필드 이름은 새로 짓지 않는다.** DB 컬럼 이름을 camelCase로 바꾼 것을 VO 필드, DTO 필드, JSON 키, 경로 변수, Mapper의 파라미터에 그대로 쓴다. ERD가 곧 이름 규칙이다.

| DB 컬럼 | 쓰는 이름 | 쓰지 않는 이름 |
| --- | --- | --- |
| `PST_SN` | `pstSn` | `postSn`, `postId` |
| `CKUP_RCD_SN` | `ckupRcdSn` | `checkupSn`, `checkupId` |
| `MDFCN_DT` | `mdfcnDt` | `updatedAt`, `modifiedDate` |

- 읽기 좋게 고쳐 지으면 같은 컬럼을 사람마다 다르게 부르게 된다. 이름이 길거나 어색해도 그대로 쓴다.
- 컬럼에 없는 값(계산한 값, 여러 컬럼을 합친 값)만 새 이름을 짓는다.
- 이 규칙은 필드 이름에만 해당한다. URL의 리소스 이름(`/posts`, `/care-matches`)과 클래스 이름은 뜻이 드러나는 영어를 쓴다.

## 부록. 공통 모듈에서 가져다 쓰는 것

새로 만들지 말고 아래를 쓴다.

| 이름 | 패키지 | 하는 일 |
| --- | --- | --- |
| `ApiResponse`, `PageResponse` | `global/response` | 모든 응답과 목록 응답을 감싼다 |
| `ErrorCode`, `BusinessException` | `global/error` | 오류 코드 목록과, 그 코드를 담아 던지는 예외 |
| `LoginMember` | `global/security` | 로그인한 회원(회원 번호와 권한) |
| `FieldCryptoService` | `global/security/crypto` | 컬럼 암호화·복호화, 중복 확인용 해시, 이메일 정규화 |
| `PetAuthorizer`, `OwnerAuthorizer` | `global/auth` | 반려동물 권한과 작성자 권한 검사 |
| `PageQuery` | `global/mybatis` | 목록 요청의 페이지·크기·정렬 |
| `FileStore` | `global/file` | 파일 저장과 읽기 |
| `NotificationEvent`, `MailEvent` | `global/notification` | 알림과 메일을 보내 달라는 이벤트 |
| `EventPublisher` | `global/notification` | 이벤트를 발행한다. 트랜잭션이 없으면 예외를 던진다 |
| `@WithMockLoginMember` | 테스트 `global/security` | 컨트롤러 테스트에서 로그인한 회원을 흉내 낸다 |
