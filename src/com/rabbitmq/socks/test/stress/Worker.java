package com.rabbitmq.socks.test.stress;

import java.util.concurrent.Executor;

import com.rabbitmq.socks.api.RabbitSocksAPI;


/**
 *
 * @author tfox
 *
 */
public abstract class Worker extends Thread
{
    protected final RabbitSocksAPI api;
    protected final Executor executor;
    protected volatile Exception exception;
    protected volatile boolean closed;
    protected long runLength;
    protected volatile int count;
    protected final String guid;


    public Worker(final RabbitSocksAPI api, final Executor executor, final long runLength, final String guid)
    {
        this.api = api;
        this.executor = executor;
        this.runLength = runLength;
        this.guid = guid;
    }

    public Exception getException()
    {
    	return exception;
    }
    
    public int getCount()
    {
    	return count;
    }
    
    @Override
    public String toString()
    {
    	return this.getClass().getName() + ":" + this.hashCode();
    }
    
}
