package com.rabbitmq.socks.api.impl;

import com.rabbitmq.socks.api.EndpointInfo;
import com.rabbitmq.socks.api.EndpointBuilder;

/**
 * 
 * @author tfox
 * 
 */
public class EndpointBuilderImpl implements EndpointBuilder
{
    @Override
    public EndpointInfo buildEndpoint(final String name)
    {
        return new EndpointInfoImpl(name);
    }
}
