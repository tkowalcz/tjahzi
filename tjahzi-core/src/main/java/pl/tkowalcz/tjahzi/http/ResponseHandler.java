package pl.tkowalcz.tjahzi.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpStatusClass;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;

@ChannelHandler.Sharable
class ResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final MonitoringModule monitoringModule;

    ResponseHandler(MonitoringModule monitoringModule) {
        super(false);
        this.monitoringModule = monitoringModule;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
        monitoringModule.incrementHttpResponses();
        if (msg.status().codeClass() != HttpStatusClass.SUCCESS) {
            monitoringModule.incrementHttpErrors(msg.status(), msg.content());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        monitoringModule.addPipelineError(cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        monitoringModule.incrementChannelInactive();
    }
}
