package pl.tkowalcz.tjahzi.http;

import io.netty.util.concurrent.FastThreadLocal;
import pl.tkowalcz.tjahzi.javolution.text.TextBuilder;

public class TextBuilders {

    private static final FastThreadLocal<TextBuilder> THREAD_LOCAL = new FastThreadLocal<TextBuilder>() {

        @Override
        protected TextBuilder initialValue() {
            return new TextBuilder();
        }
    };

    public static TextBuilder threadLocal() {
        TextBuilder result = THREAD_LOCAL.get();
        result.clear();

        return result;
    }
}
