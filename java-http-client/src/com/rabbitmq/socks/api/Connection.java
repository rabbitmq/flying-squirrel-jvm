package com.rabbitmq.socks.api;

public interface Connection
{
	String getConnectionID();
	
	String getEndpointName();
	
	String getProtocol();
	
	byte[] getMetaData();
}
