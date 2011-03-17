package com.rabbitmq.socks.client.api;

import java.io.IOException;
import java.io.StringReader;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

/**
 *
 * @author mbridgen
 *
 */
public class Connect extends Frame
{
    public final static String CONNECT = "connect";

    public Connect()
    {
    }

    public Connect(final String ticket)
    {
        this.ticket = ticket;
    }

    private String ticket;

    public String getTicket()
    {
        return ticket;
    }

    public String toJSON()
    {
        StringBuffer buff = new StringBuffer("{\"").append(CONNECT);
        buff.append("\":\"").append(ticket).append("\"}");
        return buff.toString();
    }

    protected void handleField(String fieldName, JsonParser jp) throws IOException
    {
        if (CONNECT.equals(fieldName))
        {
            ticket = jp.getText();
        }
    }

}
