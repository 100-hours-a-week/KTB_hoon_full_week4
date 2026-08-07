package kakao.bootcamp.fullstack.edit_revision.fake;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import kakao.bootcamp.fullstack.api.domain.edit_revision.EditRevision;
import kakao.bootcamp.fullstack.api.repository.edit_revision.EditRevisionRepository;

public class FakeEditRevisionRepository implements EditRevisionRepository {

    private final List<EditRevision> store = new CopyOnWriteArrayList<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public void save(EditRevision editRevision) {
        if (editRevision.isNew()) {
            editRevision.assignId(sequence.incrementAndGet());
        }
        store.add(editRevision);
    }

    public List<EditRevision> findAll() {
        return List.copyOf(store);
    }

    public void clear() {
        store.clear();
        sequence.set(0);
    }
}
