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
public class Message extends Frame
{
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

    public String getBody()
    {
        return message;
    }

    public void setBody(String body)
    {
        this.message = body;
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
        return buff.toString();
    }

    protected void handleField(String fieldName, JsonParser jp)
        throws IOException
    {
        if ("channel".equals(fieldName))
        {
            channel = jp.getText();
        }
        else if ("reply".equals(fieldName))
        {
            reply = jp.getText();
        }
        else if ("identity".equals(fieldName))
        {
            identity = jp.getText();
        }
        else if ("message".equals(fieldName))
        {
            message = jp.getText();
        }
    }
}
