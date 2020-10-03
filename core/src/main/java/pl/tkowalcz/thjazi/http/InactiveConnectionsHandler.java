package pl.tkowalcz.thjazi.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InactiveConnectionsHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InactiveConnectionsHandler.class);

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.error("Channel became inactive");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
        LOGGER.error("Unexpected channel read invocation");
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            LOGGER.debug("Idle state event {}", ctx.channel().remoteAddress());
            ctx.close();
        }
    }
}
