package redis.clients.jedis.async.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import redis.clients.jedis.async.utils.NullObject;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.SafeEncoder;

import java.util.ArrayList;
import java.util.List;

import static redis.clients.jedis.Protocol.*;

public class RedisProtocolDecoder extends ReplayingDecoder<Object> {
  // RecyclableArrayList in Netty doesn't allow adding null
  public static final NullObject NULL = new NullObject();

  @Override
  protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf,
      List<Object> list) throws Exception {
    try {
      Object response = process(byteBuf);
      if (response == null) {
        list.add(NULL);
      } else {
        list.add(response);
      }

    } catch (JedisDataException e) {
      list.add(e);
    }
  }

  private Object process(final ByteBuf byteBuf) {
    final byte type = byteBuf.readByte();
    if (type == PLUS_BYTE) {
      return processStatusCodeReply(byteBuf);
    } else if (type == DOLLAR_BYTE) {
      return processBulkReply(byteBuf);
    } else if (type == ASTERISK_BYTE) {
      return processMultiBulkReply(byteBuf);
    } else if (type == COLON_BYTE) {
      return processInteger(byteBuf);
    } else if (type == MINUS_BYTE) {
      processError(byteBuf);
      return null;
    } else {
      throw new JedisConnectionException("Unknown reply: " + (char) type);
    }
  }

  private String processStatusCodeReply(final ByteBuf byteBuf) {
    return SafeEncoder.encode(readLineCrLf(byteBuf));
  }

  private byte[] processBulkReply(final ByteBuf byteBuf) {
    final int len = (int) readLongCrLf(byteBuf);
    if (len == -1) {
      return null;
    }

    byte[] content = new byte[len];
    byteBuf.readBytes(content);

    // read 2 more bytes for the command delimiter
    getRidOfCrLf(byteBuf);

    return content;
  }

  private Long processInteger(final ByteBuf byteBuf) {
    return readLongCrLf(byteBuf);
  }

  private List<Object> processMultiBulkReply(final ByteBuf byteBuf) {
    final int num = (int) readLongCrLf(byteBuf);
    if (num == -1) {
      return null;
    }

    List<Object> ret = new ArrayList<Object>(num);
    for (int i = 0; i < num; i++) {
      try {
        ret.add(process(byteBuf));
      } catch (JedisDataException e) {
        ret.add(e);
      }
    }
    return ret;
  }

  private void processError(final ByteBuf byteBuf) {
    String message = SafeEncoder.encode(readLineCrLf(byteBuf));
    throw new JedisDataException(message);
  }

  private byte[] readLineCrLf(ByteBuf byteBuf) {
    int length = findCr(byteBuf);
    ByteBuf contentBuf = byteBuf.readSlice(length);

    byte[] content = new byte[length];
    contentBuf.readBytes(content);

    getRidOfCrLf(byteBuf);

    return content;
  }

  private long readLongCrLf(ByteBuf byteBuf) {
    byte b = byteBuf.readByte();
    final boolean isNeg = b == '-';

    if (isNeg) {
      b = byteBuf.readByte();
    }

    long value = b - '0';

    int length = findCr(byteBuf);

    ByteBuf contentBuf = byteBuf.readSlice(length);

    while (contentBuf.isReadable()) {
      value = value * 10 + contentBuf.readByte() - '0';
    }

    getRidOfCrLf(byteBuf);

    return (isNeg ? -value : value);
  }

  private int findCr(ByteBuf byteBuf) {
    int length = byteBuf.bytesBefore((byte) '\r');
    if (length < 0) {
      throw new JedisConnectionException("Seems like Redis closed connection");
    }

    return length;
  }

  private void getRidOfCrLf(ByteBuf byteBuf) {
    // skip \r\n
    byteBuf.skipBytes(2);
  }

}
