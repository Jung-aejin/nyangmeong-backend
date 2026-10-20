# LJS 핸드오프

## 지금 하는 것

JWT 라이브러리 교체 jjwt → Nimbus(`spring-security-oauth2-jose`) — 구현 중 (승인 2026-10-20 23:03, 시작 2026-10-20 23:01)
화면 ID 없는 공통 기반 작업이라 `milestones/LJS.md`에 절이 없다. 동작 변화 없이 구현만 바꾼다. `sid` 클레임 추가와 로그인·재발급·로그아웃은 범위 밖.

### 기획

- JWT-1 의존성 교체: `build.gradle`에서 `jjwt-api`·`jjwt-impl`·`jjwt-jackson` 3줄과 그 주석을 지우고 `org.springframework.security:spring-security-oauth2-jose`(버전은 Boot BOM)를 추가 / 완료 조건 `명령: ./gradlew -q dependencies --configuration runtimeClasspath | grep -E 'jjwt|oauth2-jose'` 출력에 jjwt 없음, oauth2-jose 있음
- JWT-2 `global/security/jwt/JwtTokenProvider.java` 내부 구현 교체
  - 공개 메서드 `createAccessToken(Long)`·`createRefreshToken(Long)`·`parseMbrSn(String)`·`initializeKey()`의 시그니처와 오류 코드(`TOKEN_EXPIRED`·`UNAUTHORIZED`)는 그대로
  - `initializeKey()`: Base64·32바이트 검사 유지. 키 길이로 알고리즘 선택(64바이트 이상 HS512, 48 이상 HS384, 32 이상 HS256). `SecretKeySpec`으로 키, `NimbusJwtEncoder(new ImmutableSecret<>(key))`(헤더에 kid 없음), `NimbusJwtDecoder.withSecretKey(key).macAlgorithm(alg)`를 여기서 한 번 만든다
  - 만료 여유 0초: `setJwtValidator(new JwtTimestampValidator(Duration.ZERO))`. 기본 60초면 만료 테스트(-1초 만료)가 통과해 버린다
  - `parseMbrSn` 예외 매핑: `JwtValidationException` → `TOKEN_EXPIRED`, 그 밖의 `JwtException`·`IllegalArgumentException` → `UNAUTHORIZED`. 좁은 예외부터 잡는다. subject 숫자 변환 실패 → `UNAUTHORIZED` 유지
  - 발급: `JwtClaimsSet`에 `subject(mbrSn)`·`issuedAt`·`expiresAt`, `JwsHeader.with(alg)`
  - 완료 조건 `명령: ./gradlew build -q` 종료 코드 0
- JWT-3 `JwtTokenProviderTest.java` 64~66행: 숫자가 아닌 subject 토큰을 jjwt 대신 Nimbus로 만든다. 단언·기대값은 바꾸지 않는다. 기존 5개 테스트 모두 기대값 그대로 통과. 이 수정은 별도 커밋(사람 승인 받음, 2026-10-20) / 완료 조건 `명령: ./gradlew test --tests '*JwtTokenProviderTest'` 5개 통과
- 회귀: `JwtAuthenticationFilterTest`·`SecurityConfigTest`·`DevTokenControllerTest`·`AuthControllerTest`가 바뀌지 않은 채 통과 / 완료 조건 `명령: ./gradlew build -q`, `화면: Postman으로 /api/auth/dev-tokens 발급 후 GET /api/auth/me 200`

새 의존성: `org.springframework.security:spring-security-oauth2-jose` (Spring Boot BOM 관리, 현재 spring-security-core 7.1.1과 같은 버전). 이유: 카카오 OAuth2(`spring-boot-starter-security-oauth2-client`)가 쓰는 JWT 스택과 하나로 맞추고, jjwt-jackson이 끌어오던 Jackson 2를 뺀다. 제거: `io.jsonwebtoken:jjwt-api`·`jjwt-impl`·`jjwt-jackson` 0.12.6

직전: SCR-COM-002 기본 레이아웃 — 끝 (2026-10-20 16:21). 기능 ID와 추가 요청 1~13은 그 마일스톤 커밋 메시지 본문에 있다.

## 다음에 할 것

- SCR-COM-002 연결 남음: YAY가 `POST /api/auth/login`·`logout`·`refresh`를 만들면 `DevTokenController`를 지운다. 로그인은 `JwtTokenProvider`·`TokenCookies`·`TokenHashUtil`을 쓰고 Refresh 토큰 해시를 RFRSH_TKN에 저장한다. 재발급은 `TOKEN_EXPIRED` 뒤 프론트가 부른다
- SCR-COM-002 연결 남음: `/auth/me`의 프로필 이미지 URL은 `FileStore`·파일 조회 API(SCR-MYP-010) 뒤 연결. 지금은 `atchFileSn`만 준다
- YAY 소셜 로그인 때 nct-backend의 `global/security/handler`(OAuth2 성공·실패 핸들러)와 `CustomOAuth2UserService`를 가져올 수 있다. 위치(`auth` 도메인 또는 `global/security`)는 `docs/OPEN_ISSUES.md`에서 정한다
- SCR-AIM-008 때 nct-backend의 `global/idempotency`(멱등 요청 인터셉터, `RequestFingerprintMapper`)를 가져와 영수증 저장에 쓴다
- 디스코드 공유 `application.properties`를 맞춘다: 팀 공용 `app.crypto.*`·`app.jwt.secret-base64` 키를 정해 넣고, `mybatis.configuration.map-underscore-to-camel-case`는 넣지 않는다(추가-12, resultMap 방식)
- 공용 DB의 테스트 회원(MBR_SN 1, `ljs_test`, RGTR_ID `LJS`)은 개인 키로 암호화돼 있다. 팀 키를 정한 뒤 지우거나 다시 만든다
- YAY에게 알린다: 로그인·로그아웃·재발급 API는 `auth` 도메인(`auth/controller/AuthController`)에, 접두어는 `/api`(추가-9·10)
- 프론트엔드 저장소 변경(`src/api/axios.js` baseURL `/api`, `docs/convention/{api,pages}.md`, `scripts/spec-extract.sh`, start 스킬)을 따로 커밋한다
