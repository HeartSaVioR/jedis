package redis.clients.jedis.async.process;

import redis.clients.jedis.Builder;
import redis.clients.jedis.async.callback.AsyncResponseCallback;
import redis.clients.jedis.exceptions.JedisException;

public class AsyncJedisTask {
  private final byte[] request;

  private final AsyncResponseCallback callback;
  private Builder responseTypeConverter;

  public AsyncJedisTask(byte[] request, AsyncResponseCallback callback) {
    this.request = request;
    this.callback = callback;
  }

  public AsyncJedisTask(byte[] request, Builder responseTypeConverter,
      AsyncResponseCallback callback) {
    this.request = request;
    this.responseTypeConverter = responseTypeConverter;
    this.callback = callback;
  }

  public byte[] getRequest() {
    return request;
  }

  public Builder getResponseTypeConverter() {
    return responseTypeConverter;
  }

  public AsyncResponseCallback getCallback() {
    return callback;
  }

  public void callback(Object response, JedisException exception) {
    Builder responseTypeConverter = getResponseTypeConverter();

    try {
      if (responseTypeConverter != null && exception == null) {
        response = responseTypeConverter.build(response);
      }

      getCallback().execute(response, exception);
    } catch (Exception e) {
      getCallback().execute(null, new JedisException(e));
    }

  }

}
