package com.rabbitmq.socks.client.api.impl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import com.rabbitmq.socks.client.api.Connect;
import com.rabbitmq.socks.client.api.Message;

/**
 *
 * @author mbridgen
 *
 */
public abstract class Frame
{
    public final static String CHANNEL_FIELD = "channel";
    public final static String BODY_FIELD = "body";
    public final static String IDENTITY_FIELD = "identity";
    public final static String REPLY_TO_FIELD = "reply-to";
    public final static String CONNECT_FIELD = "connect";
    public final static String ERROR_CODE_FIELD = "error-code";

    private final FrameType frameType;

    public Frame(final FrameType frameType)
    {
        this.frameType = frameType;
    }

    protected abstract void generateFields(JsonGenerator _jg)
        throws IOException;

    public String toJSON() throws IOException
    {
        JsonFactory factory = new JsonFactory();
        StringWriter writer = new StringWriter();
        JsonGenerator jg = factory.createJsonGenerator(writer);
        jg.writeStartObject();
        generateFields(jg);
        jg.writeEndObject();
        jg.close();
        return writer.toString();
    }

    public static Frame fromJSON(final String json) throws IOException
    {
        JsonFactory factory = new JsonFactory();
        JsonParser jp = factory.createJsonParser(new StringReader(json));
        jp.nextToken();
        String channel = null;
        String reply = null;
        String identity = null;
        String message = null;
        String connect = null;
        String errorCode = null;
        while (jp.nextToken() != JsonToken.END_OBJECT)
        {
            String fieldName = jp.getCurrentName();
            jp.nextToken();
            if (CHANNEL_FIELD.equals(fieldName))
            {
                channel = jp.getText();
            }
            else if (BODY_FIELD.equals(fieldName))
            {
                message = jp.getText();
            }
            else if (IDENTITY_FIELD.equals(fieldName))
            {
                identity = jp.getText();
            }
            else if (REPLY_TO_FIELD.equals(fieldName))
            {
                reply = jp.getText();
            }
            else if (CONNECT_FIELD.equals(fieldName))
            {
                connect = jp.getText();
            }
            else if (ERROR_CODE_FIELD.equals(fieldName))
            {
                errorCode = jp.getText();
            }
        }
        if (connect != null)
        {
        	return new Connect(connect);
        }
        else if (errorCode != null)
        {
            return new Error(errorCode);
        }
        else
        {
        	return new Message(channel, reply, identity, message);
        }
    }

    public FrameType getFrameType()
    {
    	return frameType;
    }

}
