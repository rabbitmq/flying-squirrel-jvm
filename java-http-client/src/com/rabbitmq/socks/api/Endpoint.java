package com.rabbitmq.socks.api;

import java.util.List;

/**
 * 
 * @author tfox
 *
 */
public interface Endpoint
{
	String getName();
	
	String getKey();
	
	void setKey(String key);
	
	List<ProtocolURL> getProtocolURLs();
	
	List<ChannelDefinition> getChannelDefinitions();
	
	Endpoint addChannelDefinition(String channelName,
			                      ChannelType channelType, String resource);
	
	Endpoint addProtocolURL(ProtocolURL url);
}
