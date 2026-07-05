package kakao.bootcamp.fullstack.api.service;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.comment.Comment;
import kakao.bootcamp.fullstack.api.domain.comment.CommentErrorCode;
import kakao.bootcamp.fullstack.api.domain.edit_revision.EditRevision;
import kakao.bootcamp.fullstack.api.domain.member.MemberErrorCode;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostErrorCode;
import kakao.bootcamp.fullstack.api.domain.post.PostLike;
import kakao.bootcamp.fullstack.api.domain.post.PostViewLog;
import kakao.bootcamp.fullstack.api.dto.request.CommentCreateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.CommentUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.PostCreateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.PostUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.response.CommentCreateResDto;
import kakao.bootcamp.fullstack.api.dto.response.CommentResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostCreateResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostDetailsResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostLikeResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostSummaryPageResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostSummaryResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostUpdateResDto;
import kakao.bootcamp.fullstack.api.repository.comment.CommentRepository;
import kakao.bootcamp.fullstack.api.repository.edit_revision.EditRevisionRepository;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
import kakao.bootcamp.fullstack.api.repository.post.PostLikeRepository;
import kakao.bootcamp.fullstack.api.repository.post.PostRepository;
import kakao.bootcamp.fullstack.api.repository.post.PostViewLogRepository;
import kakao.bootcamp.fullstack.global.exception.ConflictException;
import kakao.bootcamp.fullstack.global.exception.ForbiddenException;
import kakao.bootcamp.fullstack.global.exception.NotFoundException;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final PostLikeRepository postLikeRepository;
    private final EditRevisionRepository editRevisionRepository;
    private final PostViewLogRepository postViewLogRepository;

    public PostSummaryPageResDto getPostSummariesList(Long memberId, Long cursor, Long size) {
        Member member = loadMemberOrThrow(memberId);
        List<Post> posts = postRepository.findPage(cursor, size + 1);
        boolean hasNext = posts.size() > size;
        List<Post> page = hasNext ? posts.subList(0, size.intValue()) : posts;
        List<PostSummaryResDto> summaries = page.stream()
                .map(PostSummaryResDto::from)
                .toList();
        Long nextCursor = hasNext ? page.get(page.size() - 1).getId() : null;
        return new PostSummaryPageResDto(summaries, nextCursor, hasNext);
    }

    @Transactional
    public PostDetailsResDto getPostDetails(Long memberId, Long postId) {
        Member member = loadMemberOrThrow(memberId);
        Post post = loadPostOrThrow(postId);
        PostViewLog viewLog = loadPostViewLogOrCreate(memberId, postId);
        if (viewLog.isNew() || viewLog.canCountAsNewView()) {
            post.increaseViewCount();
            if (!viewLog.isNew()) {
                viewLog.refreshViewedAt();
            }
            postViewLogRepository.save(viewLog);
        }
        List<CommentResDto> commentResDtos = commentRepository
                .findByPostId(postId)
                .stream()
                .map(comment ->
                        CommentResDto.from(comment, comment.isWriter(memberId)))
                .toList();
        boolean isLikedByMemberId = postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
        return PostDetailsResDto.from(post, post.isWriter(memberId), isLikedByMemberId, commentResDtos);
    }

    @Transactional
    public PostCreateResDto createPost(Long memberId, PostCreateReqDto request) {
        Member member = loadMemberOrThrow(memberId);
        Post post = Post.create(member, request.title(), request.content(), request.imageUrl());
        postRepository.save(post);
        return PostCreateResDto.from(post);
    }

    @Transactional
    public PostUpdateResDto updatePost(Long memberId, Long postId, PostUpdateReqDto request){
        Member member = loadMemberOrThrow(memberId);
        Post post = loadPostOrThrow(postId);
        checkPostWriter(memberId, post);
        editRevisionRepository.save(EditRevision.fromPost(post));
        post.updatePost(request.title(), request.content(), request.imageUrl());
        return PostUpdateResDto.from(post);
    }

    @Transactional
    public void deletePost(Long memberId, Long postId) {
        Member member = loadMemberOrThrow(memberId);
        Post post = loadPostOrThrow(postId);
        checkPostWriter(memberId, post);
        post.delete();
    }

    @Transactional
    public PostLikeResDto postLike(Long memberId, Long postId) {
        Member member = loadMemberOrThrow(memberId);
        Post post = loadPostOrThrow(postId);
        checkAlreadyLiked(memberId, postId);
        PostLike postLike = PostLike.create(post, member);
        long postLikeCount = post.increaseLikeCount();
        postLikeRepository.save(postLike);
        return PostLikeResDto.from(postLikeCount, true);
    }

    @Transactional
    public PostLikeResDto postUnLike(Long memberId, Long postId) {
        Member member = loadMemberOrThrow(memberId);
        Post post = loadPostOrThrow(postId);
        PostLike postLike = loadActivePostLikeOrThrow(postId, memberId);
        long postLikeCount = post.decreaseLikeCount();
        postLike.delete();
        return PostLikeResDto.from(postLikeCount, false);
    }

    @Transactional
    public CommentCreateResDto createComment(Long memberId, Long postId, CommentCreateReqDto request) {
        Member member = loadMemberOrThrow(memberId);
        Post post = loadPostOrThrow(postId);
        Comment comment = Comment.create(post, member, request.content());
        post.increaseCommentCount();
        commentRepository.save(comment);
        return CommentCreateResDto.from(comment.getId());
    }

    @Transactional
    public void updateComment(Long memberId, Long postId, Long commentId, CommentUpdateReqDto request) {
        loadMemberOrThrow(memberId);
        loadPostOrThrow(postId);
        Comment comment = loadCommentOrThrow(commentId);
        checkCommentWriter(memberId, comment);
        editRevisionRepository.save(EditRevision.fromComment(comment));
        comment.updateContent(request.content());
    }

    @Transactional
    public void deleteComment(Long memberId, Long postId, Long commentId) {
        loadMemberOrThrow(memberId);
        Post post = loadPostOrThrow(postId);
        Comment comment = loadCommentOrThrow(commentId);
        checkCommentWriter(memberId, comment);
        comment.delete();
        post.decreaseCommentCount();
    }

    private Member loadMemberOrThrow(Long memberId) {
        return memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new UnauthorizedException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private Post loadPostOrThrow(Long postId) {
        return postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException(PostErrorCode.POST_NOT_FOUND));
    }

    private Comment loadCommentOrThrow(Long commentId) {
        return commentRepository.findActiveById(commentId)
                .orElseThrow(() -> new NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    private void checkPostWriter(Long memberId, Post post) {
        if(!post.isWriter(memberId)){
            throw new ForbiddenException(PostErrorCode.NOT_POST_WRITER);
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

    private PostLike loadActivePostLikeOrThrow(Long postId, Long memberId) {
        return postLikeRepository.findActiveByPostIdAndMemberId(postId, memberId)
                .orElseThrow(() -> new ConflictException(PostErrorCode.POST_ALREADY_UNLIKED));
    }

    private PostViewLog loadPostViewLogOrCreate(Long memberId, Long postId) {
        return postViewLogRepository.findByPostIdAndMemberId(postId, memberId)
                .orElseGet(() -> PostViewLog.create(postId, memberId));
    }
}
