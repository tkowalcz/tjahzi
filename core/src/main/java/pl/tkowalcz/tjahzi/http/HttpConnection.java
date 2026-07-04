package pl.tkowalcz.tjahzi.http;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.concurrent.DefaultThreadFactory;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;

import io.netty.channel.Channel;

import java.io.Closeable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class HttpConnection implements Closeable {

    private static final long DEFAULT_CLOSE_TIMEOUT_MILLIS = 10_000;

    private final ClientConfiguration clientConfiguration;
    private final MonitoringModule monitoringModule;

    private final NioEventLoopGroup group;
    private final ChannelFutureListener writeFailureListener;
    private volatile ChannelFuture lokiConnection;

    public HttpConnection(ClientConfiguration clientConfiguration, MonitoringModule monitoringModule) {
        this.clientConfiguration = clientConfiguration;
        this.monitoringModule = monitoringModule;

        this.writeFailureListener = future -> {
            if (!future.isSuccess()) {
                monitoringModule.incrementFailedHttpRequests();
                monitoringModule.addPipelineError(future.cause());
            }
        };

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

    private void recreateConnection(EventLoopGroupRetry retry) {
        lokiConnection = BootstrapUtil.initConnection(
                group,
                clientConfiguration,
                monitoringModule
        );

        lokiConnection.addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {
                retry.reset();

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
            stableReference
                    .channel()
                    .writeAndFlush(request)
                    .addListener(writeFailureListener);
        } else {
            retry.retry();
        }
    }

    @Override
    public void close() {
        close(DEFAULT_CLOSE_TIMEOUT_MILLIS);
    }

    /**
     * Closes the connection waiting for outstanding requests to be acknowledged by Loki
     * and for the event loop to flush and terminate, so that the last batch is not lost
     * when the JVM exits right after this method returns.
     */
    public void close(long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;

        awaitOutstandingRequests(deadline);

        group.shutdownGracefully(0, timeoutMillis, TimeUnit.MILLISECONDS);
        group.terminationFuture()
                .awaitUninterruptibly(Math.max(1, deadline - System.currentTimeMillis()));
    }

    private void awaitOutstandingRequests(long deadline) {
        ChannelFuture stableReference = this.lokiConnection;
        if (stableReference == null || !stableReference.isDone() || !stableReference.isSuccess()) {
            return;
        }

        Channel channel = stableReference.channel();
        if (!channel.isActive()) {
            return;
        }

        try {
            // A noop task behind any queued writes - once it completes all
            // preceding writeAndFlush calls have registered their requests.
            channel.eventLoop()
                    .submit(() -> {
                    })
                    .await(Math.max(1, deadline - System.currentTimeMillis()));

            RequestAndResponseHandler handler = channel.pipeline().get(RequestAndResponseHandler.class);
            while (handler != null
                    && handler.outstandingRequests() > 0
                    && channel.isActive()
                    && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
