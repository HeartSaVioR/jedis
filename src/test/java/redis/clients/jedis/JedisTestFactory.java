package redis.clients.jedis;

// Since JedisFactory is package private, this class should be same package
// or let JedisFactory change to public class
public class JedisTestFactory extends JedisFactory {

	public JedisTestFactory(String host, int port, int timeout,
			String password, int database) {
		super(host, port, timeout, password, database);
	}

}
