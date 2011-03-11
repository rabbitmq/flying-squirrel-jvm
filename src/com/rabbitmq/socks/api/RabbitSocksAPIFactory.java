package com.rabbitmq.socks.api;

import com.rabbitmq.socks.api.impl.EndpointBuilderImpl;
import com.rabbitmq.socks.api.impl.RabbitSocksAPIImpl;

/**
 *
 * @author tfox
 *
 */
public class RabbitSocksAPIFactory
{
    public static RabbitSocksAPI getClient(String hostName, int port,
                                           String prefix)
    {
        return new RabbitSocksAPIImpl(hostName, port, prefix);
    }

    public static EndpointBuilder getEndpointBuilder()
    {
        return new EndpointBuilderImpl();
    }
}
