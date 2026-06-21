package kakao.bootcamp.fullstack.api.repository.edit_revision;

import kakao.bootcamp.fullstack.api.domain.edit_revision.EditRevision;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEditRevisionRepository extends JpaRepository<EditRevision, Long> {
}
