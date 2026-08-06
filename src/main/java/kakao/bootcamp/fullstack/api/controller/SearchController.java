package kakao.bootcamp.fullstack.api.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kakao.bootcamp.fullstack.api.dto.response.PostSummaryPageResDto;
import kakao.bootcamp.fullstack.api.service.SearchService;
import kakao.bootcamp.fullstack.global.exception.code.SuccessCode;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;
import kakao.bootcamp.fullstack.global.response.ApiResponse;
import kakao.bootcamp.fullstack.global.security.dto.AuthMember;
import kakao.bootcamp.fullstack.global.security.jwt.annotation.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PostSummaryPageResDto>> searchPosts(
            @LoginMember AuthMember authMember,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10")
                    @Min(value = 1, message = ValidationCode.INVALID_PAGE_SIZE)
                    @Max(value = 10, message = ValidationCode.INVALID_PAGE_SIZE)
                    Long size) {
        PostSummaryPageResDto response =
                searchService.searchPosts(authMember.memberId(), keyword, cursor, size);
        return ResponseEntity.status(SuccessCode.SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS, response));
    }
}
