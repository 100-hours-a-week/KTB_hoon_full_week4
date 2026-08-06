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

    @Query(
            "SELECT p FROM Post p JOIN FETCH p.member"
                    + " WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))"
                    + " OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))"
                    + " AND (:category IS NULL OR p.category = :category)"
                    + " AND (:meetingType IS NULL OR p.meetingType = :meetingType)"
                    + " AND (:recruitStatus IS NULL OR p.recruitStatus = :recruitStatus)"
                    + " AND (:sido IS NULL OR p.address.sido = :sido)"
                    + " AND (:sigungu IS NULL OR p.address.sigungu = :sigungu)"
                    + " AND (:createdFrom IS NULL OR p.createdAt >= :createdFrom)"
                    + " AND (:createdTo IS NULL OR p.createdAt < :createdTo)"
                    + " AND (:cursor IS NULL OR p.id < :cursor)"
                    + " AND p.deleted = false AND p.blinded = false"
                    + " ORDER BY p.id DESC")
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
}
