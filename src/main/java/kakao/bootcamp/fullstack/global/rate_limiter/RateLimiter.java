package kakao.bootcamp.fullstack.global.rate_limiter;

public interface RateLimiter {
    boolean tryAcquire(Long memberId);
}
