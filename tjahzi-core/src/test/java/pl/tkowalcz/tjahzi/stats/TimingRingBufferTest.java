package pl.tkowalcz.tjahzi.stats;

import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimingRingBufferTest {

    @Test
    void shouldActLikeARingBufferOfTimings() {
        // Given
        Clock clock = mock(Clock.class);

        long firstRecording = 10L;
        long secondRecording = 20L;
        long thirdRecording = 33L;
        long firstResponse = 38L;
        long secondResponse = 41L;
        long thirdResponse = 56L;

        when(clock.millis())
                .thenReturn(firstRecording)
                .thenReturn(secondRecording)
                .thenReturn(thirdRecording)
                .thenReturn(firstResponse)
                .thenReturn(secondResponse)
                .thenReturn(thirdResponse);

        TimingRingBuffer buffer = new TimingRingBuffer(
                clock,
                10
        );

        // When
        buffer.record();
        buffer.record();
        buffer.record();

        long firstRTT = buffer.measure();
        long secondRTT = buffer.measure();
        long thirdRTT = buffer.measure();

        // Then
        assertThat(firstRTT).isEqualTo(firstResponse - firstRecording);
        assertThat(secondRTT).isEqualTo(secondResponse - secondRecording);
        assertThat(thirdRTT).isEqualTo(thirdResponse - thirdRecording);
    }

    @Test
    void shouldHandleIntermixedResponsesAndNewRexuests() {
        // Given
        Clock clock = mock(Clock.class);

        long firstRecording = 10L;
        long secondRecording = 20L;
        long firstResponse = 22L;
        long thirdRecording = 33L;
        long secondResponse = 41L;
        long fourthRecording = 48L;
        long thirdResponse = 56L;
        long fourthResponse = 58L;

        when(clock.millis())
                .thenReturn(firstRecording)
                .thenReturn(secondRecording)
                .thenReturn(firstResponse)
                .thenReturn(thirdRecording)
                .thenReturn(secondResponse)
                .thenReturn(fourthRecording)
                .thenReturn(thirdResponse)
                .thenReturn(fourthResponse);

        TimingRingBuffer buffer = new TimingRingBuffer(
                clock,
                10
        );

        // When
        buffer.record();
        buffer.record();
        long firstRTT = buffer.measure();
        buffer.record();
        long secondRTT = buffer.measure();
        buffer.record();
        long thirdRTT = buffer.measure();
        long fourthRTT = buffer.measure();

        // Then
        assertThat(firstRTT).isEqualTo(firstResponse - firstRecording);
        assertThat(secondRTT).isEqualTo(secondResponse - secondRecording);
        assertThat(thirdRTT).isEqualTo(thirdResponse - thirdRecording);
        assertThat(fourthRTT).isEqualTo(fourthResponse - fourthRecording);
    }

    @Test
    void shouldHandleWrappingAround() {
        // Given
        SettableClock clock = new SettableClock();

        TimingRingBuffer buffer = new TimingRingBuffer(
                clock,
                10
        );

        // When & Then
        for (int i = 0; i < 23; i++) {
            long tick = 40L;

            buffer.record();
            clock.tick(tick);
            long actual = buffer.measure();

            assertThat(actual).isEqualTo(tick);
        }
    }
}
