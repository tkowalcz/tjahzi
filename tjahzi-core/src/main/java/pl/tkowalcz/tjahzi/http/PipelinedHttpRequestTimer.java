package pl.tkowalcz.tjahzi.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;
import pl.tkowalcz.tjahzi.stats.TimingRingBuffer;

public class PipelinedHttpRequestTimer extends ChannelDuplexHandler {

    private final MonitoringModule monitoringModule;
    private final TimingRingBuffer timer;

    public PipelinedHttpRequestTimer(
            MonitoringModule monitoringModule,
            int maxRequestsInFlight
    ) {
        this.monitoringModule = monitoringModule;
        this.timer = new TimingRingBuffer(monitoringModule.getClock(), maxRequestsInFlight);
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
 
