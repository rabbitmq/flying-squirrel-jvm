package com.rabbitmq.socks.client.api;

import java.io.IOException;
import java.io.StringReader;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonGenerator;
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
    
    public Message(final String channel, final String reply,
    		       final String identity, final String message)
    {
		this.channel = channel;
		this.reply = reply;
		this.identity = identity;
		this.message = message;
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
    
    @Override
    public boolean isConnect()
    {
    	return false;
    }

    protected void generateFields(JsonGenerator jg) throws IOException
    {
        jg.writeStringField("channel", channel);
        jg.writeStringField("message", message);
        if (identity != null)
        {
            jg.writeStringField("identity", identity);
        }
        if (reply != null)
        {
            jg.writeStringField("reply", reply);
        }
    }
}
