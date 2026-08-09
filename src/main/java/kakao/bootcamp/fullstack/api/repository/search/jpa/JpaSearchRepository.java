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

    // 키워드가 있을 때만 FULLTEXT 를 태운다. 키워드가 없는 요청까지 이 쿼리로 보내면
    // 불필요한 매치 비용을 지불하게 된다(findActivePostPage 와 동일한 이유로 분리 유지).
    // FULLTEXT(title, content) WITH PARSER ngram 인덱스 필요(bench/sql/06_fulltext_index.sql).
    // BOOLEAN MODE + 따옴표 구문(phrase search)으로 LIKE '%kw%' 의 부분 문자열 매칭에 근접시킨다
    // (NATURAL LANGUAGE MODE 는 50% 이상 매치되는 토큰을 스톱워드로 취급해 흔한 키워드가
    // 조용히 빠질 위험이 있어 배제했다). 네이티브 쿼리라 JPQL 처럼 enum 매핑 타입을 못 읽으므로
    // category/meetingType/recruitStatus 는 어댑터에서 문자열로 변환해 넘긴다. Pageable 은
    // Sort 없는 채로 유지할 것 — 네이티브 쿼리는 JPQL 과 달리 임의의 SQL 에 안전하게 ORDER BY 를
    // 주입할 수 없다.
    String NATIVE_FILTERS =
            " AND (:category IS NULL OR p.category = :category)"
                    + " AND (:meetingType IS NULL OR p.meeting_type = :meetingType)"
                    + " AND (:recruitStatus IS NULL OR p.recruit_status = :recruitStatus)"
                    + " AND (:sido IS NULL OR p.sido = :sido)"
                    + " AND (:sigungu IS NULL OR p.sigungu = :sigungu)"
                    + " AND (:createdFrom IS NULL OR p.created_at >= :createdFrom)"
                    + " AND (:createdTo IS NULL OR p.created_at < :createdTo)"
                    + " AND (:cursor IS NULL OR p.id < :cursor)"
                    + " AND p.deleted = false AND p.blinded = false"
                    + " ORDER BY p.id DESC";

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
