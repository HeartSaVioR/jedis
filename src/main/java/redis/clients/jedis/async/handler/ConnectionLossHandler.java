package redis.clients.jedis.async.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import redis.clients.jedis.async.process.AsyncJedisDispatcher;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class ConnectionLossHandler extends ChannelInboundHandlerAdapter {
  private AsyncJedisDispatcher dispatcher;

  public ConnectionLossHandler(AsyncJedisDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    dispatcher.handleConnectionLoss(null);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (cause instanceof JedisConnectionException) {
      dispatcher.handleConnectionLoss(cause);
    } else {
      super.exceptionCaught(ctx, cause);
    }
  }
}
