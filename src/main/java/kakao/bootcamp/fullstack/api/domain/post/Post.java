package kakao.bootcamp.fullstack.api.domain.post;

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
    private Long writerId;
    private String writerNickname;
    private long likeCount = 0L;
    private long commentCount = 0L;
    private long viewCount = 0L;
    private long reportCount = 0L;
    private boolean edited = false;
    private boolean blinded = false;

    private Post(Long writerId, String title, String content, String imageUrl,
            String writerNickname) {
        this.writerId = writerId;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.writerNickname = writerNickname;
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

    public void updatePost(String title, String content, String imageUrl){
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        if(!edited) {
            this.edited = true;
        }
    }

    public boolean isWriter(Long memberId){
        return writerId.equals(memberId);
    }

    public static Post create(Long writerId, String title, String content, String imageUrl, String writerNickname){
        return new Post(writerId, title, content, imageUrl, writerNickname);
    }
}