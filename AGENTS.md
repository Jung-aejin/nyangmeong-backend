# 냥멍케어 백엔드 컨벤션

이 저장소에서 코드를 쓰는 사람과 AI 도구가 따르는 규칙이다. 규칙은 이 파일 하나에만 있다.

- Codex는 이 파일을 자동으로 읽는다. Claude Code는 `CLAUDE.md`의 `@AGENTS.md` 한 줄로 읽는다. 그 줄을 지우면 규칙이 통째로 빠진다.
- 10장(Git 규칙)은 팀 합의 전 제안이다. 아직 정하지 않은 것은 `docs/OPEN_ISSUES.md`에 있다.

## 0. 코드를 쓰기 전에 꼭 알 것

어기면 되돌리기 어렵거나 오류 없이 조용히 잘못되는 것만 모았다. 나머지는 필요할 때 찾아본다.

1. **비밀값을 커밋하지 않는다.** 이 저장소는 공개다. 비밀번호, 키, 토큰은 환경변수로만 넣는다. (8장)
2. **의존은 한 방향이다.** `domain`이 `global`을 쓰고, 다른 도메인은 `service`로만, 자기보다 낮은 층만 부른다. 순환 참조 오류가 나면 `@Lazy`나 `ObjectProvider`로 덮지 않고 이벤트로 바꾸거나 기능을 위층으로 옮긴다. (3장)
3. **알림과 메일은 `@Transactional` 메서드 안에서 `EventPublisher.publish()`로 발행한다.** 트랜잭션 밖에서 부르면 예외가 난다. `ApplicationEventPublisher`를 직접 쓰면 이 검사가 빠져서 오류 없이 사라진다. (4장 Service)
4. **테스트를 끄거나 지워서 통과시키지 않는다.** (9장)
5. **`DEL_YN`이 있는 테이블을 조회할 때는 `DEL_YN = 'N'` 조건을 넣는다.** 빠뜨리면 삭제된 데이터가 오류 없이 화면에 나온다. 삭제는 `DELETE`가 아니라 `DEL_YN = 'Y'`로 바꾸는 `UPDATE`다. (4장 Mapper와 XML)

## 1. 기술 스택

Java 21, Spring Boot 4.1, Spring Security 7, MyBatis(Mapper 인터페이스 + XML), MySQL 8, Lombok, Gradle. Docker는 쓰지 않는다.

### 편집기와 포맷

- 백엔드는 VS Code로 작업한다. 포맷 설정은 저장소의 `.vscode/settings.json`에 있고 저장할 때 자동으로 적용된다. 개인 설정으로 덮어쓰지 않는다.
- Java는 공백 4칸으로 들여쓴다. 줄바꿈은 LF다(`.gitattributes`가 커밋할 때 맞춘다).
- Mapper XML은 자동 포맷하지 않는다. 포매터가 SQL의 줄 배치를 바꾸기 때문이다.
- 다른 IDE를 쓰면 파일 전체 포맷을 하지 않는다. 포매터가 달라서 다른 사람이 쓴 줄까지 바뀐다.

## 2. 패키지 구조

```
com.nyangmeong
├─ global/                 공통 모듈. 도메인을 모른다
│  ├─ response/  error/  security/  auth/  validation/  file/
│  ├─ notification/  mail/  mybatis/  code/  setting/
│  └─ external/  batch/  config/  log/
└─ domain/                 업무 기능. 도메인 1개 = 패키지 1개
   ├─ member/  pet/  health/  ai/  community/  place/
   ├─ support/  notification/  expense/
   ├─ admin/               여러 도메인을 합치는 관리자 조회만
   └─ job/                 여러 도메인에 걸친 배치만

src/main/resources/mapper/{도메인}/{이름}Mapper.xml   (공통 모듈은 mapper/global/)
db/                        테이블 생성·변경 SQL (저장소 루트). 사람이 직접 실행한다.
                           src/main/resources/db/migration은 만들지 않는다
```

도메인 안은 다섯 패키지로 통일한다.

| 패키지 | 넣는 것 | 넣지 않는 것 |
| --- | --- | --- |
| `controller` | `@RestController`, 요청 검증, `ApiResponse` 반환 | 비즈니스 분기 |
| `service` | `@Transactional`, 권한 검사, 도메인 규칙, 이벤트 발행 | `HttpServletRequest` |
| `dto` | 요청·응답·검색 조건 | 테이블 1:1 VO(`domain`에 둔다). VO를 응답으로 그대로 반환하지 않는다 |
| `mapper` | MyBatis 인터페이스 | (4장 Mapper와 XML) |
| `domain` | 테이블 1:1 VO, 상태·종류 enum | 요청·응답 전용 필드 |

- 도메인 안에서 따로 묶을 기능은 하위 패키지로 둘 수 있다(지금은 `community/chat` 하나). 하위 패키지 안도 같은 다섯 패키지로 나눈다.
- 설정 클래스는 그 기능의 패키지에 둔다(예: `SecurityConfig`는 `global/security`). `global/config`에는 속할 기능 패키지가 없는 설정만 둔다(`OpenApiConfig`, `AsyncConfig`, `WebConfig`).

## 3. 의존 방향

1. `domain`은 `global`을 쓴다. `global`은 `domain`을 참조하지 않는다. `global`이 테이블을 읽어야 하면 도메인의 Mapper를 쓰지 않고 자기 Mapper를 둔다(`mapper/global/`).
2. 도메인 안에서는 `controller` → `service` → `mapper` 순서로만 부른다.
3. 다른 도메인은 그 도메인의 `service`로만 부른다. 다른 도메인의 `mapper`를 직접 부르지 않는다.
4. 알림과 메일은 호출하지 않고 이벤트를 발행한다.

1~3번과 3.1~3.3절의 import 규칙은 ArchUnit 테스트(`ArchitectureTest`)가 검사한다. `./gradlew build`에서 이 테스트가 실패하면 메시지에 나온 import를 고친다. 테스트를 고치거나 꺼서 통과시키지 않는다. 도메인을 새로 만들면 3.1절의 층 표와 이 테스트를 같이 고친다.

### 3.1 도메인 간 호출은 낮은 층으로만

| 층 | 도메인 | 부를 수 있는 곳 |
| --- | --- | --- |
| 0 | `member` | 없음 |
| 1 | `pet` | 0층 |
| 2 | `health`, `place`, `notification` | 0~1층 |
| 3 | `expense`, `community`, `support` | 0~2층 |
| 4 | `ai` | 0~3층 |
| 5 | `admin`, `job` | 전부 |

- 자기보다 낮은 층의 service만 부른다. 같은 층끼리는 부르지 않는다. 이 규칙이면 두 도메인이 서로를 부르는 순환이 생기지 않는다.
- 위층의 기능이 필요하면 위층을 부르지 않는다. 위층이 아래층을 부르도록 방향을 바꾸거나, 이벤트를 발행하거나, 그 기능을 위층으로 옮긴다.
  - 예: 건강기록·가계부·커뮤니티의 활동이 케어미션(`pet`)에 반영되어야 한다. `pet`이 위층을 읽어 오지 않고, 활동이 일어난 도메인이 `pet`의 service를 부른다.
- 순환 참조 오류(`BeanCurrentlyInCreationException`)가 나면 위 방법으로 푼다. `ObjectProvider`, `@Lazy`, setter 주입으로 오류만 숨기지 않는다.
- 다른 도메인의 service는 인터페이스를 따로 만들지 않고 클래스를 바로 주입한다.

### 3.2 관리자 코드

| 순서 | 판정 | 위치 |
| --- | --- | --- |
| 1 | 읽거나 바꾸는 테이블이 한 도메인 소유인가 | 그 도메인의 `controller`에 `AdminXxxController` |
| 2 | 두 도메인 이상을 합쳐 보여 주는 조회인가 | `domain/admin` |
| 3 | 2번인데 데이터를 바꾸는가 | `admin`에 두지 않는다. 바꾸는 테이블을 소유한 도메인에 둔다 |

- 관리자 컨트롤러의 `@RequestMapping`은 반드시 `/api/v1/admin/`으로 시작한다. 관리자 권한은 `SecurityConfig`가 이 경로로 검사한다. 클래스 이름을 `AdminXxx`로 지어도 경로가 다르면 보호되지 않는다.
- 다른 도메인은 `admin`을 import하지 않는다.
- 관리자 계정도 회원 테이블의 한 행이다. 관리자 계정 관리는 `member`의 `AdminMemberController`에 둔다.

### 3.3 여러 도메인에 걸친 배치

여러 도메인의 service를 순서대로 불러야 하는 배치는 `domain/job`에 둔다. 한 도메인 안에서 끝나는 배치는 그 도메인에 둔다. 다른 도메인은 `job`을 import하지 않는다.

## 4. 계층별 규칙

### Controller

- `ApiResponse`로 감싼 값만 반환한다.
- 로그인한 회원은 `@AuthenticationPrincipal LoginMember`로 받는다. 회원 일련번호를 경로, 쿼리, 요청 본문으로 받지 않는다. 받은 값을 그대로 쓰면 다른 회원의 번호를 넣어 남의 데이터를 조회하거나 바꿀 수 있다.
- 리소스 경로 전체는 클래스의 `@RequestMapping`에 적고(`/api/v1/posts`), 메서드에는 하위 경로만 적는다.
- 요청 DTO에 `@Valid`를 붙인다.
- 메서드마다 `@Operation(summary = "...")`을 붙인다. 그 밖의 Swagger 어노테이션은 붙이지 않는다. 프론트는 Swagger UI(`/swagger-ui.html`)에서 API 명세를 본다.

```java
@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/posts")                             // 리소스 경로는 클래스에 적는다
public class PostController {
    private final PostService postService;

    @Operation(summary = "게시글 작성")
    @PostMapping                                             // 메서드에는 하위 경로만 적는다
    @ResponseStatus(HttpStatus.CREATED)                      // 생성은 201
    public ApiResponse<PostResponse> create(@AuthenticationPrincipal LoginMember me,
                                            @Valid @RequestBody PostCreateRequest req) {
        return ApiResponse.ok(postService.create(me.id(), req));
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{pstSn}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal LoginMember me, @PathVariable Long pstSn) {
        postService.delete(me.id(), pstSn);
        return ApiResponse.ok(null);                         // 삭제와 상태 변경은 data가 null이다
    }
}
```

### Service

- `@Transactional`은 클래스가 아니라 메서드에 붙인다. 데이터를 바꾸는 메서드에는 `@Transactional`, 조회만 하는 메서드에는 `@Transactional(readOnly = true)`를 붙인다. Service의 `public` 메서드에는 둘 중 하나가 반드시 있어야 한다. 빠뜨리면 ArchUnit 테스트(`ArchitectureTest`)가 실패한다. 트랜잭션이 필요 없는 `public` 메서드는 이 테스트의 예외 목록에 추가한다.

```java
@Service @RequiredArgsConstructor
public class PostService {

    @Transactional(readOnly = true)
    public PostResponse getPost(Long pstSn) { ... }

    @Transactional
    public PostResponse create(Long memberId, PostCreateRequest req) { ... }
}
```

- 권한 검사는 데이터를 바꾸거나 돌려주기 전에 한다.
  - 반려동물 권한은 `PetAuthorizer`로 메서드 첫 줄에서 검사한다.
  - 작성자 권한은 대상을 조회한 바로 다음에 `OwnerAuthorizer.checkOwner`로 검사한다.

- 오류는 `ErrorCode`에 추가하고 `BusinessException`을 던진다. 도메인별 예외 클래스를 만들지 않는다. 메시지는 화면설계서 문구를 그대로 쓴다.
- 암호화 컬럼은 `FieldCryptoService`의 `encrypt()`·`decrypt()`·`hmac()`을 서비스에서 호출한다. XML에는 암호문이 들어가고 암호문이 나온다.
- 이메일과 전화번호는 암호화하거나 비교하기 전에 정규화한다(이메일은 `normalizeEmail()`: 앞뒤 공백 제거와 소문자 변환, 전화번호는 `PhoneNumberNormalizer.normalize()`).
- 파일은 `FileStore`를 쓴다. 크기와 확장자 검사는 `FileStore`가 저장할 때 한다. 도메인 코드에서 다시 검사하지 않는다.
  - 파일 하나에 10MB 이하, 확장자는 jpg·jpeg·png·webp다. 검진 첨부만 pdf도 된다.
  - 어기면 `FILE_TOO_LARGE`, `UNSUPPORTED_FILE_TYPE`(둘 다 400)이 나간다.
  - 한 번에 올릴 수 있는 파일 개수는 화면마다 다르므로 기능명세서를 따른다.
- 알림은 `NotificationEvent`, 메일은 `MailEvent`를 발행한다. 알림 테이블에 직접 insert하지 않는다.
- 이벤트를 받아 알림을 저장하는 service는 `domain/notification`에 있다. 이벤트 클래스는 `global/notification`에 있다.
- 이벤트는 `@Transactional` 메서드 안에서 `EventPublisher.publish()`로만 발행한다. `ApplicationEventPublisher`를 직접 쓰지 않는다. 직접 쓰면 트랜잭션 밖에서 발행한 이벤트를 리스너가 받지 않고 버리며 오류도 나지 않는다.
- `EventPublisher.publish()`는 트랜잭션이 없으면 예외를 던진다. 같은 클래스의 `@Transactional` 메서드를 `this.메서드()`로 부른 경우에도 트랜잭션이 걸리지 않으므로 예외가 난다. 이때는 호출하는 메서드에 `@Transactional`을 붙인다.

```java
@Transactional
public void invite(Long memberId, InviteCreateRequest req) {
    // ... 초대 저장 ...
    eventPublisher.publish(new NotificationEvent(...));
}
// NG: notificationMapper.insert(...)  -- 알림은 직접 insert하지 않는다
```

### Mapper와 XML

- SQL은 XML에만 쓴다. `@Select` 같은 어노테이션 SQL을 쓰지 않는다.
- 정렬 컬럼은 enum으로만 받는다. 요청값을 SQL에 그대로 붙이지 않는다.
- 감사 컬럼(`REG_DT`, `MDFCN_DT`, `RGTR_ID`, `MDFR_ID`)은 XML에 직접 적는다. **테이블마다 가진 감사 컬럼이 다르다.** `REG_DT`만 있는 테이블도 있고 `MDFR_ID`가 없는 테이블도 있으므로, ERD에서 그 테이블에 있는 컬럼만 적는다.
  - 네 컬럼이 모두 있는 테이블의 수정 예: `MDFR_ID = #{actorId}, MDFCN_DT = NOW()`
  - 서비스가 `actorId`로 `String.valueOf(memberId)`을 넘기고, 배치는 `"SYSTEM"`을 넘긴다.
- 삭제는 테이블에 `DEL_YN` 컬럼이 있는지에 따라 다르다. ERD에서 확인한다.
  - `DEL_YN`이 있는 테이블은 삭제 API에서 `DELETE`를 쓰지 않고 `UPDATE ... SET DEL_YN = 'Y'`로 처리한다. 이 테이블을 조회하는 SQL에는 `DEL_YN = 'N'` 조건을 반드시 넣는다.
  - `DEL_YN`이 없는 테이블은 `DELETE`를 쓴다. 외래키가 삭제를 막으므로(`ON DELETE RESTRICT`) 자식 테이블부터 지운다.
  - `USE_YN`은 삭제가 아니라 사용 여부다. 삭제 용도로 쓰지 않는다.
  - 탈퇴 30일 파기 배치처럼 실제로 지워야 하는 작업만 `DEL_YN`이 있는 테이블에 `DELETE`를 쓴다.
- 페이지가 있는 목록은 아래 형태로만 쓰고 XML에 `LIMIT`을 직접 쓰지 않는다. 최근 N건이나 1건만 가져오는 조회는 XML에 `LIMIT`을 쓴다. 목록을 조회하는 Mapper는 응답 DTO 형태(`XxxSummary`)로 바로 돌려준다(목록에만 해당한다).

```java
@Transactional(readOnly = true)
public PageResponse<PostSummary> search(PostSearchCond cond, PageQuery pq) {
    PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(10));   // 바로 다음 select 한 번에만 적용된다
    List<PostSummary> rows = postMapper.selectList(cond, PostSort.from(pq.sort()));
    return PageResponse.of(new PageInfo<>(rows));
}
// NG: rows.stream().map(PostResponse::from).toList()  -- 전체 건수가 사라진다
```

### DTO와 Lombok

- MyBatis가 값을 채우는 클래스(VO, `XxxSummary`, `XxxSearchCond`)는 `@Data`를 붙인 클래스로 쓴다. `@Builder`는 붙이지 않는다. `@Builder`만 붙이면 기본 생성자가 없어져 MyBatis가 객체를 만들지 못한다.
- 요청·응답 DTO(`XxxRequest`, `XxxResponse`)는 `record`로 쓴다. 응답 DTO는 `XxxResponse.from(vo)` 정적 메서드로 VO에서 만든다.
- VO와 DTO 객체를 통째로 로그에 찍지 않는다(`log.info("{}", member)`). `@Data`와 `record`의 `toString()`은 모든 필드를 출력한다. 필요한 값만 골라 찍는다.
- VO와 DTO의 boolean 필드 이름에는 `is`를 붙이지 않는다(`owner`, `hasNext`, `canEdit`). `@Data` 클래스에서 필드가 `isOwner`면 Lombok이 getter를 `isOwner()`, setter를 `setOwner()`로 만들어 JSON 키와 MyBatis 속성 이름이 `owner`가 된다.
- 의존성은 `@RequiredArgsConstructor`와 `private final` 필드로만 주입한다. `@Autowired`를 쓰지 않는다.

```java
@Data
public class Post {                                      // 테이블 1:1 VO. MyBatis가 채운다
    private Long pstSn;
    private String pstTtl;
    private LocalDateTime regDt;
}

public record PostResponse(Long pstSn, String pstTtl, LocalDateTime regDt) {
    public static PostResponse from(Post post) {
        return new PostResponse(post.getPstSn(), post.getPstTtl(), post.getRegDt());
    }
}
// 읽을 때: @Data 클래스는 post.getPstTtl(), record는 res.pstTtl()
```

### VO와 enum

- VO는 부모 클래스 없이 테이블 컬럼만 필드로 가진다.
- Y/N 컬럼과 코드 컬럼은 `String`으로 둔다. TypeHandler를 쓰지 않는다.
- 상태·종류 enum은 `code` 필드와 `getCode()`, `is(String code)`를 가진다.
- 코드 값을 Java와 XML에 직접 적지 않는다. 비교는 enum의 `is()`로 하고, XML에는 서비스가 `getCode()`로 넘긴 값을 `#{mtchDlngSttsCd}`처럼 파라미터로 받는다. Y/N만 `"Y".equals(vo.getDelYn())`, `DEL_YN = 'N'`처럼 직접 쓴다.

```java
public enum MatchStatus {
    REQUESTED("01"), ACCEPTED("02");
    private final String code;
    MatchStatus(String code) { this.code = code; }
    public String getCode() { return code; }
    public boolean is(String code) { return this.code.equals(code); }
}
// 비교: MatchStatus.REQUESTED.is(match.getMtchDlngSttsCd())
// NG: "01".equals(match.getMtchDlngSttsCd())  -- 코드 값을 직접 적지 않는다
```

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

보호 파일 다운로드(파일 자체를 돌려준다)를 제외한 모든 응답은 아래 모양이다. 프론트는 이 모양에 맞춰 `data`를 꺼내고 `error.code`로 분기한다. 이 형식은 프론트와의 합의가 남아 있으므로(`docs/OPEN_ISSUES.md`), 바꿀 때는 프론트 저장소의 `AGENTS.md` 5장과 같이 고친다.

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

## 7. 여러 명이 같이 고치는 파일

`ErrorCode`와 `application.yml`은 모든 도메인 담당이 줄을 추가한다. 다들 맨 끝에 붙이면 병합할 때마다 충돌한다.

- 파일 안을 도메인별 구획으로 나누고(주석으로 구분), 자기 도메인 구획 안에 추가한다.
- 이 파일들의 줄 정렬이나 순서 바꾸기를 기능 PR에 섞지 않는다.

`db/`의 SQL 파일도 여러 명이 추가한다.

- 파일 이름은 `YYYYMMDD_설명.sql`이다(예: `20261015_add_pet_weight_unit.sql`).
- 병합된 SQL 파일은 고치지 않는다. 바꿀 것이 있으면 새 파일을 추가한다.
- 테이블을 바꾸는 PR은 본문에 "DB 변경 있음"과 파일 이름을 적는다.
- PR 작성자가 병합 직후 팀 공용 DB에 그 SQL을 실행하고 PR에 완료 댓글을 남긴다.

## 8. 설정과 비밀값

**이 저장소는 공개다. 한 번 커밋된 값은 지워도 이력에 남는다.**

- 비밀값은 모두 환경변수로만 넣는다. 예: `JWT_SECRET`, `DB_PASSWORD`, `ENC_KEY`, `HMAC_KEY`, `MAIL_PASSWORD`, 소셜 로그인 클라이언트 시크릿, 외부 API 키(AI, OCR, 지도).
- 설정 파일에는 `${DB_PASSWORD}`처럼 키 이름만 둔다.
- 설정 파일은 `application.yml`, `application-local.yml`, `application-prod.yml`로 나눈다.
- 로그에 비밀번호와 토큰을 남기지 않는다.
- 비밀값이 든 파일(`.env` 등)과 개인 작업 기록(`.claude/handoff.md`, `CLAUDE.local.md`)은 git에 올리지 않는다.

## 9. 테스트

- 테스트를 지우거나 끄거나 기대값을 바꿔서 통과시키지 않는다. 가끔 실패하는 테스트도 꺼서 넘기지 않고 원인을 고친다.
- PR은 `./gradlew build`가 통과해야 병합한다. `-x test`로 테스트를 건너뛰지 않는다. CI가 설정되기 전에는 PR을 올리는 사람이 로컬에서 돌린다.
- 컨트롤러 테스트를 쓸 때는 `@WebMvcTest`와 `@WithMockLoginMember`를 쓴다.
- DB에 붙는 자동 테스트는 만들지 않는다. 팀이 같이 쓰는 통합 DB에 붙는 테스트는 다른 사람의 데이터를 바꾸고, Docker를 쓰지 않으므로 Testcontainers도 쓸 수 없다. Mapper는 로컬 MySQL에 붙여 직접 실행해 확인한다.
- `DEL_YN`이 있는 테이블을 조회하는 `<select>`에 `DEL_YN` 조건이 있는지는 Mapper XML을 읽는 테스트(`MapperXmlRuleTest`)가 검사한다(0장 5번). 이 테스트가 실패하면 SQL에 조건을 넣는다. 삭제된 행까지 읽어야 하는 조회는 이 테스트의 예외 목록에 추가한다.

## 10. Git 규칙 (제안, 팀 합의 전)

### 브랜치

| 브랜치 | 용도 |
| --- | --- |
| `main` | 배포본. `develop`과 `hotfix/`에서만 PR |
| `develop` | 통합. 개인 브랜치와 `hotfix/`에서 PR |
| 개인 브랜치 | 한 사람이 하나를 계속 쓴다. 이름은 자기 이니셜(예: `LJS`) |
| `hotfix/{내용}` | 배포본 긴급 수정. `main`에서 분기해 `main`과 `develop` 양쪽에 병합하고 지운다 |

- 기능마다 브랜치를 새로 만들지 않고 자기 브랜치에서 작업해 `develop`으로 PR을 올린다.
- PR 하나에는 기능 하나만 담는다. 열려 있는 PR이 있는 동안 같은 브랜치에 올린 커밋은 그 PR에 함께 들어가므로, 앞 PR이 병합된 뒤에 다음 기능을 시작한다.
- 새 작업을 시작하기 전에 `develop`의 최신 내용을 자기 브랜치에 합친다. 건너뛰면 다음 PR에서 충돌이 커진다. `git fetch origin` 다음에 `git merge origin/develop`으로 합치고 rebase는 쓰지 않는다. 개인 브랜치는 이미 푸시되어 있어서 rebase하면 커밋이 새로 만들어지고 강제 푸시가 필요해진다.
- 병합은 merge 커밋으로 한다. squash는 쓰지 않는다. 개인 브랜치는 병합 뒤에도 계속 쓰는데, squash로 합치면 이력이 갈라져 이미 병합된 변경이 다음 PR에 다시 나타난다.
- `main`과 `develop`에는 직접 푸시하지 않는다.
- `hotfix/`는 배포본에 문제가 생겨 `develop`의 다음 배포를 기다릴 수 없을 때만 쓴다. `main`으로 PR을 올려 병합한 뒤 같은 내용을 `develop`에도 병합한다. 한쪽만 병합하면 다음 배포에서 수정이 사라진다.

### 커밋

```
[<이니셜>] <종류>: <무엇을 했는지>

[LJS] feat: 반려동물 등록 API 추가
[LJS] fix: 체중 기록 삭제 시 프로필 체중이 갱신되지 않는 문제 수정
```

종류는 `feat`, `fix`, `refactor`, `test`, `docs`, `chore`다. 이니셜은 자기 브랜치 이름과 같게 쓴다. 관련 기능명세서 ID가 있으면 본문에 적는다(예: `FN-MYP-003-01`).

커밋 메시지에 `Co-Authored-By:` 줄을 넣지 않는다. AI 도구가 자동으로 붙인 줄도 커밋하기 전에 지운다. GitHub은 이 줄에 적힌 계정을 그 커밋의 공동 작성자로 표시한다.

### PR

- 대상은 `develop`이다(`hotfix/`만 `main`과 `develop` 양쪽). 승인과 병합은 저장소 관리자가 한다. CI 통과 후 merge 커밋으로 병합한다.
- 제목은 커밋 형식과 같게 쓴다.
- 본문에 관련 FN ID, 무엇을 왜 바꿨는지, 어떻게 확인했는지를 적는다.

### 작업 상태 공유

- 기능의 현재 상태는 GitHub 이슈로 관리한다. 기능 하나에 이슈 하나, 제목에 FN ID나 화면 ID를 쓴다.
- 다른 사람의 코드에 영향을 주는 결정은 개인 기록에 두지 않는다. 이 문서를 고치는 PR로 알린다.

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
