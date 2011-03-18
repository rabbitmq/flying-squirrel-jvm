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

        runWorkers(10, new WorkerFactory()
        {
            @Override
            public Worker createWorker()
            {
                return new OpenCloseWorker(endpoint, api, exec);
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

        runWorkers(10, new WorkerFactory()
        {
            int i;
            @Override
            public Worker createWorker()
            {
                return new PubSubWorker(api, exec, "topic-" + i++,
                                        channelPub, channelSub, numMessages);
            }
        });
    }

    private interface WorkerFactory
    {
        Worker createWorker();
    }

    private void runWorkers(final int numWorkers, WorkerFactory wf)
        throws Exception
    {
        Worker[] workers = new Worker[numWorkers];
        for (int i = 0; i < workers.length; i++)
        {
            workers[i] = wf.createWorker();
            workers[i].start();
        }
        long runLength = 10 * 60 * 1000;
        Thread.sleep(runLength);
        System.out.println("Stopping workers");
        for (int i = 0; i < workers.length; i++)
        {
            workers[i].close();
            workers[i].join();
            assertFalse(workers[i].isFailed());
        }
        System.out.println("Workers stopped");
    }
}
