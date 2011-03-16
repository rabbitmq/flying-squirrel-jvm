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
    protected volatile boolean failed;
    protected volatile boolean closed;

    public Worker(final RabbitSocksAPI api, final Executor executor)
    {
        this.api = api;
        this.executor = executor;
    }

    public void close()
    {
        closed = true;
    }

    public boolean isFailed()
    {
        return failed;
    }
}
