package kakao.bootcamp.fullstack.api.service;

import java.time.LocalDateTime;
import java.util.List;
import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.post.Comment;
import kakao.bootcamp.fullstack.api.domain.post.CommentErrorCode;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostErrorCode;
import kakao.bootcamp.fullstack.api.domain.post.PostLike;
import kakao.bootcamp.fullstack.api.dto.request.CommentCreateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.CommentUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.PostCreateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.PostReportReqDto;
import kakao.bootcamp.fullstack.api.dto.request.PostUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.response.CommentCreateResDto;
import kakao.bootcamp.fullstack.api.dto.response.CommentResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostCreateResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostDetailsResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostLikeResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostSummaryResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostUpdateResDto;
import kakao.bootcamp.fullstack.api.repository.CommentRepository;
import kakao.bootcamp.fullstack.api.repository.MemberRepository;
import kakao.bootcamp.fullstack.api.repository.PostLikeRepository;
import kakao.bootcamp.fullstack.api.repository.PostRepository;
import kakao.bootcamp.fullstack.api.repository.ReportRepository;
import kakao.bootcamp.fullstack.global.exception.ConflictException;
import kakao.bootcamp.fullstack.global.exception.ForbiddenException;
import kakao.bootcamp.fullstack.global.exception.NotFoundException;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import kakao.bootcamp.fullstack.global.utils.CursorParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final PostLikeRepository postLikeRepository;
    private final ReportRepository reportRepository;

    // TODO : 삭제된 게시물은 조회대상에서 제외해야 함
    public List<PostSummaryResDto> getPostSummariesList(Long memberId, String cursor, Long size){
        Member member = loadMemberOrThrow(memberId);
        if (cursor == null || cursor.isBlank()) {
            return postRepository.findFirstPage(size)
                    .stream()
                    .map(PostSummaryResDto::from)
                    .toList();
        }
        LocalDateTime cursorCreatedAt = CursorParser.parseCreatedAt(cursor);
        Long cursorId = CursorParser.parsePostId(cursor);
        return postRepository.findNextPage(cursorCreatedAt, cursorId, size)
                .stream()
                .map(PostSummaryResDto::from)
                .toList();
    }

    // TODO : 삭제된 게시물은 조회대상에서 제외해야 함
    public PostDetailsResDto getPostDetails(Long memberId, Long postId) {
        Member member = loadMemberOrThrow(memberId);
        Post post = loadPostOrThrow(postId);
        post.increaseViewCount();
        List<CommentResDto> commentResDtos = commentRepository
                .findByPostId(postId)
                .stream()
                .map(comment ->
                        CommentResDto.from(comment, comment.isWriter(memberId)))
                .toList();
        boolean isLikedByMemberId = postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
        PostDetailsResDto postDetailsResDto =
                PostDetailsResDto.from(post, post.isWriter(memberId), isLikedByMemberId, commentResDtos);
        postRepository.save(post);
        return postDetailsResDto;
    }

    public PostCreateResDto createPost(Long memberId, PostCreateReqDto request) {
        Member member = loadMemberOrThrow(memberId);
        Post post = Post.create(member, request.title(), request.content(), request.imageUrl());
        postRepository.save(post);
        return PostCreateResDto.from(post);
    }

    public PostUpdateResDto updatePost(Long memberId, Long postId, PostUpdateReqDto request){
        Member member = loadMemberOrThrow(memberId);
        Post post = loadPostOrThrow(postId);
        checkPostWriter(memberId, post);
        post.updatePost(request.title(), request.content(), request.imageUrl());
        postRepository.save(post);
        return PostUpdateResDto.from(post);
    }

    public void deletePost(Long memberId, Long postId) {
        Member member = loadMemberOrThrow(memberId);
        Post post = loadPostOrThrow(postId);
        checkPostWriter(memberId, post);
        post.delete();
        postRepository.save(post);
    }

    public PostLikeResDto postLike(Long memberId, Long postId) {
        Member member = loadMemberOrThrow(memberId);
        Post post = loadPostOrThrow(postId);
        checkAlreadyLiked(memberId, postId);
        PostLike postLike = PostLike.create(memberId, postId);
        long postLikeCount = post.increaseLikeCount();
        postLikeRepository.save(postLike);
        postRepository.save(post);
        return PostLikeResDto.from(postLikeCount, true);
    }

    public PostLikeResDto postUnLike(Long memberId, Long postId) {
        Member member = loadMemberOrThrow(memberId);
        Post post = loadPostOrThrow(postId);
        checkNotAlreadyLiked(memberId, postId);
        long postLikeCount = post.decreaseLikeCount();
        postLikeRepository.delete(postId, memberId);
        postRepository.save(post);
        return PostLikeResDto.from(postLikeCount, false);
    }

    public CommentCreateResDto createComment(Long memberId, Long postId, CommentCreateReqDto request) {
        Member member = loadMemberOrThrow(memberId);
        Post post = loadPostOrThrow(postId);
        Comment comment = Comment.create(postId, member, request.content());
        commentRepository.save(comment);
        return CommentCreateResDto.from(comment.getId());
    }

    public void updateComment(Long memberId, Long postId, Long commentId, CommentUpdateReqDto request) {
        loadMemberOrThrow(memberId);
        loadPostOrThrow(postId);
        Comment comment = loadCommentOrThrow(commentId);
        checkCommentWriter(memberId, comment);
        comment.updateContent(request.content());
        commentRepository.save(comment);
    }

    public void deleteComment(Long memberId, Long postId, Long commentId) {
        loadMemberOrThrow(memberId);
        loadPostOrThrow(postId);
        Comment comment = loadCommentOrThrow(commentId);
        checkCommentWriter(memberId, comment);
        comment.delete();
        commentRepository.save(comment);
    }

    // TODO : 자신이 작성한 글은 신고를 막아야 할까?
    public void reportPost(Long memberId, Long postId, PostReportReqDto request){
        Member member = loadMemberOrThrow(memberId);
        Post post = loadPostOrThrow(postId);

    }

    private Member loadMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(AuthErrorCode.MEMBER_NOT_FOUND));
    }

    private Post loadPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(PostErrorCode.POST_NOT_FOUND));
    }

    private Comment loadCommentOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    private void checkPostWriter(Long memberId, Post post) {
        if(!post.isWriter(memberId)){
            throw new UnauthorizedException(PostErrorCode.NOT_POST_WRITER);
        }
    }

    private void checkCommentWriter(Long memberId, Comment comment) {
        if (!comment.isWriter(memberId)) {
            throw new ForbiddenException(CommentErrorCode.NOT_COMMENT_WRITER);
        }
    }

    private void checkAlreadyLiked(Long memberId, Long postId) {
        if(postLikeRepository.existsByPostIdAndMemberId(postId, memberId)){
            throw new ConflictException(PostErrorCode.POST_ALREADY_LIKED);
        }
    }

    private void checkNotAlreadyLiked(Long memberId, Long postId) {
        if(!postLikeRepository.existsByPostIdAndMemberId(postId, memberId)){
            throw new ConflictException(PostErrorCode.POST_ALREADY_UNLIKED);
        }
    }
}
