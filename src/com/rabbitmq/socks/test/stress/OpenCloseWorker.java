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

    public OpenCloseWorker(final EndpointInfo endpoint, final RabbitSocksAPI api,
        final Executor executor)
    {
        super(api, executor);
        this.endpoint = endpoint;
    }

    @Override
    public void run()
    {
        try
        {
            String url = endpoint.getProtocols().get("websockets");
            String ticket = api.generateTicket(endpoint.getName(),
                                               "joe bloggs",
                                               1000);
            while (!closed)
            {
                Connection conn = new ConnectionImpl(new URI(url), executor);
                conn.connect(ticket);
                System.out.println("Connected");
                conn.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            failed = true;
        }
    }
}
