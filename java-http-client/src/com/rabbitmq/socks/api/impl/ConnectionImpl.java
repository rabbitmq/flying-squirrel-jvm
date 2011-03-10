package com.rabbitmq.socks.api.impl;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.rabbitmq.socks.api.Connection;


@JsonIgnoreProperties(ignoreUnknown=true)
public class ConnectionImpl implements Connection
{
	private String guid;

	public String getGuid() {return guid;}
	public void setGuid(String _guid) {guid = _guid;}
}
