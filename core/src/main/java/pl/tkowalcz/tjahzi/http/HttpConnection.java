package pl.tkowalcz.tjahzi.http;

import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.FullHttpRequest;
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

        ThreadGroup threadGroup = new ThreadGroup("Tjahzi Loki client");
        ThreadFactory threadFactory = r -> new Thread(threadGroup, r, "tjahzi-worker");

        this.group = new NioEventLoopGroup(1, threadFactory);

        EventLoopGroupRetry retry = new EventLoopGroupRetry(
                group,
                this::recreateConnection,
                monitoringModule
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
                retry -> execute(request, retry),
                clientConfiguration.getMaxRetries(),
                monitoringModule
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
