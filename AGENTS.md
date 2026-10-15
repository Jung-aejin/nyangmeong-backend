# 냥멍케어 백엔드 컨벤션

이 저장소에서 코드를 쓰는 사람과 AI 도구가 따르는 규칙이다. 이 파일에는 구조와 의존 방향처럼 항상 필요한 규칙만 있고, 나머지는 `docs/convention/`에 있다("나머지 규칙이 있는 곳").

- Codex는 이 파일을 자동으로 읽는다. Claude Code는 `CLAUDE.md`의 `@AGENTS.md` 한 줄로 읽는다. 그 줄을 지우면 규칙이 통째로 빠진다.
- Git 규칙(`docs/convention/git.md`)은 팀 합의 전 제안이다. 아직 정하지 않은 것은 `docs/OPEN_ISSUES.md`에 있다.

## 코드를 쓰기 전에 꼭 알 것

어기면 되돌리기 어렵거나 오류 없이 조용히 잘못되는 것만 모았다. 나머지는 필요할 때 찾아본다.

1. **비밀값을 커밋하지 않는다.** 이 저장소는 공개다. 비밀번호, 키, 토큰은 환경변수로만 넣는다. (설정과 비밀값)
2. **의존은 한 방향이다.** 도메인이 `global`을 쓰고, 다른 도메인은 `service`로만, 자기보다 낮은 층만 부른다. 순환 참조 오류가 나면 `@Lazy`나 `ObjectProvider`로 덮지 않고 이벤트로 바꾸거나 기능을 위층으로 옮긴다. (의존 방향)
3. **알림과 메일은 `@Transactional` 메서드 안에서 `EventPublisher.publish()`로 발행한다.** 트랜잭션 밖에서 부르면 예외가 난다. `ApplicationEventPublisher`를 직접 쓰면 이 검사가 빠져서 오류 없이 사라진다. (`docs/convention/service.md`)
4. **테스트를 끄거나 지워서 통과시키지 않는다.** (테스트)
5. **`DEL_YN`이 있는 테이블을 조회할 때는 `DEL_YN = 'N'` 조건을 넣는다.** 빠뜨리면 삭제된 데이터가 오류 없이 화면에 나온다. 삭제는 `DELETE`가 아니라 `DEL_YN = 'Y'`로 바꾸는 `UPDATE`다. (`docs/convention/mapper.md`)

## 기술 스택

Java 25, Spring Boot 4.1, Spring Security 7, MyBatis(Mapper 인터페이스 + XML), MySQL 8, Lombok, Gradle. Docker는 쓰지 않는다.

### 편집기와 포맷

- 백엔드는 VS Code로 작업한다. 포맷 설정은 저장소의 `.vscode/settings.json`에 있고 저장할 때 자동으로 적용된다. 개인 설정으로 덮어쓰지 않는다.
- Java는 공백 4칸으로 들여쓴다. 줄바꿈은 LF다(`.gitattributes`가 커밋할 때 맞춘다).
- Mapper XML은 자동 포맷하지 않는다. 포매터가 SQL의 줄 배치를 바꾸기 때문이다.
- 다른 IDE를 쓰면 파일 전체 포맷을 하지 않는다. 포매터가 달라서 다른 사람이 쓴 줄까지 바뀐다.

## 패키지 구조

```
com.nyangmeong
├─ global/                 공통 모듈. 도메인을 모른다
│  ├─ response/  error/  security/  auth/  validation/  file/
│  ├─ notification/  mail/  mybatis/  code/  setting/
│  └─ external/  batch/  config/  log/
├─ member/  pet/  health/  community/  place/
├─ support/  notification/  expense/
│                          업무 기능. 도메인 1개 = 패키지 1개
├─ admin/                  여러 도메인을 합치는 관리자 조회만
└─ job/                    여러 도메인에 걸친 배치만

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
| `mapper` | MyBatis 인터페이스 | (`docs/convention/mapper.md`) |
| `domain` | 테이블 1:1 VO, 상태·종류 enum | 요청·응답 전용 필드 |

- 도메인 안에서 따로 묶을 기능은 하위 패키지로 둘 수 있다(지금은 `community/chat` 하나). 하위 패키지 안도 같은 다섯 패키지로 나눈다.
- 패키지 깊이는 `com.nyangmeong` 아래 3단계를 넘지 않는다. 가장 깊은 경우가 `community/chat/controller`다.
- 설정 클래스는 그 기능의 패키지에 둔다(예: `SecurityConfig`는 `global/security`). `global/config`에는 속할 기능 패키지가 없는 설정만 둔다(`OpenApiConfig`, `AsyncConfig`, `WebConfig`).

## 의존 방향

1. 도메인은 `global`을 쓴다. `global`은 도메인을 참조하지 않는다. `global`이 테이블을 읽어야 하면 도메인의 Mapper를 쓰지 않고 자기 Mapper를 둔다(`mapper/global/`).
2. 도메인 안에서는 `controller` → `service` → `mapper` 순서로만 부른다.
3. 다른 도메인은 그 도메인의 `service`로만 부른다. 다른 도메인의 `mapper`를 직접 부르지 않는다.
4. 알림과 메일은 호출하지 않고 이벤트를 발행한다.

1~3번과 이 장 아래 세 절의 import 규칙은 ArchUnit 테스트(`ArchitectureTest`)가 검사한다. `./gradlew build`에서 이 테스트가 실패하면 메시지에 나온 import를 고친다. 테스트를 고치거나 꺼서 통과시키지 않는다. 도메인을 새로 만들면 "도메인 간 호출은 낮은 층으로만"의 층 표와 이 테스트를 같이 고친다.

### 도메인 간 호출은 낮은 층으로만

| 층 | 도메인 | 부를 수 있는 곳 |
| --- | --- | --- |
| 0 | `member` | 없음 |
| 1 | `pet` | 0층 |
| 2 | `health`, `place`, `notification` | 0~1층 |
| 3 | `expense`, `community`, `support` | 0~2층 |
| 4 | `admin`, `job` | 전부 |

- 자기보다 낮은 층의 service만 부른다. 같은 층끼리는 부르지 않는다. 이 규칙이면 두 도메인이 서로를 부르는 순환이 생기지 않는다.
- 위층의 기능이 필요하면 위층을 부르지 않는다. 위층이 아래층을 부르도록 방향을 바꾸거나, 이벤트를 발행하거나, 그 기능을 위층으로 옮긴다.
  - 예: 건강기록·가계부·커뮤니티의 활동이 케어미션(`pet`)에 반영되어야 한다. `pet`이 위층을 읽어 오지 않고, 활동이 일어난 도메인이 `pet`의 service를 부른다.
- 순환 참조 오류(`BeanCurrentlyInCreationException`)가 나면 위 방법으로 푼다. `ObjectProvider`, `@Lazy`, setter 주입으로 오류만 숨기지 않는다.
- 다른 도메인의 service는 인터페이스를 따로 만들지 않고 클래스를 바로 주입한다.

### 관리자 코드

| 순서 | 판정 | 위치 |
| --- | --- | --- |
| 1 | 읽거나 바꾸는 테이블이 한 도메인 소유인가 | 그 도메인의 `controller`에 `AdminXxxController` |
| 2 | 두 도메인 이상을 합쳐 보여 주는 조회인가 | `admin` |
| 3 | 2번인데 데이터를 바꾸는가 | `admin`에 두지 않는다. 바꾸는 테이블을 소유한 도메인에 둔다 |

- 관리자 컨트롤러의 `@RequestMapping`은 반드시 `/api/v1/admin/`으로 시작한다. 관리자 권한은 `SecurityConfig`가 이 경로로 검사한다. 클래스 이름을 `AdminXxx`로 지어도 경로가 다르면 보호되지 않는다.
- 다른 도메인은 `admin`을 import하지 않는다.
- 관리자 계정도 회원 테이블의 한 행이다. 관리자 계정 관리는 `member`의 `AdminMemberController`에 둔다.

### 여러 도메인에 걸친 배치

여러 도메인의 service를 순서대로 불러야 하는 배치는 `job`에 둔다. 한 도메인 안에서 끝나는 배치는 그 도메인에 둔다. 다른 도메인은 `job`을 import하지 않는다.

## 나머지 규칙이 있는 곳

아래 파일은 자동으로 읽히지 않는다. 해당하는 일을 하기 전에 연다.

| 이럴 때 | 읽는 파일 |
| --- | --- |
| 코드를 쓸 때 항상 | `docs/convention/common.md`: 로그와 예외, 주석, 이름 짓기, 공통 모듈에서 가져다 쓰는 것 |
| Controller를 쓸 때 | `docs/convention/controller.md`, `docs/convention/api.md` |
| Service를 쓸 때 | `docs/convention/service.md` |
| Mapper, Mapper XML, VO, enum을 쓸 때 | `docs/convention/mapper.md` |
| DTO를 쓸 때 | `docs/convention/dto.md` |
| 브랜치·커밋·PR을 만들 때 | `docs/convention/git.md` |

## 여러 명이 같이 고치는 파일

`ErrorCode`와 `application.yml`은 모든 도메인 담당이 줄을 추가한다. 다들 맨 끝에 붙이면 병합할 때마다 충돌한다.

- 파일 안을 도메인별 구획으로 나누고(주석으로 구분), 자기 도메인 구획 안에 추가한다.
- 이 파일들의 줄 정렬이나 순서 바꾸기를 기능 PR에 섞지 않는다.

`db/`의 SQL 파일도 여러 명이 추가한다.

- 파일 이름은 `YYYYMMDD_설명.sql`이다(예: `20261015_add_pet_weight_unit.sql`).
- 병합된 SQL 파일은 고치지 않는다. 바꿀 것이 있으면 새 파일을 추가한다.
- 테이블을 바꾸는 PR은 본문에 "DB 변경 있음"과 파일 이름을 적는다.
- PR 작성자가 병합 직후 팀 공용 DB에 그 SQL을 실행하고 PR에 완료 댓글을 남긴다.

## 설정과 비밀값

**이 저장소는 공개다. 한 번 커밋된 값은 지워도 이력에 남는다.**

- 비밀값은 모두 환경변수로만 넣는다. 예: `JWT_SECRET`, `DB_PASSWORD`, `ENC_KEY`, `HMAC_KEY`, `MAIL_PASSWORD`, 소셜 로그인 클라이언트 시크릿, 외부 API 키(AI, OCR, 지도).
- 설정 파일에는 `${DB_PASSWORD}`처럼 키 이름만 둔다.
- 설정 파일은 `application.yml`, `application-local.yml`, `application-prod.yml`로 나눈다.
- 로그에 비밀번호와 토큰을 남기지 않는다.
- 비밀값이 든 파일(`.env` 등)과 `CLAUDE.local.md`는 git에 올리지 않는다.
- 작업 기록은 `handoff/<이름>.md`에 사람마다 한 파일로 두고 커밋한다. 다른 PC에서도 이어서 작업하기 위해서다. 공개 저장소이므로 비밀값과 개인 PC 경로를 쓰지 않는다.

## 테스트

- 테스트를 지우거나 끄거나 기대값을 바꿔서 통과시키지 않는다. 가끔 실패하는 테스트도 꺼서 넘기지 않고 원인을 고친다.
- PR은 `./gradlew build`가 통과해야 병합한다. `-x test`로 테스트를 건너뛰지 않는다. CI가 설정되기 전에는 PR을 올리는 사람이 로컬에서 돌린다.
- 컨트롤러 테스트를 쓸 때는 `@WebMvcTest`와 `@WithMockLoginMember`를 쓴다.
- DB에 붙는 자동 테스트는 만들지 않는다. 팀이 같이 쓰는 통합 DB에 붙는 테스트는 다른 사람의 데이터를 바꾸고, Docker를 쓰지 않으므로 Testcontainers도 쓸 수 없다. Mapper는 로컬 MySQL에 붙여 직접 실행해 확인한다.
- `DEL_YN`이 있는 테이블을 조회하는 `<select>`에 `DEL_YN` 조건이 있는지는 Mapper XML을 읽는 테스트(`MapperXmlRuleTest`)가 검사한다("코드를 쓰기 전에 꼭 알 것" 5번). 이 테스트가 실패하면 SQL에 조건을 넣는다. 삭제된 행까지 읽어야 하는 조회는 이 테스트의 예외 목록에 추가한다.
