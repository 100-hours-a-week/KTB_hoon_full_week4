package kakao.bootcamp.fullstack.api.repository.post.jpa;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPostLikeRepository extends JpaRepository<PostLike, Long> {

    @Query("SELECT pl FROM PostLike pl "
            + "WHERE pl.post.id = :postId AND pl.member.id = :memberId AND pl.deleted = false")
    Optional<PostLike> findActiveByPostIdAndMemberId(@Param("postId") Long postId, @Param("memberId") Long memberId);

    @Query("SELECT CASE WHEN COUNT(pl) > 0 THEN true ELSE false END FROM PostLike pl "
            + "WHERE pl.post.id = :postId AND pl.member.id = :memberId AND pl.deleted = false")
    boolean existsByPostIdAndMemberId(@Param("postId") Long postId, @Param("memberId") Long memberId);
}
