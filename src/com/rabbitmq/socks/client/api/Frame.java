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
public abstract class Frame
{

    protected abstract void handleField(String fieldName, JsonParser jp)
        throws IOException;

    public abstract String toJSON();

    public void fromJSON(final String json) throws IOException
    {
        JsonFactory factory = new JsonFactory();
        JsonParser jp = factory.createJsonParser(new StringReader(json));
        jp.nextToken();
        while (jp.nextToken() != JsonToken.END_OBJECT)
        {
            String fieldName = jp.getCurrentName();
            jp.nextToken();
            handleField(fieldName, jp);
        }
    }

}
