package ch.qos.logback.core.pattern;

public class StringBuilders {

    private static final ThreadLocal<StringBuilder> THREAD_LOCAL = ThreadLocal.withInitial(StringBuilder::new);

    public static StringBuilder threadLocal() {
        StringBuilder result = THREAD_LOCAL.get();
        result.setLength(0);

        return result;
    }
}
