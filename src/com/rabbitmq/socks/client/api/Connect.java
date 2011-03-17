package com.rabbitmq.socks.client.api;

import java.io.IOException;
import java.io.StringReader;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonGenerator;
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

    public void generateFields(JsonGenerator jg) throws IOException
    {
        jg.writeStringField(CONNECT, ticket);
    }

    protected void handleField(String fieldName, JsonParser jp) throws IOException
    {
        if (CONNECT.equals(fieldName))
        {
            ticket = jp.getText();
        }
    }

}
