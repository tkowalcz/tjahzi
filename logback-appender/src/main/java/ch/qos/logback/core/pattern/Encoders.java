package ch.qos.logback.core.pattern;

public class Encoders {

    private static final ThreadLocal<Encoder> THREAD_LOCAL = ThreadLocal.withInitial(Encoder::new);

    public static Encoder threadLocal() {
        Encoder result = THREAD_LOCAL.get();
        result.clear();

        return result;
    }
}
