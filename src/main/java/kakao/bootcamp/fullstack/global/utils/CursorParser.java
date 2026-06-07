package kakao.bootcamp.fullstack.global.utils;

import java.time.LocalDateTime;

public class CursorParser {

    public static LocalDateTime parseCreatedAt(String cursor) {
        return LocalDateTime.parse(cursor.split("_")[0]);
    }

    public static Long parsePostId(String cursor) {
        return Long.valueOf(cursor.split("_")[1]);
    }
}
