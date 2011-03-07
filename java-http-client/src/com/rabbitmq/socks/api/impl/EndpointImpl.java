package com.rabbitmq.socks.api.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.rabbitmq.socks.api.ChannelDefinition;
import com.rabbitmq.socks.api.ChannelType;
import com.rabbitmq.socks.api.Endpoint;
import com.rabbitmq.socks.api.ProtocolURL;

/**
 * 
 * @author tfox
 *
 */
public class EndpointImpl implements Endpoint
{
	private final String name;
	
	private String key;
	
	private final List<ChannelDefinition> channelDefs =
		new ArrayList<ChannelDefinition>();
	
	private final List<ProtocolURL> urls = new ArrayList<ProtocolURL>();
	
	public String getKey()
	{
		return key;
	}
	
	public void setKey(final String key)
	{
	    this.key = key;
	}

	public List<ProtocolURL> getProtocolURLs()
	{
		return urls;
	}
		
	public EndpointImpl(final String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	


	public List<ChannelDefinition> getChannelDefinitions()
	{
		return Collections.unmodifiableList(channelDefs);
	}
	
	public Endpoint addChannelDefinition(final String channelName,
				final ChannelType channelType, final String resource)
	{
		channelDefs.add(new ChannelDefinitionImpl(channelName, channelType, resource));
		
		return this;
	}
	
	public Endpoint addProtocolURL(final ProtocolURL url)
	{
	    urls.add(url);
	    
	    return this;
	}
	
	private static class ChannelDefinitionImpl implements ChannelDefinition
	{
		String name;
		ChannelType channelType;
		String resource;
		
		public ChannelDefinitionImpl(final String name, final ChannelType channelType,
									 final String resource)
		{			
			this.name = name;
			this.channelType = channelType;
			this.resource = resource;
		}

		@Override
		public String getName()
		{
			return name;
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
			
			return (name.equals(cother.getName()) &&
				channelType.equals(cother.getType()) &&
				resource.equals(cother.getResource()));
		}
	}

}
