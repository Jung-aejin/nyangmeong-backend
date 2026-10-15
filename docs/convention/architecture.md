# 패키지 세부 규칙, 관리자 코드, 배치

패키지 구조와 의존 방향의 기본은 `AGENTS.md`에 있다. 이 문서는 하위 패키지, 관리자 기능, 여러 도메인에 걸친 배치를 만들 때 연다.

## 패키지 세부 규칙

- 도메인 안에서 따로 묶을 기능은 하위 패키지로 둘 수 있다(지금은 `community/chat` 하나). 하위 패키지 안도 같은 다섯 패키지로 나눈다.
- 패키지 깊이는 `com.nyangmeong` 아래 3단계를 넘지 않는다. 가장 깊은 경우가 `community/chat/controller`다.
- 설정 클래스는 그 기능의 패키지에 둔다(예: `SecurityConfig`는 `global/security`). `global/config`에는 속할 기능 패키지가 없는 설정만 둔다(`OpenApiConfig`, `AsyncConfig`, `WebConfig`).

## 관리자 코드

| 순서 | 판정 | 위치 |
| --- | --- | --- |
| 1 | 읽거나 바꾸는 테이블이 한 도메인 소유인가 | 그 도메인의 `controller`에 `AdminXxxController` |
| 2 | 두 도메인 이상을 합쳐 보여 주는 조회인가 | `admin` |
| 3 | 2번인데 데이터를 바꾸는가 | `admin`에 두지 않는다. 바꾸는 테이블을 소유한 도메인에 둔다 |

- 관리자 컨트롤러의 `@RequestMapping`은 반드시 `/api/v1/admin/`으로 시작한다. 관리자 권한은 `SecurityConfig`가 이 경로로 검사한다. 클래스 이름을 `AdminXxx`로 지어도 경로가 다르면 보호되지 않는다.
- 다른 도메인은 `admin`을 import하지 않는다.
- 관리자 계정도 회원 테이블의 한 행이다. 관리자 계정 관리는 `member`의 `AdminMemberController`에 둔다.

## 여러 도메인에 걸친 배치

여러 도메인의 service를 순서대로 불러야 하는 배치는 `job`에 둔다. 한 도메인 안에서 끝나는 배치는 그 도메인에 둔다. 다른 도메인은 `job`을 import하지 않는다.
