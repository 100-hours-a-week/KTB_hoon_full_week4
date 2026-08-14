package kakao.bootcamp.fullstack.global.utils;

public class ExpiryTimeCalculator {

    private static final long MILLIS_PER_SECOND = 1000L;

    public static long afterSeconds(long seconds) {
        return System.currentTimeMillis() + seconds * MILLIS_PER_SECOND;
    }
}
