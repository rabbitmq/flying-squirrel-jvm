package com.rabbitmq.socks.client.api.impl;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;

/**
 *
 * @author tfox
 *
 */
public class Error extends Frame
{
    private final String errorCode;

    public Error(final String errorCode)
    {
        super(FrameType.ERROR);
        this.errorCode = errorCode;
    }

    public String getErrorCode()
    {
        return errorCode;
    }

    @Override
    public void generateFields(JsonGenerator jg) throws IOException
    {
        jg.writeStringField(ERROR_CODE_FIELD, errorCode);
    }
}
