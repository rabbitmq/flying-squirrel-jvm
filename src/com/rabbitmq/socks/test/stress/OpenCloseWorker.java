package com.rabbitmq.socks.test.stress;

import java.net.URI;
import java.util.concurrent.Executor;

import com.rabbitmq.socks.api.ChannelType;
import com.rabbitmq.socks.api.EndpointInfo;
import com.rabbitmq.socks.api.RabbitSocksAPI;
import com.rabbitmq.socks.api.RabbitSocksAPIFactory;
import com.rabbitmq.socks.client.api.Connection;
import com.rabbitmq.socks.client.api.impl.ConnectionImpl;

/**
 *
 * @author tfox
 *
 */
public class OpenCloseWorker extends Worker
{
    public OpenCloseWorker(final RabbitSocksAPI api, final Executor executor, final long runLength, final String guid)
    {
        super(api, executor, runLength, guid);  
    }

    @Override
    public void run()
    {
        try
        {
        	EndpointInfo epOpenClose = RabbitSocksAPIFactory.getEndpointBuilder()
        	.buildEndpoint("open-close-endpoint-" + guid);
	        epOpenClose.putChannelDefinition("foo", ChannelType.PUB, "blah");
	        EndpointInfo endpointOpenClose = api.createEndpoint(epOpenClose);
        	
        	long start = System.currentTimeMillis();
            String url = endpointOpenClose.getProtocols().get("websockets");
            String ticket = api.generateTicket(endpointOpenClose.getName(),
                                               "joe bloggs",
                                               1000000);
            while (System.currentTimeMillis() - start < runLength)
            {
                Connection conn = new ConnectionImpl(new URI(url), executor);
                conn.connect(ticket);
                conn.close();
                count++;
            }
        }
        catch (Exception e)
        {
            exception = e;
        }
    }       
}
