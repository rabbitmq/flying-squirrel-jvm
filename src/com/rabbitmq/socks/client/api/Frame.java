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

    protected abstract void handleField(String _fieldName, JsonParser _jp)
        throws IOException;

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
