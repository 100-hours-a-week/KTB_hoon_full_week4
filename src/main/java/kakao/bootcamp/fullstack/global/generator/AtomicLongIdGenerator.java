package kakao.bootcamp.fullstack.global.generator;

import java.util.concurrent.atomic.AtomicLong;


public class AtomicLongIdGenerator implements IdGenerator {

    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public Long nextId() {
        return sequence.incrementAndGet();
    }
}
