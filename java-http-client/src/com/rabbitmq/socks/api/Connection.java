package com.rabbitmq.socks.api;

/**
 * 
 * @author tfox
 *
 */
public interface Connection
{
	String getConnectionID();
	
	String getEndpointName();
	
	String getProtocol();
	
	byte[] getMetaData();
}
