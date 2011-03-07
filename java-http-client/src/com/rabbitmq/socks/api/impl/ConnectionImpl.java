package com.rabbitmq.socks.api.impl;

import com.rabbitmq.socks.api.Connection;

/**
 * 
 * @author tfox
 * 
 */
public class ConnectionImpl implements Connection
{
    public ConnectionImpl(final String connectionID, final String endpointName,
                          final String protocol, final byte[] metaData)
    {
        this.connectionID = connectionID;
        this.endpointName = endpointName;
        this.protocol = protocol;
        this.metaData = metaData;
    }

    private String connectionID;

    private String endpointName;

    private String protocol;

    private byte[] metaData;
    
    public String getConnectionID()
    {
        return connectionID;
    }

    public String getEndpointName()
    {
        return endpointName;
    }

    public String getProtocol()
    {
        return protocol;
    }

    public byte[] getMetaData()
    {
        return metaData;
    }
   
}
