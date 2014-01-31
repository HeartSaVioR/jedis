package redis.clients.util;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

public abstract class Pool<T> {
    protected GenericObjectPool<T> internalPool;

    /**
     * Using this constructor means you have to set and initialize the
     * internalPool yourself.
     */
    public Pool() {
    }

    public Pool(final GenericObjectPoolConfig poolConfig,
	    PooledObjectFactory<T> factory) {
	initPool(poolConfig, factory);
    }

    public void initPool(final GenericObjectPoolConfig poolConfig,
	    PooledObjectFactory<T> factory) {

	if (this.internalPool != null) {
	    try {
		closeInternalPool();
	    } catch (Exception e) {
	    }
	}

	this.internalPool = new GenericObjectPool<T>(factory, poolConfig);
    }

    public T getResource() {
	try {
	    return internalPool.borrowObject();
	} catch (InterruptedException e) {
	    waitForPoolAvailable();
	    return getResponseWithSimpleException();
	} catch (IllegalStateException e) {
	    waitForPoolAvailable();
	    return getResponseWithSimpleException();
	} catch (Exception e) {
	    throw new JedisConnectionException(
		    "Could not get a resource from the pool", e);
	}
    }

    // FIXME: How about we change method access to protected?
    public void returnResourceObject(final T resource) {
	try {
	    internalPool.returnObject(resource);
	} catch (IllegalStateException e) {
		// is object managed from previous pool?
	} catch (Exception e) {
	    throw new JedisException(
		    "Could not return the resource to the pool", e);
	}
    }

    public void returnBrokenResource(final T resource) {
	returnBrokenResourceObject(resource);
    }

    public void returnResource(final T resource) {
	returnResourceObject(resource);
    }

    public void destroy() {
	closeInternalPool();
    }

    protected void returnBrokenResourceObject(final T resource) {
	try {
	    internalPool.invalidateObject(resource);
	} catch (IllegalStateException e) {
		// is object managed from previous pool?
	} catch (Exception e) {
	    throw new JedisException(
		    "Could not return the resource to the pool", e);
	}
    }

    protected void closeInternalPool() {
	try {
	    internalPool.close();
	} catch (Exception e) {
	    throw new JedisException("Could not destroy the pool", e);
	}
    }
    
    private T getResponseWithSimpleException() {
    try {
        return internalPool.borrowObject();
    } catch (Exception e) {
        throw new JedisConnectionException(
        	    "Could not get a resource from the pool", e);
    }
    }
    
    private void waitForPoolAvailable() {
    while (internalPool.isClosed()) {
        try {
    	    Thread.sleep(100);
        } catch (InterruptedException e1) {
        }
    }

    // sleep a bit more
    try {
        Thread.sleep(100);
    } catch (InterruptedException e1) {
    }
    }

}