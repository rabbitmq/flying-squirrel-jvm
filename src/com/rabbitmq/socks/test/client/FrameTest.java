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

    public void testConnectEncoding() throws Exception
    {
        Connect c = new Connect("here\"}[\"isatick]t");
        Connect c2 = new Connect();
        c2.fromJSON(c.toJSON());
        assertEquals(c.getTicket(), c2.getTicket());
    }

    private void messageRoundtrip(String channel,
                                 String identity,
                                 String reply,
                                 String body) throws Exception
    {
        Message m = new Message(channel);
        m.setReply(reply);
        m.setBody(body);
        m.setIdentity(identity);

        Message m2 = new Message();
        m2.fromJSON(m.toJSON());
        assertEquals(m.getChannelName(), m2.getChannelName());
        assertEquals(m.getReply(), m2.getReply());
        assertEquals(m.getIdentity(), m2.getIdentity());
        assertEquals(m.getBody(), m2.getBody());
    }

    public void testMessageRoundtrip() throws Exception
    {
        messageRoundtrip("channel", "identity", "reply", "body");
    }

    public void testMessageEncoding() throws Exception
    {
        messageRoundtrip("chan\"}:[nel", "iden\"}:[tity",
                         "re}:[\"ply", "bo]\"{:dy");
    }
}
