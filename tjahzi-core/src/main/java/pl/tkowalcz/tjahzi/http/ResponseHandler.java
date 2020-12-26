package pl.tkowalcz.tjahzi.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;

@ChannelHandler.Sharable
class ResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    ResponseHandler() {
        super(false);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("cause = " + cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("ResponseHandler.channelInactive");
    }
}
