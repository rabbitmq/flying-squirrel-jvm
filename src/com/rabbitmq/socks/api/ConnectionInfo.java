package com.rabbitmq.socks.api;

/**
 *
 * @author tfox
 *
 */
public interface ConnectionInfo
{
    String getConnectionName();

    String getEndpointName();

    String getProtocol();

    String getMeta();
}
