package com.rabbitmq.socks.api;

/**
 * 
 * @author tfox
 *
 */
public interface ChannelDefinition
{
	String getName();
	
	ChannelType getType();
	
	String getResource();
}
