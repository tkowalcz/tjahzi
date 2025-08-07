package pl.tkowalcz.tjahzi;

public class LabelSerializerPair {

    private final LabelSerializer first;
    private final LabelSerializer second;

    LabelSerializerPair() {
        this.first = new LabelSerializer();
        this.second = new LabelSerializer();
    }

    public LabelSerializer getFirst() {
        return first;
    }

    public LabelSerializer getSecond() {
        return second;
    }

    void clear() {
        first.clear();
        second.clear();
    }
}
