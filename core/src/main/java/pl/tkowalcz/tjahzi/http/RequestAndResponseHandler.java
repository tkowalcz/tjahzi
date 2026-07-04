package pl.tkowalcz.tjahzi.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.ReferenceCountUtil;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;

import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

class RequestAndResponseHandler extends ChannelDuplexHandler {

    private final MonitoringModule monitoringModule;
    private final int maxRetries;

    private final ArrayDeque<PendingRequest> inFlight = new ArrayDeque<>();
    private final ExponentialBackoffStrategy retryBackoff = ExponentialBackoffStrategy.withDefault();

    private PendingRequest resending;

    RequestAndResponseHandler(MonitoringModule monitoringModule, int maxRetries) {
        this.monitoringModule = monitoringModule;
        this.maxRetries = maxRetries;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object object, ChannelPromise promise) throws Exception {
        if (object instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) object;
            monitoringModule.incrementSentHttpRequests(request.content().readableBytes());

            if (resending != null) {
                inFlight.addLast(resending);
                resending = null;
            } else {
                inFlight.addLast(
                        new PendingRequest(
                                request.retainedDuplicate(),
                                maxRetries
                        )
                );
            }
        }

        super.write(ctx, object, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) {
        FullHttpResponse msg = (FullHttpResponse) object;
        PendingRequest pendingRequest = inFlight.pollFirst();

        monitoringModule.incrementHttpResponses();
        if (msg.status().codeClass() != HttpStatusClass.SUCCESS) {
            monitoringModule.incrementHttpErrors(
                    msg.status().code(),
                    msg.content().toString(Charset.defaultCharset())
            );

            handleFailedRequest(ctx, msg.status(), pendingRequest);
        } else {
            retryBackoff.reset();

            if (pendingRequest != null) {
                pendingRequest.release();
            }
        }

        if (!HttpUtil.isKeepAlive(msg)) {
            ctx.close();
        }

        ReferenceCountUtil.release(msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        if (event instanceof IdleStateEvent
                && ((IdleStateEvent) event).state() == IdleState.READER_IDLE) {
            if (!inFlight.isEmpty()) {
                monitoringModule.addPipelineError(ReadTimeoutException.INSTANCE);
                ctx.close();
            }

            return;
        }

        super.userEventTriggered(ctx, event);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        monitoringModule.addPipelineError(cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        monitoringModule.incrementChannelInactive();

        while (!inFlight.isEmpty()) {
            monitoringModule.incrementFailedHttpRequests();
            inFlight.pollFirst().release();
        }
    }

    private void handleFailedRequest(
            ChannelHandlerContext ctx,
            HttpResponseStatus status,
            PendingRequest pendingRequest
    ) {
        if (pendingRequest == null) {
            return;
        }

        if (isRetriable(status) && pendingRequest.retriesLeft > 0) {
            pendingRequest.retriesLeft--;
            monitoringModule.incrementRetriedHttpRequests();

            ctx.channel().eventLoop().schedule(
                    () -> resend(ctx, pendingRequest),
                    retryBackoff.getAsLong(),
                    TimeUnit.MILLISECONDS
            );
        } else {
            pendingRequest.release();
        }
    }

    private void resend(ChannelHandlerContext ctx, PendingRequest pendingRequest) {
        if (!ctx.channel().isActive()) {
            monitoringModule.incrementFailedHttpRequests();
            pendingRequest.release();
            return;
        }

        resending = pendingRequest;
        ctx.channel().writeAndFlush(pendingRequest.request.retainedDuplicate());
    }

    private static boolean isRetriable(HttpResponseStatus status) {
        return status.code() == HttpResponseStatus.TOO_MANY_REQUESTS.code()
                || status.codeClass() == HttpStatusClass.SERVER_ERROR;
    }

    private static class PendingRequest {

        private final FullHttpRequest request;
        private int retriesLeft;

        private PendingRequest(FullHttpRequest request, int retriesLeft) {
            this.request = request;
            this.retriesLeft = retriesLeft;
        }

        private void release() {
            request.release();
        }
    }
}
