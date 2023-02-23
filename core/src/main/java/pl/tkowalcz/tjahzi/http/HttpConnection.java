package pl.tkowalcz.tjahzi.http;

import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.concurrent.DefaultThreadFactory;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;

import java.io.Closeable;
import java.util.concurrent.ThreadFactory;

public class HttpConnection implements Closeable {

    private final ClientConfiguration clientConfiguration;
    private final MonitoringModule monitoringModule;

    private final NioEventLoopGroup group;
    private volatile ChannelFuture lokiConnection;

    public HttpConnection(ClientConfiguration clientConfiguration, MonitoringModule monitoringModule) {
        this.clientConfiguration = clientConfiguration;
        this.monitoringModule = monitoringModule;

        ThreadFactory threadFactory = new DefaultThreadFactory("tjahzi-worker", true);
        this.group = new NioEventLoopGroup(1, threadFactory);

        EventLoopGroupRetry retry = new EventLoopGroupRetry(
                group,
                __ -> {
                    monitoringModule.incrementHttpConnectAttempts();
                    recreateConnection(__);
                }
        );
        recreateConnection(retry);
    }

    private void recreateConnection(Retry retry) {
        lokiConnection = BootstrapUtil.initConnection(
                group,
                clientConfiguration,
                monitoringModule
        );

        lokiConnection.addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {
                future
                        .channel()
                        .closeFuture()
                        .addListener(__ -> retry.retry());
            } else {
                retry.retry();
            }
        });
    }

    public void execute(FullHttpRequest request) {
        BlockingRetry blockingRetry = new BlockingRetry(
                retry -> {
                    monitoringModule.incrementRetriedHttpRequests();
                    execute(request, retry);
                },
                () -> {
                    monitoringModule.incrementFailedHttpRequests();
                    request.release();
                },
                clientConfiguration.getMaxRetries()
        );

        execute(request, blockingRetry);
    }

    private void execute(FullHttpRequest request, Retry retry) {
        ChannelFuture stableReference = this.lokiConnection;

        stableReference.awaitUninterruptibly();
        if (stableReference.isSuccess() && stableReference.channel().isActive()) {
            stableReference.channel().writeAndFlush(request);
        } else {
            retry.retry();
        }
    }

    @Override
    public void close() {
        group.shutdownGracefully();
    }
}
