package com.rabbitmq.socks.api.impl;

import com.rabbitmq.socks.api.Endpoint;
import com.rabbitmq.socks.api.EndpointBuilder;

public class EndpointBuilderImpl implements EndpointBuilder
{
	public Endpoint buildEndpoint(final String name)
	{
		return new EndpointImpl(name);
	}

}
