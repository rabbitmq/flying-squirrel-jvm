package com.rabbitmq.socks.test;

import java.net.URI;
import java.util.concurrent.Executors;

import com.rabbitmq.socks.api.ChannelType;
import com.rabbitmq.socks.api.EndpointInfo;
import com.rabbitmq.socks.api.RabbitSocksAPI;
import com.rabbitmq.socks.api.RabbitSocksAPIFactory;
import com.rabbitmq.socks.client.api.Connection;
import com.rabbitmq.socks.client.api.impl.ConnectionImpl;

public class KillClient
{
	public static void main(String[] args)
	{
		try
		{
			System.out.println("Starting kill client");
			
			//Open a connection and wait until the process is killed
			
			RabbitSocksAPI api = RabbitSocksAPIFactory.getClient("localhost", 55672, "socks-api/default",
	                "guest", "guest");
			
			EndpointInfo endpoint = RabbitSocksAPIFactory.getEndpointBuilder()
	        	.buildEndpoint("pub-sub-endpoint-0");
			endpoint.putChannelDefinition("ch-pub", ChannelType.PUB, "topic1");
			endpoint = api.createEndpoint(endpoint);
			String url = endpoint.getProtocols().get("websockets");
	
			String ticket = api.generateTicket(endpoint.getName(), "joe bloggs", 1000000);
			URI uri = new URI(url);
	        Connection connection = new ConnectionImpl(uri,
	                        Executors.newSingleThreadExecutor());
	        connection.connect(ticket);
	        
	        System.out.println("Now kill me!");
	        
	        Thread.sleep(10000000);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
