package redis.clients.jedis.async.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import redis.clients.jedis.async.process.AsyncJedisTask;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Deque;

public class WriteIfPossibleHandler extends ChannelInboundHandlerAdapter {
  private Deque<AsyncJedisTask> writeTaskQueue;
  private Deque<AsyncJedisTask> readTaskQueue;

  public WriteIfPossibleHandler(Deque<AsyncJedisTask> writeTaskQueue,
      Deque<AsyncJedisTask> readTaskQueue) {
    this.writeTaskQueue = writeTaskQueue;
    this.readTaskQueue = readTaskQueue;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    writeIfPossible(ctx);
  }

  @Override
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
    writeIfPossible(ctx);
  }

  private void writeIfPossible(ChannelHandlerContext ctx) {
    while (ctx.channel().isWritable()) {
      final AsyncJedisTask task = writeTaskQueue.poll();
      if (task == null) break;

      writeTask(ctx, task);
    }
  }

  private void writeTask(ChannelHandlerContext ctx, final AsyncJedisTask task) {
    ByteBuf buffer = ctx.alloc().buffer();
    buffer.writeBytes(task.getRequest());

    ctx.writeAndFlush(buffer).addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if (channelFuture.isSuccess()) {
          readTaskQueue.add(task);
        } else {
          task.callback(null, new JedisException(channelFuture.cause()));
        }
      }
    });
  }
}
