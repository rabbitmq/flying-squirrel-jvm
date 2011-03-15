package com.rabbitmq.socks.test.endtoend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.rabbitmq.socks.api.ChannelType;
import com.rabbitmq.socks.api.EndpointInfo;
import com.rabbitmq.socks.api.RabbitSocksAPI;
import com.rabbitmq.socks.api.RabbitSocksAPIFactory;
import com.rabbitmq.socks.client.api.ChannelListener;
import com.rabbitmq.socks.client.api.Connection;
import com.rabbitmq.socks.client.api.Message;
import com.rabbitmq.socks.test.APITestBase;
import com.rabbitmq.socks.websocket.impl.WebsocketImpl;

/**
 *
 * @author tfox
 *
 */
public class EndToEndTest extends APITestBase
{
    private RabbitSocksAPI api;

    private static final String IDENTITY = "joe bloggs";
    private static final Object FOO = new Object();

    private final List<Runnable> asserts = new ArrayList<Runnable>();

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        deleteAllEndpoints();
        api = getAPI();
    }

    @Override
    protected void tearDown() throws Exception
    {
    	waitForConnections(api, null, 0);
        super.tearDown();
    }

    private String genTicket(String endpointName) throws Exception
    {
        return api.generateTicket(endpointName, IDENTITY, 1000);
    }

    public void testPubSubSameWebsocket() throws Exception
    {
        Connection conn = null;
        try
        {
            EndpointInfo endpoint = RabbitSocksAPIFactory.getEndpointBuilder()
                                                     .buildEndpoint("pub-sub-endpoint-0");
            endpoint.putChannelDefinition("ch-pub", ChannelType.PUB, "topic1");
            endpoint.putChannelDefinition("ch-sub", ChannelType.SUB, "topic1");
            endpoint = api.createEndpoint(endpoint);
            String url = endpoint.getProtocols().get("websockets");
            assertNotNull(url);
            String ticket = genTicket("pub-sub-endpoint-0");
            conn = createConnection(url, ticket);
            final int numMessages = 100;
            final CountDownLatch latch = new CountDownLatch(numMessages);
            conn.setChannelListener("ch-sub", new ChannelListener()
            {
                volatile int count;

                @Override
                public void onMessage(final Message msg)
                {
                    final int i = count;
                    addToAssertsList(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            assertEquals("this is a message " + i,
                                            msg.getMessage());
                            assertEquals(IDENTITY, msg.getIdentity());
                            assertEquals("ch-sub", msg.getChannelName());
                            assertNull(msg.getReply());
                        }
                    });
                    latch.countDown();
                    count++;
                }
            });
            for (int i = 0; i < numMessages; i++)
            {
                String msg = "this is a message " + i;
                Message m = new Message("ch-pub");
                m.setMessage(msg);
                conn.send(m);
            }
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            runAsserts();
        }
        finally
        {
            if (conn != null)
            {
                conn.close();
            }
        }
    }

    public void testPubSubDifferentWebsockets() throws Exception
    {
        final int numPublishers = 3;
        final int numSubscribers = 3;
        Connection[] publishers = null;
        Connection[] subscribers = null;
        try
        {
            EndpointInfo endpoint = RabbitSocksAPIFactory.getEndpointBuilder()
                                                     .buildEndpoint("pub-sub-endpoint-0");
            endpoint.putChannelDefinition("ch-pub", ChannelType.PUB, "topic1");
            endpoint.putChannelDefinition("ch-sub", ChannelType.SUB, "topic1");
            endpoint = api.createEndpoint(endpoint);
            String url = endpoint.getProtocols().get("websockets");
            assertNotNull(url);
            String ticket = genTicket("pub-sub-endpoint-0");
            publishers = new Connection[numPublishers];
            subscribers = new Connection[numSubscribers];
            final int numMessages = 10;
            for (int i = 0; i < numPublishers; i++)
            {
                publishers[i] = createConnection(url, ticket);
            }
            final Map<String, Object> msgs = new ConcurrentHashMap<String, Object>();
            final CountDownLatch[] latches = new CountDownLatch[numSubscribers];
            for (int i = 0; i < numSubscribers; i++)
            {
                subscribers[i] = createConnection(url, ticket);
                latches[i] = new CountDownLatch(numMessages * numPublishers);
                final CountDownLatch l = latches[i];
                subscribers[i].setChannelListener("ch-sub",
                                                  new ChannelListener()
                {
                    volatile int count;

                    @Override
                    public void onMessage(final Message msg)
                    {
                        addToAssertsList(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                msgs.remove(msg.getMessage());
                                assertEquals(IDENTITY, msg
                                                          .getIdentity());
                                assertEquals("ch-sub",
                                                msg.getChannelName());
                                assertNull(msg.getReply());
                            }
                        });
                        l.countDown();
                        count++;
                    }
                });
            }
            // FIXME - this sleep is currently necessary since the connection
            // setup (ticket sending) "handshake" is currently async, this will
            // be fixed soon
            Thread.sleep(2000);
            for (int i = 0; i < numPublishers; i++)
            {
                for (int j = 0; j < numMessages; j++)
                {
                    String msg = "this is a message " + i + "-" + j;
                    msgs.put(msg, FOO);
                    Message m = new Message("ch-pub");
                    m.setMessage(msg);
                    publishers[i].send(m);
                }
            }
            for (int i = 0; i < numSubscribers; i++)
            {
                assertTrue(latches[i].await(5, TimeUnit.SECONDS));
            }
            runAsserts();
            assertTrue(msgs.isEmpty());
        }
        finally
        {
            if (publishers != null)
            {
                for (int i = 0; i < numPublishers; i++)
                {
                    if (publishers[i] != null)
                    {
                        publishers[i].close();
                    }
                }
            }
            if (subscribers != null)
            {
                for (int i = 0; i < numSubscribers; i++)
                {
                    if (subscribers[i] != null)
                    {
                        subscribers[i].close();
                    }
                }
            }
        }
    }

    public void testReqRepSameWebsocketSameEndpoint() throws Exception
    {
        Connection conn = null;
        try
        {
            EndpointInfo endpoint = RabbitSocksAPIFactory.getEndpointBuilder()
                                                     .buildEndpoint("req-rep-endpoint-0");
            endpoint.putChannelDefinition("ch-req", ChannelType.REQ,
                                          "conversation-0");
            endpoint.putChannelDefinition("ch-rep", ChannelType.REP,
                                          "conversation-0");
            endpoint = api.createEndpoint(endpoint);
            String url = endpoint.getProtocols().get("websockets");
            assertNotNull(url);
            String ticket = genTicket("req-rep-endpoint-0");
            conn = createConnection(url, ticket);
            //FIXME - need to sleep for now, until synchronous connection
            //handshake is in place
            Thread.sleep(2000);
            testReqRep(conn, conn);
        }
        finally
        {
            if (conn != null)
            {
                conn.close();
            }
        }
    }

    public void testReqRepDifferentWebsocketsDifferentEndpoints()
        throws Exception
    {
        Connection connReq = null;
        Connection connRep = null;
        try
        {
            EndpointInfo endpointReq = RabbitSocksAPIFactory.getEndpointBuilder()
                                                        .buildEndpoint("req-endpoint-0");
            endpointReq.putChannelDefinition("ch-req", ChannelType.REQ,
                                             "conversation-1");
            endpointReq = api.createEndpoint(endpointReq);
            String urlReq = endpointReq.getProtocols().get("websockets");
            String ticketReq = api.generateTicket(endpointReq.getName(),
                                                  IDENTITY, 1000);

            EndpointInfo endpointRep = RabbitSocksAPIFactory.getEndpointBuilder()
                                                        .buildEndpoint("rep-endpoint-0");
            endpointRep.putChannelDefinition("ch-rep", ChannelType.REP,
                                             "conversation-1");
            endpointRep = api.createEndpoint(endpointRep);
            String urlRep = endpointRep.getProtocols().get("websockets");
            String ticketRep = api.generateTicket(endpointRep.getName(),
                                                  IDENTITY, 1000);
            connReq = createConnection(urlReq, ticketReq);
            connRep = createConnection(urlRep, ticketRep);
            // FIXME - this sleep is currently necessary since the connection
            // setup (ticket sending) "handshake" is currently async, this will
            // be fixed soon
            Thread.sleep(2000);
            testReqRep(connReq, connRep);
        }
        finally
        {
            if (connReq != null)
            {
                connReq.close();
            }
            if (connRep != null)
            {
                connRep.close();
            }
        }
    }

    public void testPushPullSameWebsocket() throws Exception
    {
        Connection conn = null;
        try
        {
            EndpointInfo endpoint = RabbitSocksAPIFactory.getEndpointBuilder()
                                                     .buildEndpoint("push-pull-endpoint-0");
            endpoint.putChannelDefinition("ch-push", ChannelType.PUSH, "queue1");
            endpoint.putChannelDefinition("ch-pull", ChannelType.PULL, "queue1");
            endpoint = api.createEndpoint(endpoint);
            String url = endpoint.getProtocols().get("websockets");
            String ticket = genTicket("push-pull-endpoint-0");
            conn = createConnection(url, ticket);
            final int numMessages = 100;
            final CountDownLatch l = new CountDownLatch(numMessages);
            conn.setChannelListener("ch-pull", new ChannelListener()
            {
                volatile int count;

                @Override
                public void onMessage(final Message msg)
                {
                    final int i = count;
                    addToAssertsList(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            assertEquals("this is message " + i,
                                            msg.getMessage());
                            assertEquals(IDENTITY, msg.getIdentity());
                            assertEquals("ch-pull", msg.getChannelName());
                            assertNull(msg.getReply());
                        }
                    });
                    l.countDown();
                    count++;
                }
            });
            for (int i = 0; i < numMessages; i++)
            {
                String msg = "this is message " + i;
                Message sent = new Message("ch-push");
                sent.setMessage(msg);
                conn.send(sent);
            }
            assertTrue(l.await(5, TimeUnit.SECONDS));
            runAsserts();
        }
        finally
        {
            if (conn != null)
            {
                conn.close();
            }
        }
    }

    public void testPushPull() throws Exception
    {
        EndpointInfo pushEndpoint = RabbitSocksAPIFactory.getEndpointBuilder()
                                                     .buildEndpoint("push-endpoint-0");
        pushEndpoint.putChannelDefinition("ch-push", ChannelType.PUSH, "queue2");
        pushEndpoint = api.createEndpoint(pushEndpoint);
        String urlPush = pushEndpoint.getProtocols().get("websockets");
        String ticketPush = genTicket("push-endpoint-0");
        EndpointInfo pullEndpoint = RabbitSocksAPIFactory.getEndpointBuilder()
                                                     .buildEndpoint("pull-endpoint-0");
        pullEndpoint.putChannelDefinition("ch-pull", ChannelType.PULL, "queue2");
        pullEndpoint = api.createEndpoint(pullEndpoint);
        String urlPull = pullEndpoint.getProtocols().get("websockets");
        String ticketPull = genTicket("pull-endpoint-0");
        final int numPushers = 3;
        final int numPullers = 10;
        Connection[] pushers = new Connection[numPushers];
        Connection[] pullers = new Connection[numPullers];
        for (int i = 0; i < numPushers; i++)
        {
            pushers[i] = createConnection(urlPush, ticketPush);
        }
        for (int i = 0; i < numPullers; i++)
        {
            pullers[i] = createConnection(urlPull, ticketPull);
        }

        testPushPull(pushers, pullers);

        for (int i = 0; i < numPushers; i++)
        {
            pushers[i].close();
        }
        for (int i = 0; i < numPullers; i++)
        {
            pullers[i].close();
        }
    }

    public void testHardCloseConnections() throws Exception
    {
    	testCloseConnections(true);
    }

    public void testCloseConnections() throws Exception
    {
    	testCloseConnections(false);
    }

    //Make sure connections are closed on server when closed from client
    private void testCloseConnections(boolean hardClose) throws Exception
    {
    	EndpointInfo endpoint = RabbitSocksAPIFactory.getEndpointBuilder()
    		.buildEndpoint("endpoint-x");
    	endpoint.putChannelDefinition("ch-push", ChannelType.PUSH, "queue2");
    	endpoint = api.createEndpoint(endpoint);
    	String url = endpoint.getProtocols().get("websockets");
        String ticket = genTicket("endpoint-x");
        final int numConnections = 10;
        Connection[] conns = new Connection[numConnections];
        for (int i = 0; i < numConnections; i++)
        {
        	conns[i] = createConnection(url, ticket);
        }
        waitForConnections(api, "endpoint-x", numConnections);
        WebsocketImpl.HARD_CLOSE = hardClose;
        try
        {
	        for (int i = 0; i < numConnections; i++)
	        {
	        	conns[i].close();
	        }
	        waitForConnections(api, "endpoint-x", 0);
        }
        finally
        {
        	WebsocketImpl.HARD_CLOSE = false;
        }
    }

    public void testInvalidTicket() throws Exception
    {
        testInvalidTicket(false);
    }

    public void testExpiredTicket() throws Exception
    {
        testInvalidTicket(true);
    }

    private void testInvalidTicket(boolean expired) throws Exception
    {
        Connection conn = null;
        try
        {
            EndpointInfo endpoint = RabbitSocksAPIFactory.getEndpointBuilder()
                                                     .buildEndpoint("pub-sub-endpoint-0");
            endpoint.putChannelDefinition("ch-pub", ChannelType.PUB, "topic1");
            endpoint.putChannelDefinition("ch-sub", ChannelType.SUB, "topic1");
            endpoint = api.createEndpoint(endpoint);
            String url = endpoint.getProtocols().get("websockets");
            String ticket;
            if (expired)
            {
                ticket = api.generateTicket(endpoint.getName(), IDENTITY, 0);
            }
            else
            {
                ticket = "invalid-ticket";
            }
            assertNotNull(url);
            conn = createConnection(url, ticket);
            final CountDownLatch latch = new CountDownLatch(1);
            conn.setChannelListener("ch-sub", new ChannelListener()
            {
                @Override
                public void onMessage(final Message msg)
                {
                    latch.countDown();
                }
            });
            Message m = new Message("ch-pub");
            m.setMessage("this is a message");
            conn.send(m);
            //Make sure message *doesn't* arrive
            assertFalse(latch.await(1, TimeUnit.SECONDS));
            runAsserts();
        }
        finally
        {
            if (conn != null)
            {
                try
                {
                    conn.close();
                }
                catch (Exception ignore)
                {
                    //Close might fail since server might close connection
                    //if ticket invalid
                }
            }
        }
    }

    private void testPushPull(final Connection[] pushers,
                              final Connection[] pullers) throws Exception
    {
        final int numMessages = 100;
        final CountDownLatch l = new CountDownLatch(pushers.length
                                                    * numMessages);
        final Map<String, Object> msgs = new ConcurrentHashMap<String, Object>();
        for (int i = 0; i < pullers.length; i++)
        {
            pullers[i].setChannelListener("ch-pull", new ChannelListener()
            {
                @Override
                public void onMessage(final Message msg)
                {
                    msgs.remove(msg.getMessage());
                    addToAssertsList(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            assertEquals(IDENTITY, msg.getIdentity());
                            assertEquals("ch-pull", msg.getChannelName());
                            assertNull(msg.getReply());
                        }
                    });
                    l.countDown();
                }
            });
        }
        for (int i = 0; i < pushers.length; i++)
        {
            for (int j = 0; j < numMessages; j++)
            {
                String msg = "this is a message" + i + "-" + j;
                msgs.put(msg, FOO);
                Message sent = new Message("ch-push");
                sent.setMessage(msg);
                pushers[i].send(sent);
            }
        }
        assertTrue(l.await(5, TimeUnit.SECONDS));
        assertTrue(msgs.isEmpty());
        runAsserts();
    }

    private void testReqRep(final Connection connReq, final Connection connRep)
        throws Exception
    {
        final int numMessages = 10;
        connRep.setChannelListener("ch-rep", new ChannelListener()
        {
            volatile int count;
            @Override
            public void onMessage(final Message msg)
            {
                // Received the request
                final int i = count;
                addToAssertsList(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        assertEquals("this is message " + i, msg.getMessage());
                        assertEquals("ch-rep", msg.getChannelName());
                        assertEquals(IDENTITY, msg.getIdentity());
                        assertNotNull(msg.getReply());
                    }
                });
                count++;
                // Send the reply
                try
                {
                    Message reply = new Message("ch-rep");
                    reply.setReply(msg.getReply());
                    reply.setMessage("this is response " + i);
                    connRep.send(reply);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });

        final CountDownLatch latch = new CountDownLatch(numMessages);
        connReq.setChannelListener("ch-req", new ChannelListener()
        {
            volatile int count;
            @Override
            public void onMessage(final Message msg)
            {
                // Received the reply
                final int i = count;
                addToAssertsList(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        assertEquals("this is response " + i, msg.getMessage());
                        assertEquals("ch-req", msg.getChannelName());
                        assertEquals(IDENTITY, msg.getIdentity());
                        assertNotNull(msg.getReply());
                    }
                });
                count++;
                latch.countDown();
            }
        });
        for (int i = 0; i < numMessages; i++)
        {
            // Send a request
            String msg = "this is message " + i;
            Message sent = new Message("ch-req");
            sent.setMessage(msg);
            connReq.send(sent);
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        runAsserts();
    }

    private synchronized void addToAssertsList(Runnable runnable)
    {
        asserts.add(runnable);
    }

    /*
     * We add asserts to a list and execute them at the end of the test. Why?
     * Because Junit asserts only work if executed on the main thread the test
     * is run on. So any asserts in ChannelListeners would not normally work
     * since they're executed on a different thread
     */
    private synchronized void runAsserts()
    {
        for (Runnable ass : asserts)
        {
            ass.run();
        }
        asserts.clear();
    }
}
