package pl.tkowalcz.tjahzi.log4j2.labels;

class LabelTest extends LabelBaseTest {

    @Override
    public LabelBase createCUT(String name, String value, String pattern) {
        return Label.createLabel(name, value, pattern);
    }
}
