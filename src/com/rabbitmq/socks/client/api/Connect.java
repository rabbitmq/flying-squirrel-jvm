package com.rabbitmq.socks.client.api;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;

/**
 *
 * @author mbridgen
 *
 */
public class Connect extends Frame
{
    public final static String CONNECT = "connect";

    private String connect;

    public Connect()
    {
    }

    public Connect(final String connect)
    {
        this.connect = connect;
    }

    public String getConnect()
    {
        return connect;
    }

    public void generateFields(JsonGenerator jg) throws IOException
    {
        jg.writeStringField(CONNECT, connect);
    }
    
    public boolean isConnect()
    {
    	return true;
    }

}
