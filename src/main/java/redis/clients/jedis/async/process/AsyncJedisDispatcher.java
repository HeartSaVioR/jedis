package redis.clients.jedis.async.process;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.async.callback.AsyncResponseCallback;
import redis.clients.jedis.async.handler.ConnectionLossHandler;
import redis.clients.jedis.async.handler.RedisProtocolDecoder;
import redis.clients.jedis.async.handler.TaskCallbackExecutor;
import redis.clients.jedis.async.handler.WriteIfPossibleHandler;
import redis.clients.jedis.async.request.RequestBuilder;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AsyncJedisDispatcher {
  private final String host;
  private final int port;
  private String password;

  private AtomicBoolean closing = new AtomicBoolean(false);

  private AtomicReference<Channel> channelReference = new AtomicReference<Channel>(null);
  private EventLoopGroup workerGroup;

  private Deque<AsyncJedisTask> writeTaskQueue = new LinkedBlockingDeque<AsyncJedisTask>();
  private Deque<AsyncJedisTask> readTaskQueue = new LinkedBlockingDeque<AsyncJedisTask>();

  public AsyncJedisDispatcher(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public AsyncJedisDispatcher(String host, int port, String password) {
    this.host = host;
    this.port = port;
    this.password = password;
  }

  public void start() {
    // initialize EventLoop only once
    if (workerGroup == null) {
      workerGroup = new NioEventLoopGroup();
    }
    connect();
  }

  public void stop() {
    // check if it is already closing
    if (closing.getAndSet(true)) {
      return;
    }

    while (!readTaskQueue.isEmpty() || !writeTaskQueue.isEmpty()) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
      }
    }

    try {
      Channel channel = channelReference.get();
      if (channel != null) {
        channel.close().awaitUninterruptibly();
        channelReference.set(null);
      }
    } finally {
      if (workerGroup != null) {
        workerGroup.shutdownGracefully();
      }
    }
  }

  public void handleConnectionLoss(Throwable cause) {
    // consume all tasks, marking these to connection failed
    consumeReadTaskQueueToConnectionFail(cause);
    consumeWriteTaskQueueToConnectionFail(cause);

    // TODO: reconnect if possible, but not reconnect if shutdown is in progress
    stop();
  }

  public synchronized void registerRequest(final AsyncJedisTask task) {
    if (closing.get()) {
      throw new JedisException("Jedis is closing or closed, and doesn't accept any request");
    }

    writeTaskQueue.add(task);

    final Channel channel = channelReference.get();
    if (channel != null) {
      channel.eventLoop().execute(new Runnable() {
        @Override
        public void run() {
          while (channel.isWritable() && !writeTaskQueue.isEmpty()) {
            AsyncJedisTask taskToWrite = writeTaskQueue.poll();

            ByteBuf buffer = Unpooled.buffer();
            buffer.writeBytes(taskToWrite.getRequest());
            channel.writeAndFlush(buffer).addListener(new WriteCompleteListener(taskToWrite));
          }
        }
      });
    } else {
      task.callback(null, new JedisConnectionException("Connection to Redis is broken"));
    }

  }

  private synchronized void connect() {
    final AsyncJedisDispatcher dispatcher = this;

    Bootstrap b = new Bootstrap();
    b.group(workerGroup);
    b.channel(NioSocketChannel.class);
    b.option(ChannelOption.TCP_NODELAY, true);
    b.option(ChannelOption.SO_KEEPALIVE, true);
    b.handler(new ChannelInitializer<SocketChannel>() {
      @Override
      public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new WriteIfPossibleHandler(writeTaskQueue, readTaskQueue));
        ch.pipeline().addLast(new RedisProtocolDecoder());
        ch.pipeline().addLast(new TaskCallbackExecutor(readTaskQueue));
        ch.pipeline().addLast(new ConnectionLossHandler(dispatcher));
      }
    });

    // Start the client.
    ChannelFuture channelFuture = b.connect(host, port).awaitUninterruptibly();

    if (!channelFuture.isSuccess()) {
      // consume all tasks queues to mark connection failed
      Throwable cause = channelFuture.cause();

      handleConnectionLoss(cause);
    }

    channelReference.set(channelFuture.channel());

    // Handle Auth
    handleAuth();
  }

  private void handleAuth() {
    if (password != null) {
      writeTaskQueue.addFirst(buildAuthRequest());
    }
  }

  private AsyncJedisTask buildAuthRequest() {
    return new AsyncJedisTask(RequestBuilder.build(Protocol.Command.AUTH, password),
        new AsyncResponseCallback<String>() {
          @Override
          public void execute(String response, JedisException exc) {
            if (exc != null) {
              // FIXME: how to handle auth fails?
              System.err.println("ERROR: exception occurred while handling auth : " + exc);
            }
          }
        });
  }

  private void consumeWriteTaskQueueToConnectionFail(Throwable cause) {
    consumeTaskQueueToConnectionFail(writeTaskQueue, cause);
  }

  private void consumeReadTaskQueueToConnectionFail(Throwable cause) {
    consumeTaskQueueToConnectionFail(readTaskQueue, cause);
  }

  private void consumeTaskQueueToConnectionFail(Deque<AsyncJedisTask> queue, Throwable cause) {
    while (!queue.isEmpty()) {
      AsyncJedisTask taskToRead = queue.poll();
      if (cause != null) {
        taskToRead.callback(null, new JedisConnectionException(cause));
      } else {
        taskToRead.callback(null, new JedisConnectionException("Connection to Redis is broken"));
      }
    }
  }

  private class WriteCompleteListener implements ChannelFutureListener {
    private final AsyncJedisTask task;

    public WriteCompleteListener(AsyncJedisTask task) {
      this.task = task;
    }

    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
      if (channelFuture.isSuccess()) {
        readTaskQueue.add(task);
      } else {
        task.callback(null, new JedisException(channelFuture.cause()));
      }
    }
  }
}
