package pl.tkowalcz.tjahzi.http;

import com.google.common.base.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.Test;
import org.xerial.snappy.Snappy;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class SnappyTest {

    @Test
    void shouldCompressAndDecompress() throws IOException {
        // Given
        String logLine = "Cupcake ipsum dolor sit amet cake wafer. " +
                "Souffle jelly beans biscuit topping. " +
                "Danish bonbon gummies powder caramels. " +
                "Danish jelly beans sweet roll topping jelly beans oat cake toffee. " +
                "Chocolate cake sesame snaps brownie biscuit cheesecake. " +
                "Ice cream dessert sweet donut marshmallow. " +
                "Muffin bear claw cookie jelly-o sugar plum jelly beans apple pie fruitcake cookie. " +
                "Tootsie roll carrot cake pastry jujubes jelly beans chupa chups. " +
                "Souffle cake muffin liquorice tart souffle pie sesame snaps.";

        byte[] expected = Strings.repeat(logLine, 10_000).getBytes();

        ByteBuf input = PooledByteBufAllocator.DEFAULT.heapBuffer();
        input.writeBytes(expected);

        ByteBuf output = PooledByteBufAllocator.DEFAULT.heapBuffer();

        // When
        new pl.tkowalcz.tjahzi.http.Snappy().encode(input, output, input.readableBytes());
        byte[] compressed = new byte[output.readableBytes()];
        output.readBytes(compressed);

        // Then
        byte[] actual = Snappy.uncompress(compressed);
        assertThat(actual).isEqualTo(expected);
    }
}
