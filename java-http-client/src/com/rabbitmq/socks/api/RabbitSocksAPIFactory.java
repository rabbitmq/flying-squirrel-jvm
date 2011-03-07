package com.rabbitmq.socks.api;

import com.rabbitmq.socks.api.impl.EndpointBuilderImpl;
import com.rabbitmq.socks.api.impl.RabbitSocksAPIImpl;

public class RabbitSocksAPIFactory
{
	public static RabbitSocksAPI getClient(String hostName, int port)
	{
		return new RabbitSocksAPIImpl(hostName, port);
	}
	
	public static EndpointBuilder getEndpointBuilder()
	{
		return new EndpointBuilderImpl();
	}
}
