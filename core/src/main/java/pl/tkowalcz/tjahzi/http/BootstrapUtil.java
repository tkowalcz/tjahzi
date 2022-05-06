package pl.tkowalcz.tjahzi.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;

import javax.net.ssl.SSLException;

public class BootstrapUtil {

    public static ChannelFuture initConnection(
            EventLoopGroup group,
            ClientConfiguration clientConfiguration,
            MonitoringModule monitoringModule
    ) {
        Bootstrap bootstrap = new Bootstrap();

        SslContext sslContext = null;
        if (clientConfiguration.isUseSSL()) {
            sslContext = createSslContext();
        }

        return bootstrap.group(group)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, clientConfiguration.getConnectionTimeoutMillis())
                .channel(NioSocketChannel.class)
                .handler(
                        new HttpClientInitializer(
                                monitoringModule,
                                sslContext,
                                clientConfiguration.getRequestTimeoutMillis(),
                                clientConfiguration.getMaxRequestsInFlight()
                        )
                )
                .remoteAddress(
                        clientConfiguration.getHost(),
                        clientConfiguration.getPort()
                ).connect();
    }

    private static SslContext createSslContext() {
        try {
            return SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }
}
