package com.rabbitmq.socks.test.stress;

import java.io.IOException;
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
public class ReqRepWorker extends Worker
{
    private final String convName;
    private final String channelReq;
    private final String channelRep;
    private final int numMessages;
    private volatile CountDownLatch latchReq;
    private volatile CountDownLatch latchRep;
    private volatile int countReq;
    private volatile int countRep;

    public ReqRepWorker(final RabbitSocksAPI api,
                        final Executor executor,
                        final String convName,
                        final String channelReq,
                        final String channelRep,
                        final int numMessages,
                        final long runLength,
                        final String guid)
    {
        super(api, executor, runLength, guid);
        this.convName = convName + "-" + guid;
        this.channelReq = channelReq;
        this.channelRep = channelRep;
        this.numMessages = numMessages;
        this.runLength = runLength;
    }

    @Override
    public void run()
    {
        try
        {
        	long start = System.currentTimeMillis();
            EndpointInfo epReq = RabbitSocksAPIFactory.getEndpointBuilder()
                                        .buildEndpoint("ep-" + convName + "-req-" + guid);
            epReq.putChannelDefinition(channelReq, ChannelType.REQ, convName);
            EndpointInfo endpointReq = api.createEndpoint(epReq);

            EndpointInfo epRep = RabbitSocksAPIFactory.getEndpointBuilder()
                    .buildEndpoint("ep-" + convName + "-rep-" + guid);
            epRep.putChannelDefinition(channelRep, ChannelType.REP, convName);
            EndpointInfo endpointRep = api.createEndpoint(epRep);

            String urlReq = endpointReq.getProtocols().get("websockets");
            String ticketReq = api.generateTicket(endpointReq.getName(),
                                               "joe bloggs",
                                               1000000);
            String urlRep = endpointRep.getProtocols().get("websockets");
            String ticketRep = api.generateTicket(endpointRep.getName(),
                                               "joe bloggs",
                                               1000000);
            while (System.currentTimeMillis() - start < runLength)
            {
                Connection connReq = new ConnectionImpl(new URI(urlReq), executor);
                connReq.connect(ticketReq);

                final Connection connRep = new ConnectionImpl(new URI(urlRep), executor);
                connRep.connect(ticketRep);
                connRep.setChannelListener(channelRep, new ChannelListener() {
                	@Override
                    public void onMessage(Message message)
                    {                        
                        if (!message.getBody().equals("message-" + countReq))
                        {
                            exception = new Exception("Bad ordering on request");
                        }
                        
                        latchReq.countDown();
                        
                        Message response = new Message(channelRep);
                        response.setReplyTo(message.getReplyTo());
                        response.setBody("response-" + countReq);
                        
                        countReq++;
                        
                        try
                        {
                        	connRep.send(response);
                        }
                        catch (IOException e)
                        {                        	
                        	exception = e;
                        }
                    }
                });
                
                connReq.setChannelListener(channelReq, new ChannelListener() {
                	@Override
                    public void onMessage(Message message)
                    {                        
                        if (!message.getBody().equals("response-" + countRep++))
                        {
                            exception = new Exception("Bad ordering on response");
                        }
                        
                        latchRep.countDown();
                    }
                });
                

                latchReq = new CountDownLatch(numMessages);
                latchRep = new CountDownLatch(numMessages);
                countReq = 0;
                countRep = 0;
                
                for (int i = 0; i < numMessages; i++)
                {
                    Message message = new Message(channelReq);
                    message.setBody("message-" + i);
                    connReq.send(message);
                }

                if (!latchReq.await(10, TimeUnit.SECONDS))
                {
                    exception = new Exception("Timed out waiting for requests");
                }
                
                if (!latchRep.await(10, TimeUnit.SECONDS))
                {
                    exception = new Exception("Timed out waiting for responses");
                }

                connReq.close();
                connRep.close();

                count++;                
            }
            api.deleteEndpoint(epReq.getName());
            api.deleteEndpoint(epRep.getName());
        }
        catch (Exception e)
        {
            exception = e;
        }
    }
}
