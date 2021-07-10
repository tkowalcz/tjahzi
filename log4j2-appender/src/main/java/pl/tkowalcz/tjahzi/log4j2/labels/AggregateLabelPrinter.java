package pl.tkowalcz.tjahzi.log4j2.labels;

import org.apache.logging.log4j.core.LogEvent;

import java.util.List;
import java.util.function.Consumer;

public class AggregateLabelPrinter implements LabelPrinter {

    private final LabelPrinter[] nodes;

    public AggregateLabelPrinter(LabelPrinter... nodes) {
        this.nodes = nodes;
    }

    public static LabelPrinter of(List<LabelPrinter> result) {
        return new AggregateLabelPrinter(result.toArray(new LabelPrinter[0]));
    }

    @Override
    public void append(LogEvent event, Consumer<CharSequence> appendable) {
        for (LabelPrinter node : nodes) {
            node.append(event, appendable);
        }
    }
}
