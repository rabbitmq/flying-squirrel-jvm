package com.rabbitmq.socks.client.api.impl;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.rabbitmq.socks.client.api.ChannelListener;
import com.rabbitmq.socks.client.api.Connect;
import com.rabbitmq.socks.client.api.Connection;
import com.rabbitmq.socks.client.api.Frame;
import com.rabbitmq.socks.client.api.Message;
import com.rabbitmq.socks.websocket.Websocket;
import com.rabbitmq.socks.websocket.WebsocketListener;
import com.rabbitmq.socks.websocket.impl.WebsocketImpl;

/**
 *
 * @author tfox
 *
 */
public class ConnectionImpl implements Connection, WebsocketListener
{
    private final Websocket ws;
    
    private CountDownLatch connectLatch = new CountDownLatch(1);
    
    private volatile String connect;

    public ConnectionImpl(final URI uri, final Executor executor)
    {
        ws = new WebsocketImpl(uri, executor);
        ws.setListener(this);
    }

    @Override
    public void connect(String ticket) throws IOException
    {
        ws.connect();
        ws.send(new Connect(ticket).toJSON());
        while (true)
        {
        	try
        	{
		        if (!connectLatch.await(5, TimeUnit.SECONDS))
		        {
		        	throw new IOException("Timed out waiting for connect response from server");
		        }
		        else
		        {
		        	break;
		        }
        	}
        	catch (InterruptedException e)
        	{
        		//Ignore - this can happen- we need to keep waiting
        	}
        }
        if (!connect.equals("ok"))
        {
        	throw new IOException("Failed to connect to server: " + connect);
        }
    }

    private final Map<String, ChannelListener> listeners =
    	new ConcurrentHashMap<String, ChannelListener>();

    @Override
    public void setChannelListener(String channelName, ChannelListener listener)
    {
        listeners.put(channelName, listener);
    }

    @Override
    public void send(Message message) throws IOException
    {
        ws.send(message.toJSON());
    }

    @Override
    public void close() throws IOException
    {
        ws.close();
    }

    @Override
    public void onMessage(final String json)
    {        
        try
        {
        	Frame frame = Frame.fromJSON(json);        	
        	if (frame.isConnect())
        	{
        		//TODO - the connect field can pass an error message to the user
        		connect = ((Connect)frame).getConnect();
        		connectLatch.countDown();        		
        	}
        	else
        	{
        		Message msg = (Message)frame;
                ChannelListener listener = listeners.get(msg.getChannelName());
                if (listener != null)
                {
                    listener.onMessage(msg);
                }
        	}
        }
        catch (IOException e)
        {
            System.err.println("Failed to read message " + e.getMessage());
        }
    }
}
