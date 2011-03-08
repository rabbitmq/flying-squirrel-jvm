package com.rabbitmq.socks.api;

/**
 * 
 * @author tfox
 *
 */
public interface ChannelDefinition
{
	ChannelType getType();
	
	String getResource();
}
