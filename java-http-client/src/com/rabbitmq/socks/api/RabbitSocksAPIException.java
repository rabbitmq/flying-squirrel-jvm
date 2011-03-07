package com.rabbitmq.socks.api;

public class RabbitSocksAPIException extends Exception
{
	private static final long serialVersionUID = -3982745140832792222L;
	
	private final int responseCode;

	public RabbitSocksAPIException(final String message, final int responseCode)
	{
		super(message);
		
		this.responseCode = responseCode;
	}

	public int getResponseCode()
	{
		return responseCode;
	}

}
