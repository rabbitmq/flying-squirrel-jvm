package com.rabbitmq.socks.api.test;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import com.rabbitmq.socks.api.ChannelDefinition;
import com.rabbitmq.socks.api.ChannelType;
import com.rabbitmq.socks.api.Endpoint;
import com.rabbitmq.socks.api.EndpointBuilder;
import com.rabbitmq.socks.api.RabbitSocksAPI;
import com.rabbitmq.socks.api.RabbitSocksAPIException;
import com.rabbitmq.socks.api.RabbitSocksAPIFactory;

/**
 * 
 * @author tfox
 *
 */
public class RabbitSocksAPITest extends TestCase
{
	protected void setUp() throws Exception
	{
		super.setUp();
		
		this.deleteAllEndpoints();
	}
	
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}
	
	public void testBuildEndpoint() throws Exception
	{
	    EndpointBuilder builder = RabbitSocksAPIFactory.getEndpointBuilder();	    
	    final String endpointName = "my-endpoint";
	    Endpoint endpoint = builder.buildEndpoint(endpointName);
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
	    for (Map.Entry<String, ChannelDefinition> entry:
	             endpoint.getChannelDefinitions().entrySet())
	    {
	        assertEquals("channel-" + count, entry.getKey());
	        assertEquals(getChannelType(count), entry.getValue().getType());
	        assertEquals("resource-" + count, entry.getValue().getResource());
	        count++;
	    }
	    count = 0;
	    for (Map.Entry<String, String> entry:
	            endpoint.getProtocolURLMap().entrySet())
	    {
	        assertEquals("protocol-" + count, entry.getKey());
	        assertEquals("url-" + count, entry.getValue());
	        count++;
	    }
	}
	
	public void testCreateEndpoint() throws Exception
	{
		String endpointName = "endpoint-1";		
		Endpoint endpoint1 = genEndpoint(endpointName, 10);				
		RabbitSocksAPI api = getAPI();	
		api.createEndpoint(endpoint1);		
		//Creating again with exact same definition should succeed		
		api.createEndpoint(endpoint1);
		api.createEndpoint(endpoint1);		
		//Create a new endpoint with same name but different definitions
		//Should fail		
		Endpoint endpoint2 = genEndpoint(endpointName, 5);   	
		try
		{
			api.createEndpoint(endpoint2);
			fail("Should throw exception");
		}
		catch (RabbitSocksAPIException e)
		{
			assertEquals(409, e.getResponseCode());
		}
		//Get the endpoint and assert it's the same as the one we created		
		Endpoint endpoint3 = api.getEndpoint(endpointName);		
		assertNotNull(endpoint3);		
		assertEquals(endpoint1.getName(), endpoint3.getName());		
		assertSameChannelMaps(endpoint1.getChannelDefinitions(),
		                      endpoint3.getChannelDefinitions());		
		assertNotNull(endpoint3.getKey());		
		String url = endpoint3.getProtocolURLMap().get("websockets");		
		assertNotNull(url);		
	}
	
	public void testListEndpointNames() throws Exception
	{
		RabbitSocksAPI api = getAPI();
		final int count = 10;
		createEndpoints(api, count);
		List<String> endpointNames = api.listEndpointNames();		
		assertNotNull(endpointNames);		
		assertEquals(10, endpointNames.size());		
		Set<String> names = new HashSet<String>();
		for (int i = 0; i < count; i++)
		{
			names.add("endpoint-" + i);
		}		
		for (String endpointName: endpointNames)
		{
			names.remove(endpointName);
		}		
		assertTrue(names.isEmpty());
    }
	
	public void testDeleteEndpoint() throws Exception
	{
		RabbitSocksAPI api = getAPI();
		final int count = 10;
		createEndpoints(api, count);		
		//Delete them one-by-one		
		for (int i = 0; i < count; i++)
		{
			api.deleteEndpoint("endpoint-" + i);		
			List<String> endpointNames = api.listEndpointNames();			
			assertNotNull(endpointNames);			
			assertEquals(10 - (i + 1), endpointNames.size());			
			Set<String> names = new HashSet<String>();
			for (int j = i + 1; j < 10; j++)
			{
				names.add("endpoint-" + j);
			}			
			for (String endpointName: endpointNames)
			{
				names.remove(endpointName);
			}			
			assertTrue(names.isEmpty());
		}		
	}
	
	public void testGetEndpoint() throws Exception
	{
		RabbitSocksAPI api = RabbitSocksAPIFactory.getClient("localhost", 55672);
		final int count = 10;
		Endpoint[] endpoints = createEndpoints(api, count);		
		for (int i = 0; i < count; i++)
		{
			Endpoint endpoint = api.getEndpoint("endpoint-" + i);			
			assertEquals("endpoint-" + i, endpoint.getName());			
			assertSameChannelMaps(endpoints[i].getChannelDefinitions(), endpoint.getChannelDefinitions());
		}		
		try
		{
			api.getEndpoint(null);
			fail("Should throw exception");
		}
		catch (RabbitSocksAPIException e)
		{
			assertEquals(404, e.getResponseCode());
		}		
		try
		{
			api.getEndpoint("does-not-exist");
		}
		catch (RabbitSocksAPIException e)
		{
			assertEquals(404, e.getResponseCode());
		}		
    }
	
	public void testGenerateTicket() throws Exception
	{
		RabbitSocksAPI api = getAPI();		
		try
		{
			api.generateTicket("does-not-exist", "joe bloggs",
					System.currentTimeMillis() + 10000);
		}
		catch (RabbitSocksAPIException e)
		{
			assertEquals(404, e.getResponseCode());
		}
		final String endpointName = "endpoint-0";
		Endpoint endpoint = genEndpoint(endpointName, 10);
		api.createEndpoint(endpoint);
		final int count = 10;
		for (int i = 0; i < count; i++)
		{		
			String ticket = api.generateTicket(endpointName, "joe bloggs",
					System.currentTimeMillis() + 10000);
			
			assertNotNull(ticket);
		}
	}
	
	public void testListConnectionsForEndpoint() throws Exception
	{
		RabbitSocksAPI api = getAPI();		
		try
		{
			api.listConnectionsForEndpoint("does-not-exist");
		}
		catch (RabbitSocksAPIException e)
		{
			assertEquals(404, e.getResponseCode());
		}				
		final String endpointName = "endpoint-0";		
		Endpoint endpoint = genEndpoint(endpointName, 10);
		api.createEndpoint(endpoint);		
		List<String> conns = api.listConnectionsForEndpoint(endpointName);		
		assertTrue(conns.isEmpty());		
		Endpoint endpoint2 = api.getEndpoint(endpointName);		
		String url = endpoint2.getProtocolURLMap().get("websockets");
		assertNotNull(url);		
		String ticket = api.generateTicket(endpointName, "joe bloggs", 1000);
		final int numWS = 10;		
		Websocket[] wsa = new Websocket[numWS];
		for (int i = 0; i < numWS; i++)
		{		
		    wsa[i] = this.createWebsocket(url);
		}		
		conns = api.listConnectionsForEndpoint(endpointName); 		
		//Connection shouldn't be visible as a connection until ticket is sent
		assertTrue(conns.isEmpty());		
		for (int i = 0; i < numWS; i++)
        {
		    //First thing we do is send the ticket
		    wsa[i].send(ticket);		    
        }		
		waitForConnections(api, endpointName, numWS);			
		for (int i = 0; i < numWS; i++)
        {
		    wsa[i].close();
        }		
		//FIXME - we should wait for connections to close too - but right
		//now connection close code is not handled properly on the server
		//this.waitForConnections(api, endpointName, 0);
	}
	
	private void sendMessage(final Websocket ws, final String channelName,
	                         final String message)
	    throws IOException
	{
	    StringBuffer buff = new StringBuffer("{'channel':'");
	    buff.append(channelName).append("',message:'").append(message).append("'}");
	    ws.send(buff.toString());
	}
	
	/*
	 * Waiting for connections after sending a ticket will take an
	 * indeterminate amount of time, so we retry in a loop and
	 * time out 
	 */
	private void waitForConnections(final RabbitSocksAPI api, final String endpointName,
	                                final int count)	
	    throws Exception
	{
	    final long timeout = 5000;
	    long start = System.currentTimeMillis();
	    do
	    {
	        List<String> conns = api.listConnectionsForEndpoint(endpointName);
	        
	        if (conns.size() == count)
	        {
	            return;
	        }
	        
	        Thread.sleep(10);
	    }
	    while (System.currentTimeMillis() - start < timeout);
	    fail("Timedout waiting for connections " + count);
	}
		
	private void assertSameChannelMaps(final Map<String, ChannelDefinition> defs1,
							           final Map<String, ChannelDefinition> defs2)
	{
		assertEquals(defs1.size(), defs2.size());

		for (Map.Entry<String, ChannelDefinition> entry: defs1.entrySet())
		{
		    ChannelDefinition otherDef = defs2.get(entry.getKey());
		    assertNotNull(otherDef);
		    assertEquals(entry.getValue(), otherDef);
		}
	}
	
	private void deleteAllEndpoints() throws Exception
	{
		RabbitSocksAPI api = RabbitSocksAPIFactory.getClient("localhost", 55672);
		
		List<String> endpointNames = api.listEndpointNames();
		
		for (String endpointName: endpointNames)
		{
			api.deleteEndpoint(endpointName);
		}
	}
	
	private Endpoint[] createEndpoints(final RabbitSocksAPI api, final int count)
       throws Exception
    {  
       Endpoint[] endpoints = new Endpoint[10];
       for (int i = 0; i < count; i++)
       {
           endpoints[i] = genEndpoint("endpoint-" + i, 10);           
           api.createEndpoint(endpoints[i]);
       }       
       return endpoints;
    }
	
    private Endpoint genEndpoint(final String endpointName, final int numChannels)
    {
        Endpoint endpoint =
            RabbitSocksAPIFactory.getEndpointBuilder().buildEndpoint(endpointName);        
        for (int i = 0; i < numChannels; i++)
        {
            endpoint.putChannelDefinition("channel-" + i, getChannelType(i),
                                          "resource-" + i);
        }        
        return endpoint;
    }
    
    private RabbitSocksAPI getAPI()
    {
        return RabbitSocksAPIFactory.getClient("localhost", 55672);
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
    
    private Websocket createWebsocket(final String surl) throws Exception
    {
        URI uri = new URI(surl);
        Websocket ws = new Websocket(uri);
        ws.connect();
        return ws;
    }
    
    private void dumpProtocolMap(Map<String, String> protocolMap)
    {
        for (Map.Entry<String, String> entry: protocolMap.entrySet())
        {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
    }
    
    
}
