package com.rabbitmq.socks.test.api;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.rabbitmq.socks.api.ChannelDefinition;
import com.rabbitmq.socks.api.ChannelType;
import com.rabbitmq.socks.api.ConnectionInfo;
import com.rabbitmq.socks.api.EndpointBuilder;
import com.rabbitmq.socks.api.EndpointInfo;
import com.rabbitmq.socks.api.RabbitSocksAPI;
import com.rabbitmq.socks.api.RabbitSocksAPIException;
import com.rabbitmq.socks.api.RabbitSocksAPIFactory;
import com.rabbitmq.socks.client.api.Connection;
import com.rabbitmq.socks.client.api.ErrorListener;
import com.rabbitmq.socks.client.api.Message;
import com.rabbitmq.socks.client.api.impl.ConnectionImpl;
import com.rabbitmq.socks.test.APITestBase;
import com.rabbitmq.socks.websocket.Websocket;

/**
 *
 * @author tfox
 *
 */
public class APITest extends APITestBase
{
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        this.deleteAllEndpoints();
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testBuildEndpoint() throws Exception
    {
        EndpointBuilder builder = RabbitSocksAPIFactory.getEndpointBuilder();
        final String endpointName = "my-endpoint";
        EndpointInfo endpoint = builder.buildEndpoint(endpointName);
        int channelCount = 10;
        for (int i = 0; i < channelCount; i++)
        {
            endpoint = endpoint.putChannelDefinition("channel-" + i,
                                                     getChannelType(i),
                                                     "resource-" + i);
        }
        int urlCount = 10;
        for (int i = 0; i < urlCount; i++)
        {
            endpoint = endpoint.putProtocolURL("protocol-" + i, "url-" + i);
        }
        assertEquals(endpointName, endpoint.getName());
        assertEquals(channelCount, endpoint.getChannelDefinitions().size());
        int count = 0;
        for (Map.Entry<String, ChannelDefinition> entry : endpoint
                                                                  .getChannelDefinitions()
                                                                  .entrySet())
        {
            assertEquals("channel-" + count, entry.getKey());
            assertEquals(getChannelType(count), entry.getValue().getType());
            assertEquals("resource-" + count, entry.getValue().getResource());
            count++;
        }
        count = 0;
        for (Map.Entry<String, String> entry : endpoint.getProtocols()
                                                       .entrySet())
        {
            assertEquals("protocol-" + count, entry.getKey());
            assertEquals("url-" + count, entry.getValue());
            count++;
        }
    }

    public void testCreateEndpoint() throws Exception
    {
        String endpointName = "endpoint-1";
        EndpointInfo endpoint1 = genEndpoint(endpointName, 10);
        RabbitSocksAPI api = getAPI();
        EndpointInfo endpoint_ret = api.createEndpoint(endpoint1);
        assertEndpoint(endpoint1, endpoint_ret);
        // Creating again with exact same definition should succeed
        endpoint_ret = api.createEndpoint(endpoint1);
        assertEndpoint(endpoint1, endpoint_ret);
        endpoint_ret = api.createEndpoint(endpoint1);
        assertEndpoint(endpoint1, endpoint_ret);
        // Create a new endpoint with same name but different definitions
        // Should fail
        EndpointInfo endpoint2 = genEndpoint(endpointName, 5);
        try
        {
            api.createEndpoint(endpoint2);
            failNoException();
        }
        catch (RabbitSocksAPIException e)
        {
            assertEquals(409, e.getResponseCode());
        }
        // Get the endpoint and assert it's the same as the one we created
        endpoint_ret = api.getEndpoint(endpointName);
        assertEndpoint(endpoint1, endpoint_ret);
        try
        {
            api.createEndpoint(null);
            failNoException();
        }
        catch (IllegalArgumentException e)
        {
        }
        try
        {
            EndpointInfo endpointNullName = RabbitSocksAPIFactory
                                                                 .getEndpointBuilder()
                                                                 .buildEndpoint(null);
            api.createEndpoint(endpointNullName);
            failNoException();
        }
        catch (IllegalArgumentException e)
        {
        }
        try
        {
            EndpointInfo endpointNullName = RabbitSocksAPIFactory
                                                                 .getEndpointBuilder()
                                                                 .buildEndpoint("");
            api.createEndpoint(endpointNullName);
            failNoException();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testListEndpoints() throws Exception
    {
        RabbitSocksAPI api = getAPI();
        final int count = 10;
        EndpointInfo[] endpoints = createEndpoints(api, count);
        List<EndpointInfo> endpointsRet = api.listEndpoints();
        assertNotNull(endpoints);
        assertEquals(count, endpointsRet.size());
        Map<String, EndpointInfo> map = new HashMap<String, EndpointInfo>();
        for (EndpointInfo ei : endpointsRet)
        {
            map.put(ei.getName(), ei);
        }
        for (EndpointInfo ei : endpoints)
        {
            EndpointInfo ei2 = map.get(ei.getName());
            assertNotNull(ei2);
            assertEndpoint(ei, ei2);
        }
    }

    public void testDeleteEndpoint() throws Exception
    {
        RabbitSocksAPI api = getAPI();
        final int count = 10;
        createEndpoints(api, count);
        // Delete them one-by-one
        for (int i = 0; i < count; i++)
        {
            api.deleteEndpoint("endpoint-" + i);
            List<EndpointInfo> endpoints = api.listEndpoints();
            assertNotNull(endpoints);
            assertEquals(10 - (i + 1), endpoints.size());
            Set<String> names = new HashSet<String>();
            for (int j = i + 1; j < 10; j++)
            {
                names.add("endpoint-" + j);
            }
            for (EndpointInfo endpoint : endpoints)
            {
                names.remove(endpoint.getName());
            }
            assertTrue(names.isEmpty());

            //TODO !! test that any connections on the endpoint get closed too
        }
        try
        {
            api.deleteEndpoint(null);
            failNoException();
        }
        catch (IllegalArgumentException e)
        {
        }
        try
        {
            api.deleteEndpoint("");
            failNoException();
        }
        catch (IllegalArgumentException e)
        {
        }
        try
        {
            api.deleteEndpoint("does-not-exist");
            failNoException();
        }
        catch (RabbitSocksAPIException e)
        {
            assertEquals(404, e.getResponseCode());
        }
    }

    public void testGetEndpoint() throws Exception
    {
        RabbitSocksAPI api = getAPI();
        final int count = 10;
        EndpointInfo[] endpoints = createEndpoints(api, count);
        for (int i = 0; i < count; i++)
        {
            EndpointInfo endpoint = api.getEndpoint(endpoints[i].getName());
            assertEndpoint(endpoints[i], endpoint);
        }
        try
        {
            api.getEndpoint(null);
            failNoException();
        }
        catch (IllegalArgumentException e)
        {
        }
        try
        {
            api.getEndpoint("");
            failNoException();
        }
        catch (IllegalArgumentException e)
        {
        }
        try
        {
            api.getEndpoint("does-not-exist");
            failNoException();
        }
        catch (RabbitSocksAPIException e)
        {
            assertEquals(404, e.getResponseCode());
        }
    }

    public void testGenerateTicket() throws Exception
    {
        RabbitSocksAPI api = getAPI();
        final String endpointName = "endpoint-0";
        EndpointInfo endpoint = genEndpoint(endpointName, 10);
        api.createEndpoint(endpoint);
        final int count = 10;
        for (int i = 0; i < count; i++)
        {
            String ticket = api.generateTicket(endpointName,
                                               "joe bloggs",
                                               System.currentTimeMillis() + 10000);

            assertNotNull(ticket);
        }
        try
        {
            api.generateTicket("does-not-exist", "joe bloggs",
                               System.currentTimeMillis() + 10000);
            failNoException();
        }
        catch (RabbitSocksAPIException e)
        {
            assertEquals(404, e.getResponseCode());
        }
        try
        {
            api.generateTicket("does-not-exist", null,
                               System.currentTimeMillis() + 10000);
            failNoException();
        }
        catch (IllegalArgumentException e)
        {
        }
        try
        {
            api.generateTicket("does-not-exist", "",
                               System.currentTimeMillis() + 10000);
            failNoException();
        }
        catch (IllegalArgumentException e)
        {
        }
        try
        {
            api.generateTicket("endpoint-0", "",
                               System.currentTimeMillis() + 10000);
            failNoException();
        }
        catch (IllegalArgumentException e)
        {
        }
        try
        {
            api.generateTicket("endpoint-0", null,
                               System.currentTimeMillis() + 10000);
            failNoException();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testListConnectionsForEndpoint() throws Exception
    {
        RabbitSocksAPI api = getAPI();
        final String endpointName = "endpoint-0";
        EndpointInfo endpoint = genEndpoint(endpointName, 10);
        endpoint = api.createEndpoint(endpoint);
        List<ConnectionInfo> connInfos =
            api.listConnectionsForEndpoint(endpointName);
        assertTrue(connInfos.isEmpty());
        String url = endpoint.getProtocols().get("websockets");
        assertNotNull(url);
        String ticket = api.generateTicket(endpointName, "joe bloggs", 1000);
        final int numConns = 10;
        Connection[] conns = new Connection[numConns];
        for (int i = 0; i < numConns; i++)
        {
            conns[i] = createConnection(url, ticket);
        }
        waitForConnections(api, endpointName, numConns);
        connInfos = api.listConnectionsForEndpoint(endpointName);
        assertEquals(numConns, connInfos.size());
        for (ConnectionInfo info : connInfos)
        {
            assertEquals(info.getEndpointName(), endpointName);
            assertNotNull(info.getConnectionName());
            //FIXME - protocol value is currently incorrect
            //assertEquals("websockets", info.getProtocol());
        }
        for (int i = 0; i < numConns; i++)
        {
            conns[i].close();
        }
        waitForConnections(api, endpointName, 0);
        try
        {
        	connInfos = api.listConnectionsForEndpoint("does-not-exist");
        	failNoException();
        }
	    catch (RabbitSocksAPIException e)
	    {
	        assertEquals(404, e.getResponseCode());
	    }
        try
        {
            api.listConnectionsForEndpoint(null);
            failNoException();
        }
        catch (IllegalArgumentException e)
        {
        }
        try
        {
            api.listConnectionsForEndpoint("");
            failNoException();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testListConnections() throws Exception
    {
        RabbitSocksAPI api = getAPI();
        final int numEndpoints = 10;
        EndpointInfo[] endpoints = createEndpoints(api, numEndpoints);
        List<ConnectionInfo> connInfos = api.listConnections();
        assertTrue(connInfos.isEmpty());
        final int numConns = 10;
        Connection[] conns = new Connection[numEndpoints * numConns];
        for (int i = 0; i < numEndpoints; i++)
        {
            String url = endpoints[i].getProtocols().get("websockets");
            assertNotNull(url);
            String ticket = api.generateTicket(endpoints[i].getName(),
                                               "joe bloggs", 1000);
            for (int j = 0; j < numConns; j++)
            {
                conns[i * numConns + j] = createConnection(url, ticket);
            }

            waitForConnections(api, endpoints[i].getName(), numConns);
        }
        //Check that we have numConns connections per endpoint
        connInfos = api.listConnections();
        assertEquals(conns.length, connInfos.size());
        Map<String, AtomicInteger> connCounts =
            new HashMap<String, AtomicInteger>();
        for (ConnectionInfo conn: connInfos)
        {
            assertNotNull(conn.getEndpointName());
            AtomicInteger ai = connCounts.get(conn.getEndpointName());
            if (ai == null)
            {
                ai = new AtomicInteger(0);
                connCounts.put(conn.getEndpointName(), ai);
            }
            ai.incrementAndGet();
            assertNotNull(conn.getConnectionName());
            assertEquals("websockets", conn.getProtocol());
        }
        for (AtomicInteger ai: connCounts.values())
        {
            assertEquals(numConns, ai.get());
        }
        for (int i = 0; i < conns.length; i++)
        {
            conns[i].close();
        }
        // FIXME - we should wait for connections to close too - but right
        // now connection close code is not handled properly on the server
        waitForConnections(api, null, 0);
    }

    public void testExpiredTicket() throws Exception
    {
        testInvalidTicket(true);
    }

    public void testMalformedTicket() throws Exception
    {
        testInvalidTicket(false);
    }

    private void testInvalidTicket(boolean expired) throws Exception
    {
        Connection conn = null;

        RabbitSocksAPI api = getAPI();
        EndpointInfo endpoint = RabbitSocksAPIFactory.getEndpointBuilder()
                                                 .buildEndpoint("pub-sub-endpoint-0");
        endpoint.putChannelDefinition("ch-pub", ChannelType.PUB, "topic1");
        endpoint.putChannelDefinition("ch-sub", ChannelType.SUB, "topic1");
        endpoint = api.createEndpoint(endpoint);
        String url = endpoint.getProtocols().get("websockets");
        String ticket;
        if (expired)
        {
            ticket = api.generateTicket(endpoint.getName(), "joe bloggs", 0);
        }
        else
        {
            ticket = "invalid-ticket";
        }
        assertNotNull(url);
        URI uri = new URI(url);
        conn = new ConnectionImpl(uri, Executors.newSingleThreadExecutor());

        final AtomicReference<String> errorCode = new AtomicReference<String>();
        final CountDownLatch latch = new CountDownLatch(1);

        conn.setErrorListener(new ErrorListener()
        {
            @Override
            public void onError(String code)
            {
                System.out.println("got error " + code);
                errorCode.set(code);
                latch.countDown();
            }
        });

        try
        {
            conn.connect(ticket);
            failNoException();
        }
        catch (IOException e)
        {
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (!expired)
        {
            assertEquals("malformed_ticket", errorCode.get());
        }
        else
        {
            assertEquals("expired_ticket", errorCode.get());
        }

        System.out.println("Got to end");
    }

    public void testSendBeforeConnected() throws Exception
    {
        Connection conn = null;

        RabbitSocksAPI api = getAPI();
        EndpointInfo endpoint = RabbitSocksAPIFactory.getEndpointBuilder()
                                                 .buildEndpoint("pub-sub-endpoint-0");
        endpoint.putChannelDefinition("ch-pub", ChannelType.PUB, "topic1");
        endpoint.putChannelDefinition("ch-sub", ChannelType.SUB, "topic1");
        endpoint = api.createEndpoint(endpoint);
        String url = endpoint.getProtocols().get("websockets");
        URI uri = new URI(url);
        conn = new ConnectionImpl(uri, Executors.newSingleThreadExecutor());

        final AtomicReference<String> errorCode = new AtomicReference<String>();
        final CountDownLatch latch = new CountDownLatch(1);

        conn.setErrorListener(new ErrorListener()
        {
            @Override
            public void onError(String code)
            {
                System.out.println("got error " + code);
                errorCode.set(code);
                latch.countDown();
            }
        });

        //Try and send message before connection is connected with ticket
        Websocket ws = ((ConnectionImpl)conn).getWebsocket();
        ws.connect();
        Message msg = new Message("ch-pub");
        msg.setBody("foo");
        ws.send(msg.toJSON());

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("not_connected", errorCode.get());

        System.out.println("Got to end");
    }

    public void testMalformedFrameBeforeConnect() throws Exception
    {
        Connection conn = null;

        RabbitSocksAPI api = getAPI();
        EndpointInfo endpoint = RabbitSocksAPIFactory.getEndpointBuilder()
                                                 .buildEndpoint("pub-sub-endpoint-0");
        endpoint.putChannelDefinition("ch-pub", ChannelType.PUB, "topic1");
        endpoint.putChannelDefinition("ch-sub", ChannelType.SUB, "topic1");
        endpoint = api.createEndpoint(endpoint);
        String url = endpoint.getProtocols().get("websockets");
        URI uri = new URI(url);
        conn = new ConnectionImpl(uri, Executors.newSingleThreadExecutor());

        final AtomicReference<String> errorCode = new AtomicReference<String>();
        final CountDownLatch latch = new CountDownLatch(1);

        conn.setErrorListener(new ErrorListener()
        {
            @Override
            public void onError(String code)
            {
                System.out.println("got error " + code);
                errorCode.set(code);
                latch.countDown();
            }
        });

        Websocket ws = ((ConnectionImpl)conn).getWebsocket();
        ws.connect();
        //Send invalid json frame before ticket
        ws.send("}{}{}{}}{81278127812");

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("malformed_frame", errorCode.get());;
    }

    public void testMalformedFrameAfterConnect() throws Exception
    {
        Connection conn = null;

        RabbitSocksAPI api = getAPI();
        EndpointInfo endpoint = RabbitSocksAPIFactory.getEndpointBuilder()
                                                 .buildEndpoint("pub-sub-endpoint-0");
        endpoint.putChannelDefinition("ch-pub", ChannelType.PUB, "topic1");
        endpoint.putChannelDefinition("ch-sub", ChannelType.SUB, "topic1");
        endpoint = api.createEndpoint(endpoint);
        String url = endpoint.getProtocols().get("websockets");
        URI uri = new URI(url);
        String ticket = api.generateTicket(endpoint.getName(), "joe bloggs", 1000);
        conn = new ConnectionImpl(uri, Executors.newSingleThreadExecutor());
        conn.connect(ticket);

        final AtomicReference<String> errorCode = new AtomicReference<String>();
        final CountDownLatch latch = new CountDownLatch(1);

        conn.setErrorListener(new ErrorListener()
        {
            @Override
            public void onError(String code)
            {
                System.out.println("got error " + code);
                errorCode.set(code);
                latch.countDown();
            }
        });
        Websocket ws = ((ConnectionImpl)conn).getWebsocket();
        //Send invalid json frame
        ws.send("}{}{}{}}{81278127812");

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("malformed_frame", errorCode.get());;
    }

    public void testUnknownChannel() throws Exception
    {
        Connection conn = null;

        RabbitSocksAPI api = getAPI();
        EndpointInfo endpoint = RabbitSocksAPIFactory.getEndpointBuilder()
                                                 .buildEndpoint("pub-sub-endpoint-0");
        endpoint.putChannelDefinition("ch-pub", ChannelType.PUB, "topic1");
        endpoint.putChannelDefinition("ch-sub", ChannelType.SUB, "topic1");
        endpoint = api.createEndpoint(endpoint);
        String url = endpoint.getProtocols().get("websockets");
        URI uri = new URI(url);
        String ticket = api.generateTicket(endpoint.getName(), "joe bloggs", 1000);
        conn = new ConnectionImpl(uri, Executors.newSingleThreadExecutor());
        conn.connect(ticket);

        final AtomicReference<String> errorCode = new AtomicReference<String>();
        final CountDownLatch latch = new CountDownLatch(1);

        conn.setErrorListener(new ErrorListener()
        {
            @Override
            public void onError(String code)
            {
                System.out.println("got error " + code);
                errorCode.set(code);
                latch.countDown();
            }
        });

        Message msg = new Message("no-such_channel");
        conn.send(msg);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("unknown_channel", errorCode.get());;
    }

    private void assertEndpoint(final EndpointInfo endpoint1,
                    final EndpointInfo endpoint2)
    {
        assertEquals(endpoint1.getName(), endpoint2.getName());
        assertSameChannelMaps(endpoint1.getChannelDefinitions(),
                              endpoint2.getChannelDefinitions());
        assertNotNull(endpoint2.getKey());
        String url = endpoint2.getProtocols().get("websockets");
        assertNotNull(url);
    }

    private void assertSameChannelMaps(
                    final Map<String, ChannelDefinition> defs1,
                    final Map<String, ChannelDefinition> defs2)
    {
        assertEquals(defs1.size(), defs2.size());

        for (Map.Entry<String, ChannelDefinition> entry : defs1.entrySet())
        {
            ChannelDefinition otherDef = defs2.get(entry.getKey());
            assertNotNull(otherDef);
            assertEquals(entry.getValue(), otherDef);
        }
    }

    private EndpointInfo[] createEndpoints(final RabbitSocksAPI api,
        final int count)
        throws Exception
    {
        EndpointInfo[] endpoints = new EndpointInfo[count];
        for (int i = 0; i < count; i++)
        {
            endpoints[i] = api.createEndpoint(genEndpoint("endpoint-" + i, 10));
        }
        return endpoints;
    }

    private EndpointInfo genEndpoint(final String endpointName,
                    final int numChannels)
    {
        EndpointInfo endpoint = RabbitSocksAPIFactory.getEndpointBuilder()
                                                     .buildEndpoint(endpointName);
        for (int i = 0; i < numChannels; i++)
        {
            endpoint.putChannelDefinition("channel-" + i, getChannelType(i),
                                          "resource-" + i);
        }
        return endpoint;
    }

    private ChannelType getChannelType(final int i)
    {
        switch (i % 6)
        {
            case 0:
                return ChannelType.PUB;
            case 1:
                return ChannelType.PULL;
            case 2:
                return ChannelType.PUSH;
            case 3:
                return ChannelType.REP;
            case 4:
                return ChannelType.REQ;
            case 5:
                return ChannelType.SUB;
            default:
                throw new IllegalArgumentException("Never gets here");
        }
    }

}
