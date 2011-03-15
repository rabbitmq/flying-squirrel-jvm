package com.rabbitmq.socks.api;

/**
 * 
 * @author tfox
 * 
 */
public interface EndpointBuilder
{
    EndpointInfo buildEndpoint(String name);
}
