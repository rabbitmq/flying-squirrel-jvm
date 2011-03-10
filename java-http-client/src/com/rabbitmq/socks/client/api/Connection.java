package com.rabbitmq.socks.client.api;

import java.io.IOException;

/**
 * 
 * @author tfox
 *
 */
public interface Connection
{
    void connect(String ticket) throws IOException;
    
    void setChannelListener(String channelName, ChannelListener listener);
    
    void send(Message message) throws IOException;
    
    void close() throws IOException;
}
