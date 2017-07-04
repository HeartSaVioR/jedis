package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

public interface PubsubCommands {
  List<String> pubsubChannels(String pattern);

  Long pubsubNumPat();

  Map<String, String> pubsubNumSub(String... channels);
}
