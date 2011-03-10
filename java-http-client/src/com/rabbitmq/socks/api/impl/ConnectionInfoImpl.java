package com.rabbitmq.socks.api.impl;

import com.rabbitmq.socks.api.ConnectionInfo;

/**
 * 
 * @author tfox
 * 
 */
public class ConnectionInfoImpl implements ConnectionInfo
{
    private final String url;
    private final String guid;
    private final String endpointName;
    private final String protocol;
    private final String meta;

    public ConnectionInfoImpl(final String url, final String guid,
                    final String endpointName, final String protocol,
                    final String meta)
    {
        super();
        this.url = url;
        this.guid = guid;
        this.endpointName = endpointName;
        this.protocol = protocol;
        this.meta = meta;
    }

    @Override
    public String getUrl()
    {
        return url;
    }

    @Override
    public String getGuid()
    {
        return guid;
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
