package kakao.bootcamp.fullstack.global;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public abstract class BaseEntity {

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private boolean deleted;

    protected BaseEntity() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.deleted = false;
    }

    protected void updateModifiedTime() {
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        updateModifiedTime();
    }

    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        updateModifiedTime();
    }
}