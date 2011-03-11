/**
 *
 * Original based on file with following licence, but hacked since then:
 *
 * The MIT License
 *
 * Copyright (c) 2009 Adam MacBeth
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.rabbitmq.socks.websocket.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.Executor;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.rabbitmq.socks.api.WebsocketListener;
import com.rabbitmq.socks.websocket.Websocket;

/**
 *
 * @author tfox
 *
 *         Originally based on file from
 *         https://github.com/adamac/Java-WebSocket-client
 *
 *         Which was released with the following licence:
 *
 *         The MIT License
 *
 *         Copyright (c) 2009 Adam MacBeth
 *
 *         Permission is hereby granted, free of charge, to any person obtaining
 *         a copy of this software and associated documentation files (the
 *         "Software"), to deal in the Software without restriction, including
 *         without limitation the rights to use, copy, modify, merge, publish,
 *         distribute, sublicense, and/or sell copies of the Software, and to
 *         permit persons to whom the Software is furnished to do so, subject to
 *         the following conditions:
 *
 *         The above copyright notice and this permission notice shall be
 *         included in all copies or substantial portions of the Software.
 *
 *         THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *         EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *         MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *         NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *         BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *         ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *         CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *         SOFTWARE.
 */
public class WebsocketImpl implements Websocket
{
    private final URI uri;

    private Socket socket;

    private InputStream inputStream;

    private OutputStream outputStream;

    private HashMap<String, String> headers;

    private final Executor executor;

    private volatile boolean closed = true;

    private volatile WebsocketListener listener;

    public WebsocketImpl(URI url, Executor executor)
    {
        uri = url;

        this.executor = executor;

        String protocol = uri.getScheme();
        if (!protocol.equals("ws") && !protocol.equals("wss"))
        {
            throw new IllegalArgumentException("Unsupported protocol: "
                            + protocol);
        }
    }

    @Override
    public void setListener(WebsocketListener listener)
    {
        this.listener = listener;
    }

    public void setHeaders(HashMap<String, String> headers)
    {
        this.headers = headers;
    }

    public Socket getSocket()
    {
        return socket;
    }

    @Override
    public void connect() throws IOException
    {
        String host = uri.getHost();
        String path = uri.getPath();
        if (path.equals(""))
        {
            path = "/";
        }
        String query = uri.getQuery();
        if (query != null)
        {
            path = path + "?" + query;
        }
        String origin = "http://" + host;
        socket = createSocket();
        int port = socket.getPort();
        if (port != 80)
        {
            host = host + ":" + port;
        }
        outputStream = new BufferedOutputStream(socket.getOutputStream());
        StringBuffer extraHeaders = new StringBuffer();
        if (headers != null)
        {
            for (Entry<String, String> entry : headers.entrySet())
            {
                extraHeaders.append(entry.getKey() + ": " + entry.getValue()
                                + "\r\n");
            }
        }
        String request = "GET " + path + " HTTP/1.1\r\n"
                        + "Upgrade: WebSocket\r\n" + "Connection: Upgrade\r\n"
                        + "Host: " + host + "\r\n" + "Origin: " + origin
                        + "\r\n" + extraHeaders.toString() + "\r\n";
        outputStream.write(request.getBytes());
        outputStream.flush();
        inputStream = new BufferedInputStream(socket.getInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                        inputStream));
        String header = reader.readLine();
        if (!header.equals("HTTP/1.1 101 Web Socket Protocol Handshake"))
        {
            throw new IOException("Invalid handshake response");
        }
        boolean foundHeader = false;
        while ((header = reader.readLine()) != null)
        {
            if (header.equals("Connection: Upgrade"))
            {
                foundHeader = true;
                break;
            }
        }
        if (!foundHeader)
        {
            throw new IOException("Invalid handshake response");
        }
        closed = false;
        startListening();
    }

    @Override
    public synchronized void send(String str) throws IOException
    {
        if (closed)
        {
            throw new IllegalStateException("Websocket is closed");
        }
        outputStream.write(0x00);
        outputStream.write(str.getBytes("UTF-8"));
        outputStream.write(0xff);
        outputStream.flush();
    }

    @Override
    public synchronized void close() throws IOException
    {
        closed = true;
        for (int i = 0; i < 9; i++)
        {
            outputStream.write(0x00);
        }
        outputStream.flush();
        inputStream.close();
        outputStream.close();
        socket.close();
    }

    private Socket createSocket() throws IOException
    {
        String scheme = uri.getScheme();
        String host = uri.getHost();

        int port = uri.getPort();
        if (port == -1)
        {
            if (scheme.equals("wss"))
            {
                port = 443;
            }
            else if (scheme.equals("ws"))
            {
                port = 80;
            }
            else
            {
                throw new IllegalArgumentException("Unsupported scheme");
            }
        }

        if (scheme.equals("wss"))
        {
            SocketFactory factory = SSLSocketFactory.getDefault();
            return factory.createSocket(host, port);
        }
        else
        {
            return new Socket(host, port);
        }
    }

    private void startListening()
    {
        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                while (!closed)
                {
                    try
                    {
                        String msg = recv();
                        listener.onMessage(msg);
                    }
                    catch (IOException e)
                    {
                        if (!closed)
                        {
                            System.err.println("Failed to receive message "
                                            + e.getMessage());
                        }
                    }
                }
            }
        });
    }

    private String recv() throws IOException
    {
        StringBuffer buf = new StringBuffer();

        int b = inputStream.read();
        if ((b & 0x80) == 0x80)
        {
            // Skip data frame
            int len = 0;
            do
            {
                b = inputStream.read() & 0x7f;
                len = len * 128 + b;
            }
            while ((b & 0x80) != 0x80);

            for (int i = 0; i < len; i++)
            {
                inputStream.read();
            }
        }

        while (true)
        {
            b = inputStream.read();
            if (b == 0xff)
            {
                break;
            }

            buf.append((char) b);
        }

        return new String(buf.toString().getBytes(), "UTF8");
    }
}
