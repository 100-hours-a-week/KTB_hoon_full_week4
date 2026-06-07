package kakao.bootcamp.fullstack.api.controller;


import jakarta.validation.Valid;
import java.util.List;
import kakao.bootcamp.fullstack.api.dto.request.AuthMember;
import kakao.bootcamp.fullstack.api.dto.request.CommentCreateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.CommentUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.PostCreateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.PostReportReqDto;
import kakao.bootcamp.fullstack.api.dto.request.PostUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.response.CommentCreateResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostCreateResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostDetailsResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostLikeResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostSummaryResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostUpdateResDto;
import kakao.bootcamp.fullstack.api.service.PostService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostSummaryResDto>>> getPosts(
            @LoginMember AuthMember authMember,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") Long size
    ) {
        List<PostSummaryResDto> response = postService.getPostSummariesList(authMember.memberId(), cursor, size);
        return ResponseEntity.status(SuccessCode.SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS, response));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailsResDto>> getPost(
            @LoginMember AuthMember authMember,
            @PathVariable Long postId){
        PostDetailsResDto response = postService.getPostDetails(authMember.memberId(), postId);
        return ResponseEntity.status(SuccessCode.SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS, response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostCreateResDto>> createPost(
            @LoginMember AuthMember authMember,
            @Valid @RequestBody PostCreateReqDto request){
        PostCreateResDto response = postService.createPost(authMember.memberId(), request);
        return ResponseEntity.status(SuccessCode.SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS, response));
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostUpdateResDto>> updatePost(
            @LoginMember AuthMember authMember,
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateReqDto request){
        PostUpdateResDto response = postService.updatePost(authMember.memberId(), postId, request);
        return ResponseEntity.status(SuccessCode.SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS, response));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @LoginMember AuthMember authMember,
            @PathVariable Long postId){
        postService.deletePost(authMember.memberId(), postId);
        return ResponseEntity.status(SuccessCode.SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS));
    }

    @PostMapping("/{postId}/likes")
    public ResponseEntity<ApiResponse<PostLikeResDto>> likePost(
            @LoginMember AuthMember authMember,
            @PathVariable Long postId){
        PostLikeResDto response = postService.postLike(authMember.memberId(), postId);
        return ResponseEntity.status(SuccessCode.SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS,response));
    }

    @DeleteMapping("/{postId}/likes")
    public ResponseEntity<ApiResponse<PostLikeResDto>> unlikePost(
            @LoginMember AuthMember authMember,
            @PathVariable Long postId){
        PostLikeResDto response = postService.postUnLike(authMember.memberId(), postId);
        return ResponseEntity.status(SuccessCode.SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS,response));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentCreateResDto>> createComment(
            @LoginMember AuthMember authMember,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateReqDto request){
        CommentCreateResDto response = postService.createComment(authMember.memberId(), postId, request);
        return ResponseEntity.status(SuccessCode.COMMENT_CREATED_SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.COMMENT_CREATED_SUCCESS, response));
    }

    @PatchMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> updateComment(
            @LoginMember AuthMember authMember,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateReqDto request){
        postService.updateComment(authMember.memberId(), postId, commentId, request);
        return ResponseEntity.status(SuccessCode.COMMENT_UPDATED_SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.COMMENT_UPDATED_SUCCESS));
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @LoginMember AuthMember authMember,
            @PathVariable Long postId,
            @PathVariable Long commentId){
        postService.deleteComment(authMember.memberId(), postId, commentId);
        return ResponseEntity.status(SuccessCode.COMMENT_DELETED_SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.COMMENT_DELETED_SUCCESS));
    }
}
