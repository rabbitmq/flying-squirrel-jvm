package com.rabbitmq.socks.api;

import java.util.Map;

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
	
	Map<String, String> getProtocolURLMap();
	
	Map<String, ChannelDefinition> getChannelDefinitions();
	
	Endpoint putChannelDefinition(String channelName,
			                      ChannelType channelType,
			                      String resource);
	
	Endpoint putProtocolURL(String protocolName, String url);
}
