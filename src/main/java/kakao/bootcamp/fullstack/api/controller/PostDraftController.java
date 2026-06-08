package kakao.bootcamp.fullstack.api.controller;

import jakarta.validation.Valid;
import java.util.List;
import kakao.bootcamp.fullstack.api.dto.request.AuthMember;
import kakao.bootcamp.fullstack.api.dto.request.PostDraftCreateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.PostDraftUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.response.PostDraftDetailsResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostDraftSaveResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostDraftsSummaryResDto;
import kakao.bootcamp.fullstack.api.service.PostDraftService;
import kakao.bootcamp.fullstack.global.exception.code.SuccessCode;
import kakao.bootcamp.fullstack.global.jwt.annotation.LoginMember;
import kakao.bootcamp.fullstack.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts/drafts")
public class PostDraftController {

    private final PostDraftService postDraftService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostDraftsSummaryResDto>>> getAllPostDrafts(
            @LoginMember AuthMember authMember) {
        List<PostDraftsSummaryResDto> response = postDraftService.getPostDrafts(authMember.memberId());
        return ResponseEntity.status(SuccessCode.SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS, response));
    }

    @GetMapping("/{draftId}")
    public ResponseEntity<ApiResponse<PostDraftDetailsResDto>> getPostDraft(
            @LoginMember AuthMember authMember,
            @PathVariable Long draftId) {
        PostDraftDetailsResDto response = postDraftService.getPostDraft(authMember.memberId(), draftId);
        return ResponseEntity.status(SuccessCode.SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS, response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostDraftSaveResDto>> postDraft(
            @LoginMember AuthMember authMember,
            @Valid @RequestBody PostDraftCreateReqDto request) {
        PostDraftSaveResDto response = postDraftService.createPostDraft(authMember.memberId(), request);
        return ResponseEntity.status(SuccessCode.CREATED.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.CREATED, response));
    }

    @PatchMapping("/{draftId}")
    public ResponseEntity<ApiResponse<PostDraftSaveResDto>> updatePostDraft(
            @LoginMember AuthMember authMember,
            @PathVariable Long draftId,
            @Valid @RequestBody PostDraftUpdateReqDto request) {
        PostDraftSaveResDto response = postDraftService.updatePostDraft(authMember.memberId(), draftId, request);
        return ResponseEntity.status(SuccessCode.SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS, response));
    }

    @DeleteMapping("/{draftId}")
    public ResponseEntity<ApiResponse<Void>> deletePostDraft(
            @LoginMember AuthMember authMember,
            @PathVariable Long draftId) {
        postDraftService.deletePostDraft(authMember.memberId(), draftId);
        return ResponseEntity.status(SuccessCode.SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS));
    }
}
