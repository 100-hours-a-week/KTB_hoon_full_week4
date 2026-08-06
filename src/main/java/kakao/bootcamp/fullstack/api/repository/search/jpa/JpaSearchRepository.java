package kakao.bootcamp.fullstack.api.repository.search.jpa;

import java.time.LocalDateTime;
import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post.MeetingType;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostCategory;
import kakao.bootcamp.fullstack.api.domain.post.RecruitStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaSearchRepository extends JpaRepository<Post, Long> {

    String FILTERS =
            " AND (:category IS NULL OR p.category = :category)"
                    + " AND (:meetingType IS NULL OR p.meetingType = :meetingType)"
                    + " AND (:recruitStatus IS NULL OR p.recruitStatus = :recruitStatus)"
                    + " AND (:sido IS NULL OR p.address.sido = :sido)"
                    + " AND (:sigungu IS NULL OR p.address.sigungu = :sigungu)"
                    + " AND (:createdFrom IS NULL OR p.createdAt >= :createdFrom)"
                    + " AND (:createdTo IS NULL OR p.createdAt < :createdTo)"
                    + " AND (:cursor IS NULL OR p.id < :cursor)"
                    + " AND p.deleted = false AND p.blinded = false"
                    + " ORDER BY p.id DESC";

    // 키워드가 있을 때만 LIKE 를 태운다. 선행 와일드카드라 인덱스를 쓸 수 없으므로,
    // 키워드가 없는 요청까지 이 쿼리로 보내면 불필요한 스캔 비용을 지불하게 된다.
    @Query(
            "SELECT p FROM Post p JOIN FETCH p.member"
                    + " WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))"
                    + " OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))"
                    + FILTERS)
    List<Post> searchActivePostPage(
            @Param("keyword") String keyword,
            @Param("category") PostCategory category,
            @Param("meetingType") MeetingType meetingType,
            @Param("recruitStatus") RecruitStatus recruitStatus,
            @Param("sido") String sido,
            @Param("sigungu") String sigungu,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            @Param("cursor") Long cursor,
            Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.member WHERE 1 = 1" + FILTERS)
    List<Post> findActivePostPage(
            @Param("category") PostCategory category,
            @Param("meetingType") MeetingType meetingType,
            @Param("recruitStatus") RecruitStatus recruitStatus,
            @Param("sido") String sido,
            @Param("sigungu") String sigungu,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            @Param("cursor") Long cursor,
            Pageable pageable);
}
