package com.rabbitmq.socks.api;

public interface ChannelDefinition
{
	String getName();
	
	ChannelType getType();
	
	String getResource();
}
