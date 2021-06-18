package pl.tkowalcz.tjahzi;

import java.util.Map;

class LabelSerializerCreator {

    public static LabelSerializer from(Map<String, String> labels, String... moreLabels) {
        LabelSerializer labelSerializer = LabelSerializers.threadLocal();

        labels.forEach(labelSerializer::appendLabel);
        for (int i = 0; i < moreLabels.length; i += 2) {
            labelSerializer.appendLabel(moreLabels[i], moreLabels[i + 1]);
        }

        return labelSerializer;
    }

    public static LabelSerializer from(String... labels) {
        LabelSerializer labelSerializer = LabelSerializers.threadLocal();

        for (int i = 0; i < labels.length; i += 2) {
            labelSerializer.appendLabel(labels[i], labels[i + 1]);
        }

        return labelSerializer;
    }
}
