package com.rabbitmq.socks.test.client;

import junit.framework.TestCase;

import com.rabbitmq.socks.client.api.Connect;
import com.rabbitmq.socks.client.api.Message;
import com.rabbitmq.socks.client.api.impl.Frame;

public class FrameTest extends TestCase
{
    public void testConnectRoundtrip() throws Exception
    {
        Connect connect = new Connect("hereisaticket");
        Connect connect2 = (Connect)Frame.fromJSON(connect.toJSON());
        assertEquals(connect.getConnect(), connect2.getConnect());
    }

    public void testConnectEncoding() throws Exception
    {
        Connect c = new Connect("here\"}[\"isatick]t");
        Connect c2 = (Connect)Frame.fromJSON(c.toJSON());
        assertEquals(c.getConnect(), c2.getConnect());
    }

    private void messageRoundtrip(String channel,
                                 String identity,
                                 String replyTo,
                                 String body) throws Exception
    {
        Message m = new Message(channel);
        m.setReplyTo(replyTo);
        m.setBody(body);
        m.setIdentity(identity);

        Message m2 = (Message) Frame.fromJSON(m.toJSON());
        assertEquals(m.getChannelName(), m2.getChannelName());
        assertEquals(m.getReplyTo(), m2.getReplyTo());
        assertEquals(m.getIdentity(), m2.getIdentity());
        assertEquals(m.getBody(), m2.getBody());
    }

    public void testMessageRoundtrip() throws Exception
    {
        messageRoundtrip("channel", "identity", "reply-to", "body");
    }

    public void testMessageEncoding() throws Exception
    {
        messageRoundtrip("chan\"}:[nel", "iden\"}:[tity",
                         "re}:[\"ply", "bo]\"{:dy");
    }
}
