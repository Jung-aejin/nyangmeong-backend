# 냥멍케어 백엔드 컨벤션: DTO

`AGENTS.md`에서 이어지는 문서다.

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
