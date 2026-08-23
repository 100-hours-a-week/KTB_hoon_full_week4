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
                    + " AND (:cursorCreatedAt IS NULL"
                    + " OR p.createdAt < :cursorCreatedAt"
                    + " OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId))"
                    + " AND p.deleted = false AND p.blinded = false"
                    + " ORDER BY p.createdAt DESC, p.id DESC";

    String NATIVE_FILTERS =
            " AND (:category IS NULL OR p.category = :category)"
                    + " AND (:meetingType IS NULL OR p.meeting_type = :meetingType)"
                    + " AND (:recruitStatus IS NULL OR p.recruit_status = :recruitStatus)"
                    + " AND (:sido IS NULL OR p.sido = :sido)"
                    + " AND (:sigungu IS NULL OR p.sigungu = :sigungu)"
                    + " AND (:createdFrom IS NULL OR p.created_at >= :createdFrom)"
                    + " AND (:createdTo IS NULL OR p.created_at < :createdTo)"
                    + " AND (:cursorCreatedAt IS NULL"
                    + " OR p.created_at < :cursorCreatedAt"
                    + " OR (p.created_at = :cursorCreatedAt AND p.id < :cursorId))"
                    + " AND p.deleted = false AND p.blinded = false"
                    + " ORDER BY p.created_at DESC, p.id DESC";

    @Query(
            value =
                    "SELECT p.* FROM posts p"
                            + " WHERE MATCH(p.title, p.content) AGAINST(CONCAT('\"', :keyword, '\"') IN BOOLEAN MODE)"
                            + NATIVE_FILTERS,
            nativeQuery = true)
    List<Post> searchActivePostPage(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("meetingType") String meetingType,
            @Param("recruitStatus") String recruitStatus,
            @Param("sido") String sido,
            @Param("sigungu") String sigungu,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query(
            "SELECT p FROM Post p JOIN FETCH p.member"
                    + " WHERE p.id IN :ids AND p.deleted = false AND p.blinded = false")
    List<Post> findActiveWithMemberByIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT p FROM Post p JOIN FETCH p.member WHERE 1 = 1" + FILTERS)
    List<Post> findActivePostPage(
            @Param("category") PostCategory category,
            @Param("meetingType") MeetingType meetingType,
            @Param("recruitStatus") RecruitStatus recruitStatus,
            @Param("sido") String sido,
            @Param("sigungu") String sigungu,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);
}
