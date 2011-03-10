package com.rabbitmq.socks.websocket;

import java.io.IOException;

import com.rabbitmq.socks.api.WebsocketListener;

public interface Websocket
{
    void connect() throws IOException;

    void send(String msg) throws IOException;

    void setListener(WebsocketListener listener);

    void close() throws IOException;
}
