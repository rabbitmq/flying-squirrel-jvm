package com.rabbitmq.socks.test.stress;

import java.net.URI;
import java.util.concurrent.Executor;

import com.rabbitmq.socks.api.EndpointInfo;
import com.rabbitmq.socks.api.RabbitSocksAPI;
import com.rabbitmq.socks.client.api.Connection;
import com.rabbitmq.socks.client.api.impl.ConnectionImpl;

/**
 *
 * @author tfox
 *
 */
public class OpenCloseWorker extends Worker
{
    private final EndpointInfo endpoint;
    private long runLength;

    public OpenCloseWorker(final EndpointInfo endpoint, final RabbitSocksAPI api,
        final Executor executor, long runLength)
    {
        super(api, executor);
        this.endpoint = endpoint;
        this.runLength = runLength;        
    }

    @Override
    public void run()
    {
        try
        {
        	long start = System.currentTimeMillis();
            String url = endpoint.getProtocols().get("websockets");
            String ticket = api.generateTicket(endpoint.getName(),
                                               "joe bloggs",
                                               1000000);
            int c = 0;
            while (System.currentTimeMillis() - start < runLength)
            {
                Connection conn = new ConnectionImpl(new URI(url), executor);
                conn.connect(ticket);
                conn.close();
                c++;
                if (c % 100 == 0)
                {
                	System.out.println("Done " + c);
                }
            }
        }
        catch (Exception e)
        {
            exception = e;
        }
    }
}
