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
public class PubSubWorker extends Worker implements ChannelListener
{
    private final String topicName;
    private final String channelPub;
    private final String channelSub;
    private final int numMessages;
    private volatile CountDownLatch latch;
    private volatile int latchCount;

    public PubSubWorker(final RabbitSocksAPI api,
                        final Executor executor,
                        final String topicName,
                        final String channelPub,
                        final String channelSub,
                        final int numMessages,
                        final long runLength)
    {
        super(api, executor, runLength);
        this.topicName = topicName;
        this.channelPub = channelPub;
        this.channelSub = channelSub;
        this.numMessages = numMessages;
        this.runLength = runLength;
    }

    @Override
    public void onMessage(Message message)
    {
        latch.countDown();

        if (!message.getBody().equals("message-" + latchCount++))
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
            EndpointInfo epPub = RabbitSocksAPIFactory.getEndpointBuilder()
                                        .buildEndpoint("ep-" + topicName + "-pub");
            epPub.putChannelDefinition(channelPub, ChannelType.PUB, topicName);
            EndpointInfo endpointPub = api.createEndpoint(epPub);

            EndpointInfo epSub = RabbitSocksAPIFactory.getEndpointBuilder()
                    .buildEndpoint("ep-" + topicName + "-sub");
            epSub.putChannelDefinition(channelSub, ChannelType.SUB, topicName);
            EndpointInfo endpointSub = api.createEndpoint(epSub);

            String urlPub = endpointPub.getProtocols().get("websockets");
            String ticketPub = api.generateTicket(endpointPub.getName(),
                                               "joe bloggs",
                                               1000000);
            String urlSub = endpointSub.getProtocols().get("websockets");
            String ticketSub = api.generateTicket(endpointSub.getName(),
                                               "joe bloggs",
                                               1000000);
            while (System.currentTimeMillis() - start < runLength)
            {
                Connection connPub = new ConnectionImpl(new URI(urlPub), executor);
                connPub.connect(ticketPub);

                Connection connSub = new ConnectionImpl(new URI(urlSub), executor);
                connSub.connect(ticketSub);
                connSub.setChannelListener(channelSub, this);

                latch = new CountDownLatch(numMessages);
                latchCount = 0;

                for (int i = 0; i < numMessages; i++)
                {
                    Message message = new Message(channelPub);
                    message.setBody("message-" + i);
                    connPub.send(message);
                }

                if (!latch.await(10, TimeUnit.SECONDS))
                {
                    exception = new Exception("Timed out waiting for messages");
                }

                connPub.close();
                connSub.close();

                count++;
            }
            api.deleteEndpoint(epPub.getName());
            api.deleteEndpoint(epSub.getName());
        }
        catch (Exception e)
        {
            exception = e;
        }
    }
}
