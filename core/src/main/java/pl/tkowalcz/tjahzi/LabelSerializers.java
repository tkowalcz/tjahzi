package pl.tkowalcz.tjahzi;

public class LabelSerializers {

    private static final ThreadLocal<LabelSerializerPair> THREAD_LOCAL = ThreadLocal.withInitial(LabelSerializerPair::new);

    public static LabelSerializerPair threadLocal() {
        LabelSerializerPair result = THREAD_LOCAL.get();
        result.clear();

        return result;
    }
}
