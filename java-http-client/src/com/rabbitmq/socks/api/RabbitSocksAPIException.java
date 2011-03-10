package com.rabbitmq.socks.api;

/**
 * 
 * @author tfox
 * 
 */
public class RabbitSocksAPIException extends Exception
{
    private static final long serialVersionUID = -3982745140832792222L;

    private final int responseCode;

    public RabbitSocksAPIException(final String message, final int responseCode)
    {
        super(message);

        this.responseCode = responseCode;
    }

    public RabbitSocksAPIException(final Exception cause)
    {
        super(cause);

        responseCode = -1;
    }

    public int getResponseCode()
    {
        return responseCode;
    }

}
