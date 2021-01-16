package pl.tkowalcz.tjahzi.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.concurrent.atomic.AtomicInteger;

public class InFlightRequestsTracker extends ChannelDuplexHandler {

    private final AtomicInteger requestsInFlight = new AtomicInteger();

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        requestsInFlight.incrementAndGet();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
        requestsInFlight.decrementAndGet();
    }
}
