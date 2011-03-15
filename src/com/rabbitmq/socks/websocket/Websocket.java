package com.rabbitmq.socks.websocket;

import java.io.IOException;


public interface Websocket
{
    void connect() throws IOException;

    void send(String msg) throws IOException;

    void setListener(WebsocketListener listener);

    void close() throws IOException;
}
