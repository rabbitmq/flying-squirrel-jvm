package com.rabbitmq.socks.api;

/**
 * 
 * @author tfox
 * 
 */
public interface ConnectionInfo
{
    String getUrl();

    String getGuid();

    String getEndpointName();

    String getProtocol();

    String getMeta();
}
