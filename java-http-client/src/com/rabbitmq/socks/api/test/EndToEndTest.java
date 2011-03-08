package com.rabbitmq.socks.api.test;

import com.rabbitmq.socks.api.ChannelType;
import com.rabbitmq.socks.api.Endpoint;
import com.rabbitmq.socks.api.RabbitSocksAPI;
import com.rabbitmq.socks.api.RabbitSocksAPIFactory;

/**
 * 
 * @author tfox
 *
 */
public class EndToEndTest extends APITestBase
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
    
    public void testPubSub() throws Exception
    {
        RabbitSocksAPI api = getAPI();                        
        final String endpointName = "pub-sub";            
        Endpoint endpoint =
            RabbitSocksAPIFactory.getEndpointBuilder().buildEndpoint(endpointName);        
        endpoint.putChannelDefinition("ch-pub", ChannelType.PUB, "foo");
        endpoint.putChannelDefinition("ch-sub", ChannelType.SUB, "foo");        
        api.createEndpoint(endpoint);
        Endpoint endpoint2 = api.getEndpoint(endpointName); 
        String url = endpoint2.getProtocolURLMap().get("websockets");
        assertNotNull(url);  
        String ticket = api.generateTicket(endpointName, "joe bloggs", 1000);
        Websocket ws = createWebsocket(url);          
        ws.send(ticket);
        final int numMessages = 1000;
                       
        for (int i = 0; i < numMessages; i++)
        {
            String msg = "this is a message " + i;
            sendMessage(ws, "ch-pub", msg);
        }
        
        for (int i = 0; i < numMessages; i++)
        {
            String received = ws.recv();
            
            //System.out.println(received);
            
            assertNotNull(received);
        }        
        ws.close();
    }
    
    
    
    
}
