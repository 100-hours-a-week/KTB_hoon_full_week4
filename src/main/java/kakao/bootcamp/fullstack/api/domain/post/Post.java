package kakao.bootcamp.fullstack.api.domain.post;

import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.global.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    private Long id;
    private String title;
    private String content;
    private String imageUrl;
    private Member writer;
    private long likeCount = 0L;
    private long commentCount = 0L;
    private long viewCount = 0L;
    private long reportCount = 0L;
    private boolean edited = false;
    private boolean blinded = false;

    private Post(Member writer, String title, String content, String imageUrl) {
        this.writer = writer;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    public boolean isNew() {
        return id == null;
    }

    public void assignId(Long id) {
        if (!isNew()) {
            throw new IllegalStateException("이미 ID가 할당된 게시글입니다.");
        }
        this.id = id;
    }

    public void increaseViewCount(){
        this.viewCount++;
    }

    public long increaseLikeCount(){
        this.likeCount++;
        return likeCount;
    }

    public long decreaseLikeCount(){
        this.likeCount--;
        return likeCount;
    }

    public long increaseReportCount(){
        this.reportCount++;
        return reportCount;
    }

    public void updatePost(String title, String content, String imageUrl){
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        if(!edited) {
            this.edited = true;
        }
    }

    public boolean isWriter(Long memberId){
        return writer.getId().equals(memberId);
    }

    public static Post create(Member writer, String title, String content, String imageUrl){
        return new Post(writer, title, content, imageUrl);
    }
}
