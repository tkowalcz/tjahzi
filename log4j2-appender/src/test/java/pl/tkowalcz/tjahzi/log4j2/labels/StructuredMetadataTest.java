package pl.tkowalcz.tjahzi.log4j2.labels;

class StructuredMetadataTest extends LabelBaseTest {

    @Override
    public LabelBase createCUT(String name, String value, String pattern) {
        return StructuredMetadata.createLabel(name, value, pattern);
    }
}
