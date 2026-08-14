package kakao.bootcamp.fullstack.global.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class ExpiryTimeCalculator {

    private static final long MILLIS_PER_SECOND = 1000L;

    public static long afterSeconds(long seconds) {
        return System.currentTimeMillis() + seconds * MILLIS_PER_SECOND;
    }

    public static long of(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
