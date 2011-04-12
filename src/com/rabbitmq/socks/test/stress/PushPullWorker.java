package com.rabbitmq.socks.test.stress;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.rabbitmq.socks.api.ChannelType;
import com.rabbitmq.socks.api.EndpointInfo;
import com.rabbitmq.socks.api.RabbitSocksAPI;
import com.rabbitmq.socks.api.RabbitSocksAPIFactory;
import com.rabbitmq.socks.client.api.ChannelListener;
import com.rabbitmq.socks.client.api.Connection;
import com.rabbitmq.socks.client.api.Message;
import com.rabbitmq.socks.client.api.impl.ConnectionImpl;

/**
 *
 * @author tfox
 *
 */
public class PushPullWorker extends Worker implements ChannelListener
{
    private final String queueName;
    private final String channelPush;
    private final String channelPull;
    private final int numMessages;
    private volatile CountDownLatch latch;
    private volatile int pushCount;

    public PushPullWorker(final RabbitSocksAPI api,
                        final Executor executor,
                        final String queueName,
                        final String channelPush,
                        final String channelPull,
                        final int numMessages,
                        final long runLength,
                        final String guid)
    {
        super(api, executor, runLength, guid);
        this.queueName = queueName + "-" + guid;
        this.channelPush = channelPush;
        this.channelPull = channelPull;
        this.numMessages = numMessages;
        this.runLength = runLength;
    }

    @Override
    public void onMessage(Message message)
    {
        latch.countDown();

        if (!message.getBody().equals("message-" + pushCount++))
        {
            exception = new Exception("Bad ordering");
        }
    }

    @Override
    public void run()
    {
        try
        {
        	long start = System.currentTimeMillis();
            EndpointInfo epPush = RabbitSocksAPIFactory.getEndpointBuilder()
                                        .buildEndpoint("ep-" + queueName + "-push-" + guid);
            epPush.putChannelDefinition(channelPush, ChannelType.PUSH, queueName);
            EndpointInfo endpointPush = api.createEndpoint(epPush);

            EndpointInfo epPull = RabbitSocksAPIFactory.getEndpointBuilder()
                    .buildEndpoint("ep-" + queueName + "-pull-" + guid);
            epPull.putChannelDefinition(channelPull, ChannelType.PULL, queueName);
            EndpointInfo endpointPull = api.createEndpoint(epPull);

            String urlPush = endpointPush.getProtocols().get("websockets");
            String ticketPush = api.generateTicket(endpointPush.getName(),
                                               "joe bloggs",
                                               1000000);
            String urlPull = endpointPull.getProtocols().get("websockets");
            String ticketSub = api.generateTicket(endpointPull.getName(),
                                               "joe bloggs",
                                               1000000);
            while (System.currentTimeMillis() - start < runLength)
            {
                Connection connPush = new ConnectionImpl(new URI(urlPush), executor);
                connPush.connect(ticketPush);

                Connection connPull = new ConnectionImpl(new URI(urlPull), executor);
                connPull.connect(ticketSub);
                connPull.setChannelListener(channelPull, this);

                latch = new CountDownLatch(numMessages);
                pushCount = 0;

                for (int i = 0; i < numMessages; i++)
                {
                    Message message = new Message(channelPush);
                    message.setBody("message-" + i);
                    connPush.send(message);
                }

                if (!latch.await(10, TimeUnit.SECONDS))
                {
                    exception = new Exception("Timed out waiting for messages");
                }

                connPush.close();
                connPull.close();

                count++;
            }
            api.deleteEndpoint(epPush.getName());
            api.deleteEndpoint(epPull.getName());
        }
        catch (Exception e)
        {
            exception = e;
        }
    }
}
