package com.rabbitmq.socks.client.api;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;

import com.rabbitmq.socks.client.api.impl.Frame;
import com.rabbitmq.socks.client.api.impl.FrameType;

/**
 *
 * @author mbridgen
 *
 */
public class Connect extends Frame
{
    private final String connect;

    public Connect(final String connect)
    {
        super(FrameType.CONNECT);
        this.connect = connect;
    }

    public String getConnect()
    {
        return connect;
    }

    @Override
    public void generateFields(JsonGenerator jg) throws IOException
    {
        jg.writeStringField(CONNECT_FIELD, connect);
    }
}
