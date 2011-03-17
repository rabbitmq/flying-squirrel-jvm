package com.rabbitmq.socks.client.api;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonToken;

/**
 *
 * @author mbridgen
 *
 */
public abstract class Frame
{
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
        while (jp.nextToken() != JsonToken.END_OBJECT)
        {
            String fieldName = jp.getCurrentName();
            jp.nextToken();            
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
            else if ("connect".equals(fieldName))
            {
                connect = jp.getText();
            }
        }        
        if (connect != null)
        {
        	return new Connect(connect);
        }
        else
        {
        	return new Message(channel, reply, identity, message);				
        }
    }
    
    public boolean isConnect()
    {
    	return false;
    }

}
