# 냥멍케어 백엔드 컨벤션: Controller

`AGENTS.md`에서 이어지는 문서다.

## Controller

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
