package com.rabbitmq.socks.api.test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import junit.framework.TestCase;

import com.rabbitmq.socks.api.EndpointInfo;
import com.rabbitmq.socks.api.RabbitSocksAPI;
import com.rabbitmq.socks.api.RabbitSocksAPIFactory;
import com.rabbitmq.socks.client.api.Connection;
import com.rabbitmq.socks.client.api.ConnectionImpl;

/**
 *
 * @author tfox
 *
 */
public abstract class APITestBase extends TestCase
{
    protected void deleteAllEndpoints() throws Exception
    {
        RabbitSocksAPI api = getAPI();
        List<EndpointInfo> endpoints = api.listEndpoints();
        for (EndpointInfo endpoint : endpoints)
        {
            api.deleteEndpoint(endpoint.getName());
        }
    }

    protected RabbitSocksAPI getAPI()
    {
        return RabbitSocksAPIFactory.getClient("localhost", 55672, "socks-api",
                                               "guest", "guest");
    }

    protected Connection createConnection(final String url, final String ticket)
        throws Exception
    {
        URI uri = new URI(url);
        Connection connection = new ConnectionImpl(uri,
                        Executors.newSingleThreadExecutor());
        connection.connect(ticket);
        return connection;
    }

    protected void dumpProtocolMap(Map<String, String> protocolMap)
    {
        for (Map.Entry<String, String> entry : protocolMap.entrySet())
        {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
    }
}
