package kakao.bootcamp.fullstack.api.repository.edit_revision.jpa;

import kakao.bootcamp.fullstack.api.domain.edit_revision.EditRevision;
import kakao.bootcamp.fullstack.api.repository.edit_revision.EditRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("prod")
@RequiredArgsConstructor
public class JpaEditRevisionRepositoryAdapter implements EditRevisionRepository {

    private final JpaEditRevisionRepository jpaEditRevisionRepository;

    @Override
    public void save(EditRevision editRevision) {
        jpaEditRevisionRepository.save(editRevision);
    }
}
