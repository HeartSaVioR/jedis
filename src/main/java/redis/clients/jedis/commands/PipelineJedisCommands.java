package redis.clients.jedis.commands;

import redis.clients.jedis.BinaryClient;
import redis.clients.jedis.BitOP;
import redis.clients.jedis.BitPosParams;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoRadiusResponse;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.Response;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.ZParams;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.set.SetParams;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PipelineJedisCommands {
  Response<Long> append(String key, String value);

  Response<List<String>> blpop(int timeout, String arg);

  Response<List<String>> brpop(int timeout, String arg);

  Response<Long> decr(String key);

  Response<Long> decrBy(String key, long integer);

  Response<Long> del(String key);

  Response<String> echo(String string);

  Response<Boolean> exists(String key);

  Response<Long> expire(String key, int seconds);

  Response<Long> pexpire(String key, long milliseconds);

  Response<Long> expireAt(String key, long unixTime);

  Response<Long> pexpireAt(String key, long millisecondsTimestamp);

  Response<String> get(String key);

  Response<Boolean> getbit(String key, long offset);

  Response<Long> bitpos(final String key, final boolean value);

  Response<Long> bitpos(final String key, final boolean value, final BitPosParams params);

  Response<String> getrange(String key, long startOffset, long endOffset);

  Response<String> getSet(String key, String value);

  Response<Long> hdel(String key, String... field);

  Response<Boolean> hexists(String key, String field);

  Response<String> hget(String key, String field);

  Response<Map<String, String>> hgetAll(String key);

  Response<Long> hincrBy(String key, String field, long value);

  Response<Double> hincrByFloat(String key, String field, double increment);

  Response<Set<String>> hkeys(String key);

  Response<Long> hlen(String key);

  Response<List<String>> hmget(String key, String... fields);

  Response<String> hmset(String key, Map<String, String> hash);

  Response<Long> hset(String key, String field, String value);

  Response<Long> hsetnx(String key, String field, String value);

  Response<List<String>> hvals(String key);

  Response<Long> incr(String key);

  Response<Long> incrBy(String key, long integer);

  Response<Double> incrByFloat(String key, double increment);

  Response<String> lindex(String key, long index);

  Response<Long> linsert(String key, BinaryClient.LIST_POSITION where, String pivot, String value);

  Response<Long> llen(String key);

  Response<String> lpop(String key);

  Response<Long> lpush(String key, String... string);

  Response<Long> lpushx(String key, String... string);

  Response<List<String>> lrange(String key, long start, long end);

  Response<Long> lrem(String key, long count, String value);

  Response<String> lset(String key, long index, String value);

  Response<String> ltrim(String key, long start, long end);

  Response<Long> move(String key, int dbIndex);

  Response<Long> persist(String key);

  Response<String> rpop(String key);

  Response<Long> rpush(String key, String... string);

  Response<Long> rpushx(String key, String... string);

  Response<Long> sadd(String key, String... member);

  Response<Long> scard(String key);

  Response<Boolean> sismember(String key, String member);

  Response<String> set(String key, String value);

  Response<String> set(byte[] key, byte[] value, SetParams params);

  Response<Boolean> setbit(String key, long offset, boolean value);

  Response<String> setex(String key, int seconds, String value);

  Response<String> psetex(String key, long milliseconds, String value);

  Response<Long> setnx(String key, String value);

  Response<Long> setrange(String key, long offset, String value);

  Response<Set<String>> smembers(String key);

  Response<List<String>> sort(String key);

  Response<List<String>> sort(String key, SortingParams sortingParameters);

  Response<String> spop(String key);

  Response<Set<String>> spop(String key, long count);

  Response<String> srandmember(String key);

  Response<Long> srem(String key, String... member);

  Response<Long> strlen(String key);

  Response<String> substr(String key, int start, int end);

  Response<Long> ttl(String key);

  Response<Long> pttl(String key);

  Response<String> type(String key);

  Response<Long> zadd(String key, double score, String member);

  Response<Long> zadd(String key, double score, String member, ZAddParams params);

  Response<Long> zadd(String key, Map<String, Double> scoreMembers);

  Response<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params);

  Response<Long> zcard(String key);

  Response<Long> zcount(String key, double min, double max);

  Response<Double> zincrby(String key, double score, String member);

  Response<Double> zincrby(String key, double score, String member, ZIncrByParams params);

  Response<Set<String>> zrange(String key, long start, long end);

  Response<Set<String>> zrangeByScore(String key, double min, double max);

  Response<Set<String>> zrangeByScore(String key, String min, String max);

  Response<Set<String>> zrangeByScore(String key, double min, double max, int offset, int count);

  Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max);

  Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max, int offset,
      int count);

  Response<Set<String>> zrevrangeByScore(String key, double max, double min);

  Response<Set<String>> zrevrangeByScore(String key, String max, String min);

  Response<Set<String>> zrevrangeByScore(String key, double max, double min, int offset, int count);

  Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min);

  Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min, int offset,
      int count);

  Response<Set<Tuple>> zrangeWithScores(String key, long start, long end);

  Response<Long> zrank(String key, String member);

  Response<Long> zrem(String key, String... member);

  Response<Long> zremrangeByRank(String key, long start, long end);

  Response<Long> zremrangeByScore(String key, double start, double end);

  Response<Set<String>> zrevrange(String key, long start, long end);

  Response<Set<Tuple>> zrevrangeWithScores(String key, long start, long end);

  Response<Long> zrevrank(String key, String member);

  Response<Double> zscore(String key, String member);

  Response<Long> zlexcount(final String key, final String min, final String max);

  Response<Set<String>> zrangeByLex(final String key, final String min, final String max);

  Response<Set<String>> zrangeByLex(final String key, final String min, final String max,
      final int offset, final int count);

  Response<Set<String>> zrevrangeByLex(final String key, final String max, final String min);

  Response<Set<String>> zrevrangeByLex(final String key, final String max, final String min,
      final int offset, final int count);

  Response<Long> zremrangeByLex(final String key, final String start, final String end);

  Response<Long> bitcount(String key);

  Response<Long> bitcount(String key, long start, long end);

  Response<Long> pfadd(final String key, final String... elements);

  Response<Long> pfcount(final String key);
  
  Response<List<Long>> bitfield(String key, String... arguments);
  
  Response<Long> hstrlen(String key, String field);

  // Geo Commands

  Response<Long> geoadd(String key, double longitude, double latitude, String member);

  Response<Long> geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap);

  Response<Double> geodist(String key, String member1, String member2);

  Response<Double> geodist(String key, String member1, String member2, GeoUnit unit);

  Response<List<String>> geohash(String key, String... members);

  Response<List<GeoCoordinate>> geopos(String key, String... members);

  Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude,
      double radius, GeoUnit unit);

  Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude,
      double radius, GeoUnit unit, GeoRadiusParam param);

  Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius,
      GeoUnit unit);

  Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius,
      GeoUnit unit, GeoRadiusParam param);

  // multi key operations

  Response<Long> del(String... keys);

  Response<Long> exists(String... keys);

  Response<List<String>> blpop(int timeout, String... args);

  Response<List<String>> brpop(int timeout, String... args);

  Response<Set<String>> keys(String pattern);

  Response<List<String>> mget(String... keys);

  Response<String> mset(String... keysvalues);

  Response<Long> msetnx(String... keysvalues);

  Response<String> rename(String oldkey, String newkey);

  Response<Long> renamenx(String oldkey, String newkey);

  Response<String> rpoplpush(String srckey, String dstkey);

  Response<Set<String>> sdiff(String... keys);

  Response<Long> sdiffstore(String dstkey, String... keys);

  Response<Set<String>> sinter(String... keys);

  Response<Long> sinterstore(String dstkey, String... keys);

  Response<Long> smove(String srckey, String dstkey, String member);

  Response<Long> sort(String key, SortingParams sortingParameters, String dstkey);

  Response<Long> sort(String key, String dstkey);

  Response<Set<String>> sunion(String... keys);

  Response<Long> sunionstore(String dstkey, String... keys);

  Response<String> watch(String... keys);

  Response<Long> zinterstore(String dstkey, String... sets);

  Response<Long> zinterstore(String dstkey, ZParams params, String... sets);

  Response<Long> zunionstore(String dstkey, String... sets);

  Response<Long> zunionstore(String dstkey, ZParams params, String... sets);

  Response<String> brpoplpush(String source, String destination, int timeout);

  Response<Long> publish(String channel, String message);

  Response<String> randomKey();

  Response<Long> bitop(BitOP op, final String destKey, String... srcKeys);

  Response<String> pfmerge(final String destkey, final String... sourcekeys);

  Response<Long> pfcount(final String... keys);

  Response<byte[]> dump(String key);

  Response<String> restore(String key, int ttl, byte[] serializedValue);

  Response<String> migrate(String host, int port, String key, int destinationDb, int timeout);

  Response<Long> objectRefcount(String key);

  Response<String> objectEncoding(String key);

  Response<Long> objectIdletime(String key);
}
