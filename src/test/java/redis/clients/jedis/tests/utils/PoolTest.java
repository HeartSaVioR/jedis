package redis.clients.jedis.tests.utils;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisTestFactory;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.tests.HostAndPortUtil;
import redis.clients.util.Pool;

public class PoolTest {
	private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);
    private static final int TEST_OPERATIONS = 1000;

    @Test
    public void multiThreadGetAndReturnObjects() throws Exception {
    	JedisPoolConfig config = new JedisPoolConfig();
    	config.setMaxWaitMillis(3000);
    	config.setBlockWhenExhausted(true);
    	
    	JedisTestFactory factory = new JedisTestFactory(hnp.getHost(), hnp.getPort(), 
    			2000, "foobared", 0);
    	
    	final Pool<Jedis> pool = new Pool<Jedis>(config, factory) {};
    	assertTrue(multiGetReturn(pool));
		pool.destroy();
    }
    
    @Test
    public void testMultiGetReturnDuringInitPoolCalled() {
    	final JedisPoolConfig config = new JedisPoolConfig();
    	config.setMaxWaitMillis(1000);
    	config.setBlockWhenExhausted(true);
    	config.setMinIdle(0);
    	config.setMaxIdle(5);
    	config.setMaxTotal(10);
    	config.setNumTestsPerEvictionRun(2);
    	config.setMinEvictableIdleTimeMillis(100);
    	config.setTimeBetweenEvictionRunsMillis(100);
    	
    	final JedisTestFactory factory = new JedisTestFactory(hnp.getHost(), hnp.getPort(), 
    			2000, "foobared", 0);
    	
    	final Pool<Jedis> pool = new Pool<Jedis>(config, factory) {};
   	
    	Thread t = new Thread(new Runnable() {
    		public void run() {
    			try {
					Thread.sleep(200);
				} catch (InterruptedException e1) {
				}
    			
    			JedisTestFactory factory = new JedisTestFactory(hnp.getHost(), hnp.getPort(), 
    	    			2000, "foobared", 0);
				pool.initPool(config, factory);
    		}
    	});
    	
    	t.start();
    	assertTrue(multiGetReturn(pool));
    	
    	try {
			t.join();
		} catch (InterruptedException e) {
		}
    	
    	pool.destroy();
    }
    
    private boolean multiGetReturn(final Pool<Jedis> pool) {
    	List<Thread> tds = new ArrayList<Thread>();

		final AtomicInteger ind = new AtomicInteger();
		final AtomicBoolean passed = new AtomicBoolean(true);
		final AtomicReference<Exception> exc = new AtomicReference<Exception>(null);
		
		for (int i = 0; i < 10; i++) {
		    Thread hj = new Thread(new Runnable() {
			public void run() {
				try {
					for (int i = 0; (i = ind.getAndIncrement()) < TEST_OPERATIONS;) {
				    	Jedis j = null;
				    	
				    	try {
				    		j = pool.getResource();
				    	} catch (JedisConnectionException e) {
				    		//System.out.println("Getting resource from pool failed - not connected or blocked (maybe deadlock?)");
				    		if (j != null)
				    			pool.returnBrokenResource(j);
				    		
				    		throw e;
				    	}/* catch (Exception e) {
				    		e.printStackTrace();
				    		// continue
				    	}*/
				    	
				    	if (j == null)
				    		continue;
				    	
				    	try {
				    		j.ping();
				    	} catch (JedisConnectionException e) {
				    		pool.returnBrokenResource(j);
				    		
				    		j = null;
				    	} finally {
				    		if (j != null) {
				    			pool.returnResource(j);
				    		}
				    	}
				    }
	
				} catch (Exception e) {
					System.out.println("Exception occurred - exception: " + e);
					e.printStackTrace();
					
					passed.set(false);
				}
		    } 
			});
		    tds.add(hj);
		    hj.start();
		}
		
		for (Thread t : tds)
			try {
				t.join();
			} catch (InterruptedException e) {
				System.out.println(e);
			}

		return passed.get();
    }
}
