package com.rabbitmq.socks.api;

/**
 * 
 * @author tfox
 *
 */
public interface EndpointBuilder
{
	Endpoint buildEndpoint(String name);
}
