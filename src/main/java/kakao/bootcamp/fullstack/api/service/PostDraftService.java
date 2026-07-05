package kakao.bootcamp.fullstack.api.service;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraft;
import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraftErrorCode;
import kakao.bootcamp.fullstack.api.dto.request.PostCreateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.PostDraftCreateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.PostDraftUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.response.PostCreateResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostDraftDetailsResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostDraftSaveResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostDraftsSummaryResDto;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
import kakao.bootcamp.fullstack.api.repository.post.PostRepository;
import kakao.bootcamp.fullstack.api.repository.post_draft.PostDraftRepository;
import kakao.bootcamp.fullstack.global.exception.ForbiddenException;
import kakao.bootcamp.fullstack.global.exception.NotFoundException;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostDraftService {

    private final PostDraftRepository postDraftRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    public List<PostDraftsSummaryResDto> getPostDrafts(Long memberId) {
        Member member = loadMemberOrThrow(memberId);
        List<PostDraft> postDrafts = postDraftRepository.getPostDraftsByMemberId(memberId);
        return PostDraftsSummaryResDto.fromDomains(postDrafts);
    }

    public PostDraftDetailsResDto getPostDraft(Long memberId, Long postDraftId) {
        Member member = loadMemberOrThrow(memberId);
        PostDraft postDraft = loadPostDraftOrThrow(postDraftId);
        checkPostDraftWriter(memberId, postDraft);
        return PostDraftDetailsResDto.from(postDraft);
    }

    @Transactional
    public PostDraftSaveResDto createPostDraft(Long memberId, PostDraftCreateReqDto request) {
        Member member = loadMemberOrThrow(memberId);
        PostDraft postDraft = PostDraft.create(member, request.title(), request.content(), request.imageUrl());
        postDraftRepository.save(postDraft);
        return PostDraftSaveResDto.from(postDraft);
    }

    @Transactional
    public PostDraftSaveResDto updatePostDraft(Long memberId, Long postDraftId, PostDraftUpdateReqDto request) {
        Member member = loadMemberOrThrow(memberId);
        PostDraft postDraft = loadPostDraftOrThrow(postDraftId);
        checkPostDraftWriter(memberId, postDraft);
        postDraft.update(request.title(), request.content(), request.imageUrl());
        return PostDraftSaveResDto.from(postDraft);
    }

    @Transactional
    public PostCreateResDto publishPostDraft(Long memberId, Long postDraftId, PostCreateReqDto request) {
        Member member = loadMemberOrThrow(memberId);
        PostDraft postDraft = loadPostDraftOrThrow(postDraftId);
        checkPostDraftWriter(memberId, postDraft);
        Post post = Post.create(member, request.title(), request.content(), request.imageUrl());
        postRepository.save(post);
        postDraft.publish();
        return PostCreateResDto.from(post);
    }

    @Transactional
    public void deletePostDraft(Long memberId, Long postDraftId) {
        Member member = loadMemberOrThrow(memberId);
        PostDraft postDraft = loadPostDraftOrThrow(postDraftId);
        checkPostDraftWriter(memberId, postDraft);
        postDraft.delete();
    }

    private static void checkPostDraftWriter(Long memberId, PostDraft postDraft) {
        if(!postDraft.isWriter(memberId)){
            throw new ForbiddenException(PostDraftErrorCode.NOT_POST_DRAFT_WRITER);
        }
    }

    private PostDraft loadPostDraftOrThrow(Long postDraftId) {
        return postDraftRepository.findActiveById(postDraftId)
                .orElseThrow(() -> new NotFoundException(PostDraftErrorCode.POST_DRAFT_NOT_FOUND));
    }

    private Member loadMemberOrThrow(Long memberId) {
        return memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCode.MEMBER_NOT_FOUND));
    }
}
