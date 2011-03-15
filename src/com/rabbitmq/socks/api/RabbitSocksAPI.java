package com.rabbitmq.socks.api;

import java.util.List;

/**
 *
 * @author tfox
 *
 */
public interface RabbitSocksAPI
{
    EndpointInfo createEndpoint(EndpointInfo endpoint) throws RabbitSocksAPIException;

    void deleteEndpoint(String endpointName) throws RabbitSocksAPIException;

    EndpointInfo getEndpoint(String endpointName) throws RabbitSocksAPIException;

    List<ConnectionInfo> listConnectionsForEndpoint(String endpointName)
        throws RabbitSocksAPIException;

    List<EndpointInfo> listEndpoints() throws RabbitSocksAPIException;

    List<ConnectionInfo> listConnections() throws RabbitSocksAPIException;

    String generateTicket(String endpointName, String identity, long timeout)
        throws RabbitSocksAPIException;
}
