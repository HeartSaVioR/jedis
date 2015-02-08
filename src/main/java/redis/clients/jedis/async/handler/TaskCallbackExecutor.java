package redis.clients.jedis.async.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import redis.clients.jedis.async.utils.NullObject;
import redis.clients.jedis.async.process.AsyncJedisTask;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Deque;

public class TaskCallbackExecutor extends ChannelInboundHandlerAdapter {
  private Deque<AsyncJedisTask> readTaskQueue;

  public TaskCallbackExecutor(Deque<AsyncJedisTask> readTaskQueue) {
    this.readTaskQueue = readTaskQueue;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    AsyncJedisTask task = readTaskQueue.poll();
    if (task == null) {
      throw new RuntimeException("task not found for callback");
    }

    // assume it's byte[] or JedisException, or Throwable
    if (msg instanceof JedisException) {
      // FIXME: Should we handle JedisConnectionException separately?
      task.callback(null, (JedisException) msg);
    } else if (msg instanceof Throwable) {
      // don't pass Exception to callback, continue propagating
      super.channelRead(ctx, msg);
    } else if (msg instanceof NullObject) {
      task.callback(null, null);
    } else {
      task.callback(msg, null);
    }
  }
}
