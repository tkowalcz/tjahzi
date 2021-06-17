package pl.tkowalcz.tjahzi;

import java.util.Map;

public class LabelSerializers {

    private static final ThreadLocal<LabelSerializer> THREAD_LOCAL = ThreadLocal.withInitial(LabelSerializer::new);

    public static LabelSerializer threadLocal() {
        LabelSerializer result = THREAD_LOCAL.get();
        result.clear();

        return result;
    }

    // @VisibleForTests
    public static LabelSerializer from(Map<String, String> labels, String... moreLabels) {
        LabelSerializer labelSerializer = threadLocal();

        labels.forEach((key, value) -> {
                    labelSerializer.appendLabelName(key);
                    labelSerializer.appendWholeLabelValue(value);
                }
        );

        for (int i = 0; i < moreLabels.length; i += 2) {
            labelSerializer.appendLabelName(moreLabels[i]);
            labelSerializer.appendWholeLabelValue(moreLabels[i + 1]);
        }

        return labelSerializer;
    }

    // @VisibleForTests
    public static LabelSerializer from(String... labels) {
        LabelSerializer labelSerializer = threadLocal();

        for (int i = 0; i < labels.length; i += 2) {
            labelSerializer.appendLabelName(labels[i]);
            labelSerializer.appendWholeLabelValue(labels[i + 1]);
        }

        return labelSerializer;
    }
}
