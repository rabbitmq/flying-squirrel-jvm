package com.rabbitmq.socks.test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import junit.framework.TestCase;

import com.rabbitmq.socks.api.ConnectionInfo;
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
public abstract class APITestBase extends TestCase
{
    protected void failNoException()
    {
        fail("Should throw exception");
    }

    protected void deleteAllEndpoints() throws Exception
    {
        RabbitSocksAPI api = getAPI();
        List<EndpointInfo> endpoints = api.listEndpoints();
        for (EndpointInfo endpoint : endpoints)
        {
            api.deleteEndpoint(endpoint.getName());
        }
    }

    protected static RabbitSocksAPI getAPI()
    {
        return RabbitSocksAPIFactory.getClient("localhost", 55670, "socks-api/default",
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

    /* Waiting for connections after sending a ticket will take an indeterminate
    * amount of time, so we retry in a loop and time out
    */
   protected void waitForConnections(final RabbitSocksAPI api,
                   final String endpointName, final int count)
       throws Exception
   {
       final long timeout = 5000;
       long start = System.currentTimeMillis();
       do
       {
           List<ConnectionInfo> conns;
           if (endpointName != null)
           {
               conns = api.listConnectionsForEndpoint(endpointName);
           }
           else
           {
               conns = api.listConnections();
           }
           if (conns.size() == count)
           {
               return;
           }
           Thread.sleep(10);
       }
       while (System.currentTimeMillis() - start < timeout);
       fail("Timedout waiting for connections " + count);
   }

}
