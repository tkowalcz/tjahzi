package pl.tkowalcz.tjahzi.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;

public class BootstrapUtil {

    public static ChannelFuture initConnection(
            EventLoopGroup group,
            ClientConfiguration clientConfiguration,
            MonitoringModule monitoringModule
    ) {
        Bootstrap bootstrap = new Bootstrap();

        return bootstrap.group(group)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, clientConfiguration.getConnectionTimeoutMillis())
                .channel(NioSocketChannel.class)
                .handler(
                        new HttpClientInitializer(
                                monitoringModule,
                                clientConfiguration.getRequestTimeoutMillis(),
                                clientConfiguration.getMaxInFlightRequests()
                        )
                )
                .remoteAddress(
                        clientConfiguration.getHost(),
                        clientConfiguration.getPort()
                ).connect();
    }
}
