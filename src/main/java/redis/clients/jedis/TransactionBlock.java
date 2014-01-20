package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisException;

public abstract class TransactionBlock {
	
	protected Transaction transaction;
	
    public TransactionBlock(Transaction trans) {
    	this.transaction = trans; 
    }

    public TransactionBlock() {
    }

    public abstract void execute() throws JedisException;
    
    public void setTransaction(Transaction trans) {
	    this.transaction = trans;
    }
}
