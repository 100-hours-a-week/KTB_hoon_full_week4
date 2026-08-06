package kakao.bootcamp.fullstack.api.controller;

import jakarta.validation.Valid;
import kakao.bootcamp.fullstack.api.dto.request.PostSearchReqDto;
import kakao.bootcamp.fullstack.api.dto.response.PostSummaryPageResDto;
import kakao.bootcamp.fullstack.api.service.SearchService;
import kakao.bootcamp.fullstack.global.exception.code.SuccessCode;
import kakao.bootcamp.fullstack.global.response.ApiResponse;
import kakao.bootcamp.fullstack.global.security.dto.AuthMember;
import kakao.bootcamp.fullstack.global.security.jwt.annotation.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PostSummaryPageResDto>> searchPosts(
            @LoginMember AuthMember authMember, @Valid @ModelAttribute PostSearchReqDto request) {
        PostSummaryPageResDto response = searchService.searchPosts(authMember.memberId(), request);
        return ResponseEntity.status(SuccessCode.SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS, response));
    }
}
