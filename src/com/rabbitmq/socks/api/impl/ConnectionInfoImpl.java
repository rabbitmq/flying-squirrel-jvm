package com.rabbitmq.socks.api.impl;

import com.rabbitmq.socks.api.ConnectionInfo;

/**
 *
 * @author tfox
 *
 */
public class ConnectionInfoImpl implements ConnectionInfo
{
    private final String connectionName;
    private final String endpointName;
    private final String protocol;
    private final String meta;

    public ConnectionInfoImpl(final String connectionName,
                    final String endpointName, final String protocol,
                    final String meta)
    {
        super();
        this.connectionName = connectionName;
        this.endpointName = endpointName;
        this.protocol = protocol;
        this.meta = meta;
    }

    @Override
    public String getConnectionName()
    {
        return connectionName;
    }

    @Override
    public String getEndpointName()
    {
        return endpointName;
    }

    @Override
    public String getProtocol()
    {
        return protocol;
    }

    @Override
    public String getMeta()
    {
        return meta;
    }

}
