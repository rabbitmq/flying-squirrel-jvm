package com.rabbitmq.socks.test.stress;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.rabbitmq.socks.api.ChannelType;
import com.rabbitmq.socks.api.EndpointInfo;
import com.rabbitmq.socks.api.RabbitSocksAPI;
import com.rabbitmq.socks.api.RabbitSocksAPIFactory;
import com.rabbitmq.socks.test.APITestBase;

/**
 *
 * @author tfox
 *
 */
public class StressTest extends APITestBase
{
    public void testOpenCloseRapidly() throws Exception
    {
        final ExecutorService exec = Executors.newCachedThreadPool();
        final RabbitSocksAPI api = getAPI();
        EndpointInfo ep = RabbitSocksAPIFactory.getEndpointBuilder()
                                    .buildEndpoint("open-close-endpoint");
        ep.putChannelDefinition("foo", ChannelType.PUB, "blah");
        final EndpointInfo endpoint = api.createEndpoint(ep);

        runWorkers(30, new WorkerFactory()
        {
            @Override
            public Worker createWorker(long runLength)
            {
                return new OpenCloseWorker(endpoint, api, exec, runLength);
            }
        });
    }

    public void testSimplePubSub() throws Exception
    {
        final ExecutorService exec = Executors.newCachedThreadPool();
        final RabbitSocksAPI api = getAPI();
        final String channelPub = "ch-pub";
        final String channelSub = "ch-sub";

        final int numMessages = 10;

        runWorkers(30, new WorkerFactory()
        {
            int i;
            @Override
            public Worker createWorker(long runLength)
            {
                return new PubSubWorker(api, exec, "topic-" + i++,
                                        channelPub, channelSub, numMessages,
                                        runLength);
            }
        });
    }

    private interface WorkerFactory
    {
        Worker createWorker(long runLength);
    }

    private void runWorkers(final int numWorkers, WorkerFactory wf)
        throws Exception
    {
        Worker[] workers = new Worker[numWorkers];
        long runLength = 30 * 60 * 1000;
        for (int i = 0; i < workers.length; i++)
        {
            workers[i] = wf.createWorker(runLength);
            workers[i].start();
        }
        for (int i = 0; i < workers.length; i++)
        {
            workers[i].join();
            Exception e = workers[i].exception;
            if (e != null)
            {
            	String msg = "Got exception from worker: " + e.getMessage();
            	System.err.println(msg);
            	e.printStackTrace(System.err);
            	fail(msg);
            }
        }
        System.out.println("Workers stopped");
    }
}
