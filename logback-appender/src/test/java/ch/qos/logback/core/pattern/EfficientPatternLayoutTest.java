package ch.qos.logback.core.pattern;

import ch.qos.logback.classic.ClassicTestConstants;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.testUtil.SampleConverter;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.pattern.parser.AbstractPatternLayoutBaseTest;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.testUtil.StringListAppender;
import ch.qos.logback.core.util.OptionHelper;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import static ch.qos.logback.classic.ClassicTestConstants.ISO_REGEX;
import static ch.qos.logback.classic.ClassicTestConstants.MAIN_REGEX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Adapted from ch.qos.logback.classic.PatternLayoutTest
 */
public class EfficientPatternLayoutTest extends AbstractPatternLayoutBaseTest<ILoggingEvent> {

    private EfficientPatternLayout patternLayout;

    private final LoggerContext loggerContext = new LoggerContext();
    private final Logger logger = loggerContext.getLogger(EfficientPatternLayoutTest.class);
    private final Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

    @Before
    public void setUp() {
        patternLayout = getPatternLayoutBase();
        patternLayout.setContext(loggerContext);
    }

    @Override
    public EfficientPatternLayout getPatternLayoutBase() {
        return new EfficientPatternLayout();
    }

    @Override
    public ILoggingEvent getEventObject() {
        return new LoggingEvent(
                ch.qos.logback.core.pattern.FormattingConverter.class.getName(),
                logger,
                Level.INFO,
                "Some message",
                null,
                null
        );
    }

    @Override
    public Context getContext() {
        return loggerContext;
    }

    @Test
    public void testOK() {
        // Given
        String pattern = "%d %le [%t] %lo{30} - %m%n";

        // When
        patternLayout.setPattern(pattern);
        patternLayout.start();
        String val = patternLayout.doLayout(getEventObject());

        // Then
        // 2021-02-01 22:38:06,212 INFO [main] c.q.l.pattern.EfficientPatternLayoutTest - Some message
        String regex = ISO_REGEX + " INFO " + MAIN_REGEX + " c.q.l.c.p.EfficientPatternLayoutTest - Some message\\s*";
        assertThat(val).matches(regex);
    }

    @Test
    public void testNoExeptionHandler() {
        // Given
        String pattern = "%m%n";
        ILoggingEvent loggingEvent = new LoggingEvent(
                ch.qos.logback.core.pattern.FormattingConverter.class.getName(),
                logger,
                Level.INFO,
                "Some message",
                new Exception("Bogus exception"),
                null
        );

        // When
        patternLayout.setPattern(pattern);
        patternLayout.start();
        String val = patternLayout.doLayout(loggingEvent);

        // Then
        assertThat(val).contains("java.lang.Exception: Bogus exception");
    }

    @Test
    public void testCompositePattern() {
        // Given
        String pattern = "%-69(%d %lo{20}) - %m%n";

        // When
        patternLayout.setPattern(pattern);
        patternLayout.start();
        String val = patternLayout.doLayout(getEventObject());

        // Then
        //                                                          / Ten spaces \
        // 2021-03-18 21:55:54,250 c.q.l.c.p.EfficientPatternLayoutTest         - Some message
        String regex = ISO_REGEX + " c.q.l.c.p.EfficientPatternLayoutTest {10}- Some message\\s*";
        assertThat(val).matches(regex);
    }

    @Test
    public void contextProperty() {
        // Given
        String pattern = "%property{a}";
        loggerContext.putProperty("a", "b");

        // When
        patternLayout.setPattern(pattern);
        patternLayout.start();
        String val = patternLayout.doLayout(getEventObject());

        // Then
        assertThat(val).isEqualTo("b");
    }

    @Test
    public void testNopExceptionHandler() {
        // Given
        ILoggingEvent loggingEvent = new LoggingEvent(
                ch.qos.logback.core.pattern.FormattingConverter.class.getName(),
                logger,
                Level.INFO,
                "Some message",
                new Exception("Bogus exception"),
                null
        );

        // When
        patternLayout.setPattern("%nopex %m%n");
        patternLayout.start();
        String val = patternLayout.doLayout(loggingEvent);

        // Then
        assertThat(val).doesNotContain("java.lang.Exception: Bogus exception");
    }

    @Test
    public void testWithParenthesis() {
        // Given
        ILoggingEvent loggingEvent = new LoggingEvent(
                ch.qos.logback.core.pattern.FormattingConverter.class.getName(),
                logger,
                Level.INFO,
                "Some message",
                null,
                null
        );

        // When
        patternLayout.setPattern("\\(%msg:%msg\\) %msg");
        patternLayout.start();
        String val = patternLayout.doLayout(loggingEvent);

        // Then
        assertThat(val).isEqualTo("(Some message:Some message) Some message");
    }

    @Test
    public void testWithLettersComingFromLog4j() {
        // Given
        // Letters: p = level and c = logger
        String pattern = "%d %p [%t] %c{30} - %m%n";

        // When
        patternLayout.setPattern(pattern);
        patternLayout.start();
        String val = patternLayout.doLayout(getEventObject());

        // Then
        // 2021-02-01 22:38:06,212 INFO [main] c.q.l.pattern.EfficientPatternLayoutTest - Some message
        String regex = ClassicTestConstants.ISO_REGEX + " INFO " + MAIN_REGEX + " c.q.l.c.p.EfficientPatternLayoutTest - Some message\\s*";
        assertThat(val).matches(regex);
    }

    @Test
    public void mdcWithDefaultValue() throws ScanException {
        // Given
        String pattern = "%msg %mdc{foo} %mdc{bar:-[null]}";
        MDC.put("foo", "foo");

        // When
        patternLayout.setPattern(OptionHelper.substVars(pattern, loggerContext));
        patternLayout.start();
        String val = patternLayout.doLayout(getEventObject());

        // Then
        assertThat(val).isEqualTo("Some message foo [null]");
    }

    @Test
    public void contextNameTest() {
        // Given
        String pattern = "%contextName";

        // When
        patternLayout.setPattern(pattern);
        patternLayout.start();

        loggerContext.setName("aValue");
        String val = patternLayout.doLayout(getEventObject());

        // Then
        assertThat(val).isEqualTo("aValue");
    }

    @Test
    public void cnTest() {
        // Given
        String pattern = "%cn";

        // When
        patternLayout.setPattern(pattern);
        patternLayout.start();

        loggerContext.setName("aValue");
        String val = patternLayout.doLayout(getEventObject());

        // Then
        assertEquals("aValue", val);
    }

    @Test
    public void testConversionRuleSupportInPatternLayout() throws JoranException {
        // Given
        String configurationFile = "src/test/resources/joran/conversionRule/patternLayout0.xml";
        String msg = "Simon says";

        // When
        configure(configurationFile);
        logger.debug(msg);

        // Then
        StringListAppender<ILoggingEvent> sla = (StringListAppender<ILoggingEvent>) root.getAppender("LIST");
        assertThat(sla).isNotNull();
        assertThat(sla.strList).containsExactly(SampleConverter.SAMPLE_STR + " - " + msg);
    }

    @Test
    public void smokeReplace() {
        // Given
        String pattern = "%replace(a1234b){'\\d{4}', 'XXXX'}";

        // When
        patternLayout.setPattern(pattern);
        patternLayout.start();
        String val = patternLayout.doLayout(getEventObject());

        // Then
        assertThat(val).isEqualTo("aXXXXb");
    }

    @Test
    public void replaceNewline() throws ScanException {
        // Given
        String pattern = "%replace(A\nB){'\n', '\n\t'}";
        ILoggingEvent loggingEvent = new LoggingEvent(
                ch.qos.logback.core.pattern.FormattingConverter.class.getName(),
                logger,
                Level.INFO,
                "",
                null,
                null
        );

        String substPattern = OptionHelper.substVars(pattern, null, loggerContext);
        assertEquals(pattern, substPattern);

        // When
        patternLayout.setPattern(substPattern);
        patternLayout.start();
        String val = patternLayout.doLayout(loggingEvent);

        // Then
        assertThat(val).isEqualTo("A\n\tB");
    }

    @Test
    public void replaceWithJoran() throws JoranException {
        // Given
        String configurationFile = "src/test/resources/joran/pattern/replace0.xml";
        String msg = "And the number is 4111111111110000, expiring on 12/2010";

        // When
        configure(configurationFile);
        logger.debug(msg);

        // Then
        StringListAppender<ILoggingEvent> sla = (StringListAppender<ILoggingEvent>) root.getAppender("LIST");
        assertThat(sla).isNotNull();
        assertThat(sla.strList).containsExactly("And the number is XXXX, expiring on 12/2010");
    }

    @Test
    public void replaceWithJoran_NEWLINE() throws JoranException {
        // Given
        String configurationFile = "src/test/resources/joran/pattern/replaceNewline.xml";
        String msg = "A\nC";

        // When
        loggerContext.putProperty("TAB", "\t");
        configure(configurationFile);
        logger.debug(msg);

        // Then
        StringListAppender<ILoggingEvent> sla = (StringListAppender<ILoggingEvent>) root.getAppender("LIST");
        assertThat(sla).isNotNull();
        assertThat(sla.strList).containsExactly("A\n\tC");
    }

    private void configure(String file) throws JoranException {
        JoranConfigurator jc = new JoranConfigurator();
        jc.setContext(loggerContext);
        jc.doConfigure(file);
    }
}
