package kakao.bootcamp.fullstack.api.repository.edit_revision.inmemory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kakao.bootcamp.fullstack.api.domain.edit_revision.EditRevision;
import kakao.bootcamp.fullstack.api.repository.edit_revision.EditRevisionRepository;
import kakao.bootcamp.fullstack.global.generator.AtomicLongIdGenerator;
import kakao.bootcamp.fullstack.global.generator.IdGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class InMemoryEditRevisionRepository implements EditRevisionRepository {

    private final IdGenerator idGenerator = new AtomicLongIdGenerator();
    private final Map<Long, EditRevision> editRevisions = new ConcurrentHashMap<>();

    @Override
    public void save(EditRevision editRevision) {
        if (editRevision.isNew()) {
            Long id = idGenerator.nextId();
            editRevision.assignId(id);
        }
        editRevisions.put(editRevision.getId(), editRevision);
    }
}
