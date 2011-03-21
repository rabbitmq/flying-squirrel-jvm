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
import com.rabbitmq.socks.client.api.ErrorListener;
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
    private final CountDownLatch connectLatch = new CountDownLatch(1);
    //private volatile String connect;
    private volatile Error connectError;
    private volatile boolean connected;

    private final Map<String, ChannelListener> listeners =
    	new ConcurrentHashMap<String, ChannelListener>();

    private ErrorListener errorListener;

    public ConnectionImpl(final URI uri, final Executor executor)
    {
        ws = new WebsocketImpl(uri, executor);
        ws.setListener(this);
    }

    @Override
    public synchronized void connect(final String ticket) throws IOException
    {
    	if (connected)
    	{
    		throw new IOException("Connection cannot be connected more than once");
    	}
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
        if (connectError != null)
        {
        	throw new IOException("Failed to connect to server: " + formatErrorString(connectError));
        }
        connected = true;
    }

    @Override
    public synchronized void setChannelListener(final String channelName, final ChannelListener listener)
    {
        listeners.put(channelName, listener);
    }

    @Override
    public void setErrorListener(final ErrorListener listener)
    {
        this.errorListener = listener;
    }

    @Override
    public synchronized void send(final Message message) throws IOException
    {
    	if (!connected)
    	{
    		throw new IOException("Connection is not yet connected");
    	}
        ws.send(message.toJSON());
    }

    @Override
    public synchronized void close() throws IOException
    {
        ws.close();
    }

    @Override
    public void onMessage(final String json)
    {
        //System.out.println("Got json: " + json);
        try
        {
        	Frame frame = Frame.fromJSON(json);
        	switch (frame.getFrameType())
        	{
        	    case MESSAGE:
                {
                    if (!connected)
                    {
                        throw new IOException("Message received before connected");
                    }
                    Message msg = (Message)frame;

                    ChannelListener listener =
                        listeners.get(msg.getChannelName());
                    if (listener != null)
                    {
                        listener.onMessage(msg);
                    }
                }
        	    case CONNECT:
        	    {
                    connectLatch.countDown();
                    break;
        	    }
        	    case ERROR:
        	    {
        	        Error error = (Error)frame;
        	        if (!connected)
        	        {
        	            connectError = error;
        	            connectLatch.countDown();
        	        }
        	        if (errorListener == null)
        	        {
        	            System.err.println(formatErrorString(error));
        	        }
        	        else
        	        {
        	            errorListener.onError(error.getErrorCode());
        	        }
        	    }
        	}
        }
        catch (IOException e)
        {
            System.err.println("Failed to read message " + e.getMessage());
        }
    }

    public Websocket getWebsocket()
    {
        return ws;
    }

    private String formatErrorString(Error error)
    {
        return "Error: " + error.getErrorCode();
    }

}
