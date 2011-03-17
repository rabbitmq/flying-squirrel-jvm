package com.rabbitmq.socks.test.client;

import com.rabbitmq.socks.client.api.*;

import junit.framework.TestCase;

public class FrameTest extends TestCase
{
    public void testConnectRoundtrip() throws Exception
    {
        Connect connect = new Connect("hereisaticket");
        Connect connect2 = new Connect();
        connect2.fromJSON(connect.toJSON());
        assertEquals(connect.getTicket(), connect2.getTicket());
    }

    public void testMessageRoundtrip() throws Exception
    {
        Message m = new Message("channel");
        m.setReply("reply");
        m.setBody("body");
        m.setIdentity("ident");

        Message m2 = new Message();
        m2.fromJSON(m.toJSON());
        assertEquals(m.getChannelName(), m2.getChannelName());
        assertEquals(m.getReply(), m2.getReply());
        assertEquals(m.getIdentity(), m2.getIdentity());
        assertEquals(m.getBody(), m2.getBody());
    }
}