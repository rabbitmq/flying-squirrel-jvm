package com.rabbitmq.socks.client.api;

import java.io.IOException;
import java.io.StringReader;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

/**
 * 
 * @author tfox
 * 
 */
public class Message
{
    public void fromJSON(final String json) throws IOException
    {
        JsonFactory factory = new JsonFactory();
        JsonParser jp = factory.createJsonParser(new StringReader(json));
        jp.nextToken();
        while (jp.nextToken() != JsonToken.END_OBJECT)
        {
            String fieldName = jp.getCurrentName();

            if ("channel".equals(fieldName))
            {
                jp.nextToken();
                channel = jp.getText();
            }
            else if ("reply".equals(fieldName))
            {
                jp.nextToken();
                reply = jp.getText();
            }
            else if ("identity".equals(fieldName))
            {
                jp.nextToken();
                identity = jp.getText();
            }
            else if ("message".equals(fieldName))
            {
                jp.nextToken();
                message = jp.getText();
            }
        }
    }

    public Message()
    {
    }

    public Message(final String channelName)
    {
        this.channel = channelName;
    }

    private String channel;

    private String reply;

    private String identity;

    private String message;

    public String getReply()
    {
        return reply;
    }

    public void setReply(String reply)
    {
        this.reply = reply;
    }

    public String getIdentity()
    {
        return identity;
    }

    public void setIdentity(String identity)
    {
        this.identity = identity;
    }

    public void setChannelName(String channelName)
    {
        this.channel = channelName;
    }

    public String getChannelName()
    {
        return channel;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String msg)
    {
        this.message = msg;
    }

    public String toJSON()
    {
        StringBuffer buff = new StringBuffer("{\"channel\":\"");
        buff.append(channel).append("\",\"message\":\"").append(message)
        .append("\"");
        if (identity != null)
        {
            buff.append(",\"identity\":\"").append(identity).append("\"");
        }
        if (reply != null)
        {
            buff.append(",\"reply\":\"").append(reply).append("\"");
        }
        buff.append('}');
        // System.out.println("json is:" + buff.toString());
        return buff.toString();
    }
}
