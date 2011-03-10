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
	
    public ConnectionInfoImpl(final String url, final String guid, final String endpointName,
            final String protocol, final String meta)
    {
        super();
        this.url = url;
        this.guid = guid;
        this.endpointName = endpointName;
        this.protocol = protocol;
        this.meta = meta;
    }

    public String getUrl()
    {
        return url;
    }

    public String getGuid()
    {
        return guid;
    }

    public String getEndpointName()
    {
        return endpointName;
    }

    public String getProtocol()
    {
        return protocol;
    }

    public String getMeta()
    {
        return meta;
    }
	
	
	
	
}
