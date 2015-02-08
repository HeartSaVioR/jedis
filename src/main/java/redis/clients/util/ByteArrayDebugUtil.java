package redis.clients.util;

public class ByteArrayDebugUtil {
  public static void printContentToChars(byte[] content) {
    for (int i = 0; i < content.length; i++) {
      System.out.println(String.format("%d (%c)", content[i], (char) content[i]));
    }

  }
}
