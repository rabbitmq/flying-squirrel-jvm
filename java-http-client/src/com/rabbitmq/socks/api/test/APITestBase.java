package com.rabbitmq.socks.api.test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.rabbitmq.socks.api.RabbitSocksAPI;
import com.rabbitmq.socks.api.RabbitSocksAPIFactory;

/**
 * 
 * @author tfox
 *
 */
public abstract class APITestBase extends TestCase
{
    protected void sendMessage(final Websocket ws, final String channelName,
            final String message) throws IOException
    {
        StringBuffer buff = new StringBuffer("{\"channel\":\"");
        buff.append(channelName).append("\",\"message\":\"").append(message)
                .append("\"}");
        String str = buff.toString();
        ws.send(str);
    }

    protected void deleteAllEndpoints() throws Exception
    {
        RabbitSocksAPI api = RabbitSocksAPIFactory
                .getClient("localhost", 55672);

        List<String> endpointNames = api.listEndpointNames();

        for (String endpointName : endpointNames)
        {
            api.deleteEndpoint(endpointName);
        }
    }

    protected RabbitSocksAPI getAPI()
    {
        return RabbitSocksAPIFactory.getClient("localhost", 55672);
    }

    protected Websocket createWebsocket(final String surl) throws Exception
    {
        URI uri = new URI(surl);
        Websocket ws = new Websocket(uri);
        ws.connect();
        return ws;
    }

    protected void dumpProtocolMap(Map<String, String> protocolMap)
    {
        for (Map.Entry<String, String> entry : protocolMap.entrySet())
        {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
    }
        

}
