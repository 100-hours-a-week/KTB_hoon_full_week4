package kakao.bootcamp.fullstack.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.post.Comment;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostErrorCode;
import kakao.bootcamp.fullstack.api.domain.post.PostLike;
import kakao.bootcamp.fullstack.api.dto.request.PostCreateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.PostUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.response.CommentResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostDetailsResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostLikeResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostSummaryResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostUpdateResDto;
import kakao.bootcamp.fullstack.api.repository.CommentRepository;
import kakao.bootcamp.fullstack.api.repository.MemberRepository;
import kakao.bootcamp.fullstack.api.repository.PostLikeRepository;
import kakao.bootcamp.fullstack.api.repository.PostRepository;
import kakao.bootcamp.fullstack.global.exception.ConflictException;
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

    // TODO : 삭제된 게시물은 조회대상에서 제외해야 함
    public List<PostSummaryResDto> getPostSummariesList(Long memberId, String cursor, Long size){
        Member member = loadMemberOrThrow(memberId);
        if (cursor == null) {
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
                        CommentResDto.from(comment, Objects.equals(comment.getWriterId(), memberId)))
                .toList();
        boolean isLikedByMemberId = postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
        PostDetailsResDto postDetailsResDto =
                PostDetailsResDto.from(post, Objects.equals(post.getWriterId(), memberId), isLikedByMemberId, commentResDtos);
        postRepository.save(post);
        return postDetailsResDto;
    }

    public void createPost(Long memberId, PostCreateReqDto request) {
        Member member = loadMemberOrThrow(memberId);
        Post post = Post.create(memberId, request.title(), request.content(), request.imageUrl(), member.getNickname());
        postRepository.save(post);
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

    private Member loadMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(AuthErrorCode.MEMBER_NOT_FOUND));
    }

    private Post loadPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(PostErrorCode.POST_NOT_FOUND));
    }

    private void checkPostWriter(Long memberId, Post post) {
        if(!post.isWriter(memberId)){
            throw new UnauthorizedException(PostErrorCode.NOT_POST_WRITER);
        }
    }

    private void checkAlreadyLiked(Long memberId, Long postId) {
        if(postLikeRepository.existsByPostIdAndMemberId(postId, memberId)){
            throw new ConflictException(PostErrorCode.POST_ALREADY_LIKED);
        }
    }

    private void checkNotAlreadyLiked(Long memberId, Long postId) {
        if(!postLikeRepository.existsByPostIdAndMemberId(postId, memberId)){
            throw new ConflictException(PostErrorCode.POST_ALREADY_LIKED);
        }
    }
}
