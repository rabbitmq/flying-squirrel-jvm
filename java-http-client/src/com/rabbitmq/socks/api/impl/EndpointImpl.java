package com.rabbitmq.socks.api.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import com.rabbitmq.socks.api.ChannelDefinition;
import com.rabbitmq.socks.api.ChannelType;
import com.rabbitmq.socks.api.Endpoint;

/**
 * 
 * @author tfox
 *
 */
public class EndpointImpl implements Endpoint
{
	private final String name;
	
	private String key;
	
	private final Map<String, ChannelDefinition> channelDefs =
		new LinkedHashMap<String, ChannelDefinition>();
	
	private final Map<String, String> urlMap =
	    new LinkedHashMap<String, String>();
	
	public String getKey()
	{
		return key;
	}
	
	public void setKey(final String key)
	{
	    this.key = key;
	}

	public Map<String, String> getProtocolURLMap()
	{
		return urlMap;
	}
		
	public EndpointImpl(final String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
	public Map<String, ChannelDefinition> getChannelDefinitions()
	{
		return channelDefs;
	}
	
	public Endpoint putChannelDefinition(final String channelName,
	                                     final ChannelType channelType,
	                                     final String resource)
	{
		channelDefs.put(channelName,
		                new ChannelDefinitionImpl(channelType, resource));
		
		return this;
	}
	
	public Endpoint putProtocolURL(final String protocolName, final String url)
	{
	    urlMap.put(protocolName, url);
	    
	    return this;
	}
	
	private static class ChannelDefinitionImpl implements ChannelDefinition
	{
		ChannelType channelType;
		String resource;
		
		public ChannelDefinitionImpl(final ChannelType channelType,
									 final String resource)
		{			
			this.channelType = channelType;
			this.resource = resource;
		}

		@Override
		public ChannelType getType()
		{
			return channelType;
		}

		@Override
		public String getResource() 
		{
			return resource;
		}	
		
		@Override
		public boolean equals(Object other)
		{
			if (other instanceof ChannelDefinition == false)
			{
				return false;
			}
			
			ChannelDefinition cother = (ChannelDefinition)other;
			
			return (channelType.equals(cother.getType()) &&
				    resource.equals(cother.getResource()));
		}
	}
}
