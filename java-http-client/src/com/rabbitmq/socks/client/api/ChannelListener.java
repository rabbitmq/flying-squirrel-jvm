package com.rabbitmq.socks.client.api;

/**
 * 
 * @author tfox
 * 
 */
public interface ChannelListener
{
    void onMessage(Message message);
}
