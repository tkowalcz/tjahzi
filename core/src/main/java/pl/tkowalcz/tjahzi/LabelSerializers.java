package pl.tkowalcz.tjahzi;

public class LabelSerializers {

    private static final ThreadLocal<LabelSerializer> THREAD_LOCAL = ThreadLocal.withInitial(LabelSerializer::new);

    public static LabelSerializer threadLocal() {
        LabelSerializer result = THREAD_LOCAL.get();
        result.clear();

        return result;
    }
}
