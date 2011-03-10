package com.rabbitmq.socks.client.api;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import com.rabbitmq.socks.api.WebsocketListener;
import com.rabbitmq.socks.websocket.Websocket;
import com.rabbitmq.socks.websocket.impl.WebsocketImpl;

/**
 * 
 * @author tfox
 *
 */
public class ConnectionImpl implements Connection, WebsocketListener
{
    private final Websocket ws;
    
    public ConnectionImpl(final URI uri, final Executor executor)
    {
        ws = new WebsocketImpl(uri, executor);        
        ws.setListener(this);
    }
    
    public void connect(String ticket) throws IOException
    { 
        ws.connect();
        ws.send(ticket);
    }
    
    private Map<String, ChannelListener> listeners = 
        new ConcurrentHashMap<String, ChannelListener>();

    public void setChannelListener(String channelName, ChannelListener listener)
    {
        listeners.put(channelName, listener);
    }

    public void send(Message message) throws IOException
    {
        ws.send(message.toJSON());
    }
    
    public void close() throws IOException
    {
        ws.close();
    }
    
    public void onMessage(final String json)
    {
        Message msg = new Message();        
        try
        {
            msg.fromJSON(json);        
            ChannelListener listener = listeners.get(msg.getChannelName());  
            
            if (listener != null)
            {
                listener.onMessage(msg);
            }
        }
        catch (IOException e)
        {
            System.err.println("Failed to read message " + e.getMessage());
        }        
    }
}
