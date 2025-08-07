package pl.tkowalcz.tjahzi.log4j2.labels;

import org.agrona.DirectBuffer;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import pl.tkowalcz.tjahzi.LabelSerializer;

import java.util.HashMap;
import java.util.Map;

public class LabelSerializerAssert extends AbstractAssert<LabelSerializerAssert, LabelSerializer> {

    public LabelSerializerAssert(LabelSerializer labelSerializer, Class<?> selfType) {
        super(labelSerializer, selfType);
    }

    public static LabelSerializerAssert assertThat(LabelSerializer labelSerializer) {
        return new LabelSerializerAssert(labelSerializer, LabelSerializerAssert.class);
    }

    public LabelSerializerAssert hasLabelCount(int expected) {
        Assertions.assertThat(actual.getLabelsCount()).isEqualTo(expected);
        return this;
    }

    public LabelSerializerAssert contains(Map<String, String> expected) {
        Map<String, String> labels = readLabels(actual.getBuffer(), actual.getLabelsCount());
        Assertions.assertThat(labels).containsAllEntriesOf(expected);

        return this;
    }

    private static Map<String, String> readLabels(DirectBuffer buffer, int labelsCount) {
        int index = 0;
        Map<String, String> result = new HashMap<>();

        for (int i = 0; i < labelsCount; i++) {
            String name = buffer.getStringAscii(index);
            index += name.length() + Integer.BYTES;

            String value = buffer.getStringAscii(index);
            index += value.length() + Integer.BYTES;

            result.put(name, value);
        }

        return result;
    }

    public LabelSerializerAssert contains(String name, String value) {
        Map<String, String> labels = readLabels(actual.getBuffer(), actual.getLabelsCount());
        Assertions.assertThat(labels).contains(Map.entry(name, value));

        return this;
    }
}
