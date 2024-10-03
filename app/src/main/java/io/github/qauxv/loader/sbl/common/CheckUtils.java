package io.github.qauxv.loader.sbl.common;

public class CheckUtils {

    private CheckUtils() {
        throw new UnsupportedOperationException("No instances");
    }

    public static void checkNonNull(Object obj, String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
    }

}
