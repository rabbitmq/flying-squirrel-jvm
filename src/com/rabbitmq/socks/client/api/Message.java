package com.rabbitmq.socks.client.api;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;

import com.rabbitmq.socks.client.api.impl.Frame;
import com.rabbitmq.socks.client.api.impl.FrameType;

/**
 *
 * @author tfox
 *
 */
public class Message extends Frame
{
    public Message(final String channelName)
    {
        super(FrameType.MESSAGE);
        this.channel = channelName;
    }

    public Message(final String channel, final String replyTo,
    		       final String identity, final String body)
    {
        super(FrameType.MESSAGE);
		this.channel = channel;
		this.replyTo = replyTo;
		this.identity = identity;
		this.body = body;
	}

	private String channel;
    private String replyTo;
    private String identity;
    private String body;

    public String getReplyTo()
    {
        return replyTo;
    }

    public void setReplyTo(String replyTo)
    {
        this.replyTo = replyTo;
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
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    @Override
    protected void generateFields(JsonGenerator jg) throws IOException
    {
        jg.writeStringField(CHANNEL_FIELD, channel);
        jg.writeStringField(BODY_FIELD, body);
        if (identity != null)
        {
            jg.writeStringField(IDENTITY_FIELD, identity);
        }
        if (replyTo != null)
        {
            jg.writeStringField(REPLY_TO_FIELD, replyTo);
        }
    }
}
