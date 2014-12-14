package redis.clients.jedis.async.request;

import redis.clients.jedis.Protocol;
import redis.clients.util.SafeEncoder;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class RequestBuilder {
  public static byte[] build(final Protocol.Command command) {
    return build(command.raw, new byte[0][]);
  }

  public static byte[] build(final Protocol.Command command, final byte[]... args) {
    return build(command.raw, args);
  }

  public static byte[] build(final Protocol.Command command, final String... args) {
    final byte[][] bargs = new byte[args.length][];
    for (int i = 0; i < args.length; i++) {
      bargs[i] = SafeEncoder.encode(args[i]);
    }
    return build(command, bargs);
  }

  public static byte[] build(final byte[] command, final byte[]... args) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    write(buffer, Protocol.ASTERISK_BYTE);
    writeIntCrLf(buffer, args.length + 1);
    write(buffer, Protocol.DOLLAR_BYTE);
    writeIntCrLf(buffer, command.length);
    write(buffer, command);
    writeCrLf(buffer);

    for (final byte[] arg : args) {
      write(buffer, Protocol.DOLLAR_BYTE);
      writeIntCrLf(buffer, arg.length);
      write(buffer, arg);
      writeCrLf(buffer);
    }

    return buffer.toByteArray();
  }

  public static void write(final ByteArrayOutputStream buffer, final byte b) {
    buffer.write(b);
  }

  public static void write(final ByteArrayOutputStream buffer, final byte[] b) {
    buffer.write(b, 0, b.length);
  }

  public static void writeCrLf(final ByteArrayOutputStream buffer) {
    buffer.write((byte) '\r');
    buffer.write((byte) '\n');
  }

  private final static int[] sizeTable = { 9, 99, 999, 9999, 99999, 999999, 9999999, 99999999,
      999999999, Integer.MAX_VALUE };

  private final static byte[] DigitTens = { '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '1',
      '1', '1', '1', '1', '1', '1', '1', '1', '1', '2', '2', '2', '2', '2', '2', '2', '2', '2',
      '2', '3', '3', '3', '3', '3', '3', '3', '3', '3', '3', '4', '4', '4', '4', '4', '4', '4',
      '4', '4', '4', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5', '6', '6', '6', '6', '6',
      '6', '6', '6', '6', '6', '7', '7', '7', '7', '7', '7', '7', '7', '7', '7', '8', '8', '8',
      '8', '8', '8', '8', '8', '8', '8', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9', };

  private final static byte[] DigitOnes = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
      '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8',
      '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6',
      '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4',
      '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2',
      '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', };

  private final static byte[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
      'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
      't', 'u', 'v', 'w', 'x', 'y', 'z' };

  public static void writeIntCrLf(final ByteArrayOutputStream buffer, final int value) {
    int val = value;
    if (val < 0) {
      buffer.write((byte) '-');
      val = -val;
    }

    int size = 0;
    while (val > sizeTable[size]) {
      size++;
    }

    size++;

    int q, r;
    byte[] numBuffer = new byte[size];
    int charPos = size;

    while (val >= 65536) {
      q = val / 100;
      r = val - ((q << 6) + (q << 5) + (q << 2));
      val = q;
      numBuffer[--charPos] = DigitOnes[r];
      numBuffer[--charPos] = DigitTens[r];
    }

    for (;;) {
      q = (val * 52429) >>> (16 + 3);
      r = val - ((q << 3) + (q << 1));
      numBuffer[--charPos] = digits[r];
      val = q;
      if (val == 0) break;
    }

    buffer.write(numBuffer, 0, numBuffer.length);

    writeCrLf(buffer);
  }
}
