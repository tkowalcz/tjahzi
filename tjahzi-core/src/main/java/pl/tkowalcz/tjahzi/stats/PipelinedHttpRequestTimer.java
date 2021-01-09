package pl.tkowalcz.tjahzi.stats;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class PipelinedHttpRequestTimer extends ChannelDuplexHandler {

    private final MonitoringModule monitoringModule;
    private final TimingRingBuffer timer;

    public PipelinedHttpRequestTimer(
            MonitoringModule monitoringModule,
            int maxNumberOfRequests
    ) {
        this.monitoringModule = monitoringModule;
        this.timer = new TimingRingBuffer(monitoringModule.getClock(), maxNumberOfRequests);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        timer.record();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
        monitoringModule.recordResponseTime(timer.measure());
    }
}
