package com.rabbitmq.socks.api.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import com.rabbitmq.socks.api.ChannelDefinition;
import com.rabbitmq.socks.api.ChannelType;
import com.rabbitmq.socks.api.ConnectionInfo;
import com.rabbitmq.socks.api.Endpoint;
import com.rabbitmq.socks.api.RabbitSocksAPI;
import com.rabbitmq.socks.api.RabbitSocksAPIException;

/**
 *
 * @author tfox
 *
 */
public class RabbitSocksAPIImpl implements RabbitSocksAPI
{
    private final String endpointURL;

    private static final String GET_METHOD = "GET";
    private static final String POST_METHOD = "POST";
    private static final String PUT_METHOD = "PUT";
    private static final String DELETE_METHOD = "DELETE";

    public RabbitSocksAPIImpl(final String host, final int port, final String prefix)
    {
        endpointURL =
            "http://" + host + ":" + port + "/" + prefix + "/endpoints/";
    }

    @Override
    public Endpoint createEndpoint(final Endpoint endpoint)
                    throws RabbitSocksAPIException
    {
        if (endpoint == null)
        {
            throw new IllegalArgumentException("endpoint cannot be null");
        }
        validateEndpointName(endpoint.getName());
        try
        {
            String uri = endpointURL + endpoint.getName();
            HttpURLConnection conn = doRequest(uri, PUT_METHOD, true);
            Writer writer = createWriter(conn);
            JsonFactory factory = new JsonFactory();
            JsonGenerator g = factory.createJsonGenerator(writer);
            g.writeStartObject();
            for (Map.Entry<String, ChannelDefinition> entry : endpoint
                            .getChannelDefinitions().entrySet())
            {
                g.writeArrayFieldStart(entry.getKey()); // Endpoint name
                g.writeString(entry.getValue().getType().toString()); // Channel
                                                                      // type
                g.writeString(entry.getValue().getResource()); // Resource
                g.writeEndArray();
            }
            g.writeEndObject();
            g.close();
            writer.close();
            int respCode = conn.getResponseCode();
            if (respCode == 200)
            {
                // OK
            }
            else if (respCode == 409)
            {
                throw new RabbitSocksAPIException("Endpoint already exists "
                                + endpoint, respCode);
            }
            else
            {
                throwUnexpectedResponseCode(respCode);
            }
            Reader reader = createReader(conn);

            return parseEndpoint(endpoint.getName(), reader, factory);
        }
        catch (IOException e)
        {
            throw new RabbitSocksAPIException(e);
        }
    }

    @Override
    public Endpoint getEndpoint(final String endpointName)
                    throws RabbitSocksAPIException
    {
        validateEndpointName(endpointName);
        try
        {
            String uri = endpointURL + endpointName;
            HttpURLConnection conn = doRequest(uri, GET_METHOD, false);
            int respCode = conn.getResponseCode();
            if (respCode == 200)
            {
                // OK
            }
            else if (respCode == 404)
            {
                throw new RabbitSocksAPIException("No such endpoint: "
                                + endpointName, 404);
            }
            else if (respCode != 200)
            {
                throwUnexpectedResponseCode(respCode);
            }
            return parseEndpoint(endpointName, createReader(conn),
                            new JsonFactory());
        }
        catch (IOException e)
        {
            throw new RabbitSocksAPIException(e);
        }
    }

    @Override
    public void deleteEndpoint(final String endpointName)
                    throws RabbitSocksAPIException
    {
        validateEndpointName(endpointName);
        try
        {
            String uri = endpointURL + endpointName;
            HttpURLConnection conn = doRequest(uri, DELETE_METHOD, false);
            int respCode = conn.getResponseCode();
            if (respCode != 204)
            {
                throwUnexpectedResponseCode(respCode);
            }
        }
        catch (IOException e)
        {
            throw new RabbitSocksAPIException(e);
        }
    }

    @Override
    public List<ConnectionInfo> listConnectionsForEndpoint(
                    final String endpointName) throws RabbitSocksAPIException
    {
        validateEndpointName(endpointName);
        try
        {
            String uri = endpointURL + endpointName + "/connections";
            HttpURLConnection conn = doRequest(uri, GET_METHOD, false);
            int respCode = conn.getResponseCode();
            if (respCode != 200)
            {
                throwUnexpectedResponseCode(respCode);
            }
            Reader reader = createReader(conn);
            JsonFactory f = new JsonFactory();
            JsonParser jp = f.createJsonParser(reader);
            List<ConnectionInfo> connections = new ArrayList<ConnectionInfo>();
            jp.nextToken(); // Move onto [
            while (jp.nextToken() != JsonToken.END_ARRAY)
            {
                jp.nextToken(); // Move onto {
                jp.nextToken(); // Move onto url field
                String url = jp.getText();
                url = jp.getText();
                jp.nextToken();
                jp.nextToken(); // Move onto GUID field
                String guid = jp.getText();
                jp.nextToken();
                jp.nextToken(); // Move onto endpointName field
                String epName = jp.getText();
                jp.nextToken();
                jp.nextToken(); // Move onto protocol field
                String protocol = jp.getText();
                jp.nextToken();
                jp.nextToken(); // Move onto meta field
                String meta = jp.getText();
                ConnectionInfo connInfo = new ConnectionInfoImpl(url, guid,
                                epName, protocol, meta);
                connections.add(connInfo);
                jp.nextToken(); // Move into }
            }
            return connections;
        }
        catch (IOException e)
        {
            throw new RabbitSocksAPIException(e);
        }
    }

    @Override
    public List<String> listEndpointNames() throws RabbitSocksAPIException
    {
        try
        {
            HttpURLConnection conn = doRequest(endpointURL, GET_METHOD, false);
            int respCode = conn.getResponseCode();
            if (respCode != 200)
            {
                throwUnexpectedResponseCode(respCode);
            }
            List<String> endpoints = new ArrayList<String>();
            Reader reader = createReader(conn);
            JsonFactory f = new JsonFactory();
            JsonParser jp = f.createJsonParser(reader);
            JsonToken token;
            while ((token = jp.nextToken()) != JsonToken.END_ARRAY)
            {
                if (token == JsonToken.VALUE_STRING)
                {
                    String resource = jp.getText(); // Endpoint name
                    endpoints.add(resource);
                }
            }
            return endpoints;
        }
        catch (IOException e)
        {
            throw new RabbitSocksAPIException(e);
        }
    }

    @Override
    public String generateTicket(final String endpointName,
                    final String identity, final long timeout)
                    throws RabbitSocksAPIException
    {
        validateEndpointName(endpointName);
        validateIdentity(identity);
        try
        {
            HttpURLConnection conn = doRequest(endpointURL + endpointName
                            + "/tickets", POST_METHOD, true);
            Writer writer = createWriter(conn);
            JsonFactory f = new JsonFactory();
            JsonGenerator g = f.createJsonGenerator(writer);
            g.writeStartObject(); // {
            g.writeStringField("identity", identity); // identity
            g.writeNumberField("timeout", timeout); // timeout
            g.writeEndObject(); // }
            g.close();
            writer.close();
            int respCode = conn.getResponseCode();
            if (respCode == 404)
            {
                throw new RabbitSocksAPIException("No such endpoint: "
                                + endpointName, 404);
            }
            else if (respCode != 200)
            {
                throwUnexpectedResponseCode(respCode);
            }
            Reader reader = createReader(conn);
            JsonParser jp = f.createJsonParser(reader);
            jp.nextToken();
            JsonToken token;
            String ticket = null;
            while ((token = jp.nextToken()) != null)
            {
                if (token == JsonToken.VALUE_STRING)
                {
                    ticket = jp.getText();
                }
            }
            return ticket;
        }
        catch (IOException e)
        {
            throw new RabbitSocksAPIException(e);
        }
    }

    // Private methods --------------------------------------------------------

    private void validateEndpointName(String endpointName)
    {
        validateStringArg(endpointName, "endpoint name");
    }

    private void validateIdentity(String identity)
    {
        validateStringArg(identity, "identity");
    }

    private void validateStringArg(final String arg, final String argName)
    {
        if (arg == null)
        {
            throw new IllegalArgumentException(argName + " cannot be null");
        }
        if ("".equals(arg))
        {
            throw new IllegalArgumentException(argName
                            + " cannot be an empty string");
        }
    }

    private Endpoint parseEndpoint(final String endpointName,
                    final Reader reader, final JsonFactory factory)
                    throws IOException
    {
        Endpoint endpoint = new EndpointImpl(endpointName);
        JsonParser jp = factory.createJsonParser(reader);
        jp.nextToken(); // Move onto {
        while (true)
        {
            JsonToken tok = jp.nextToken();
            if (tok== JsonToken.END_OBJECT)
            {
                break;
            }
            jp.nextToken();
            String k = jp.getText();
            endpoint.setKey(jp.getText());
            jp.nextToken(); // Move onto def
            while (true)
            {
                jp.nextToken(); //Move onto {
                tok = jp.nextToken();
                if (tok == JsonToken.END_OBJECT)
                {
                    break;
                }
                String channelName = jp.getCurrentName(); //channel name
                jp.nextToken(); // [
                jp.nextToken(); //Move onto channel type
                ChannelType channelType = ChannelType.fromString(jp.getText());
                jp.nextToken();
                endpoint.putChannelDefinition(channelName, channelType,
                                jp.getText());
            }
            jp.nextToken();
            jp.nextToken();
            while (true)
            {
                if (jp.nextToken() == JsonToken.END_OBJECT)
                {
                    break;
                }
                jp.nextToken(); //Move onto url field
                endpoint.putProtocolURL(jp.getCurrentName(), jp.getText());
            }
        }
        return endpoint;
    }

    private HttpURLConnection doRequest(final String uri, final String method,
                    final boolean contentType) throws IOException
    {
        URL url = new URL(uri);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod(method);
        if (contentType)
        {
            conn.addRequestProperty("content-type", "application/json");
        }
        return conn;
    }

    private void throwUnexpectedResponseCode(final int respCode)
                    throws RabbitSocksAPIException
    {
        throw new RabbitSocksAPIException("Unexpected response code "
                        + respCode, respCode);
    }

    private Reader createReader(final HttpURLConnection conn)
                    throws IOException
    {
        return new BufferedReader(new InputStreamReader(conn.getInputStream()));
    }

    private Writer createWriter(final HttpURLConnection conn)
                    throws IOException
    {
        return new BufferedWriter(
                        new OutputStreamWriter(conn.getOutputStream()));
    }

    // debug
    private void dumpReader(final Reader reader) throws IOException
    {
        int b = -1;
        StringBuffer sb = new StringBuffer();
        while ((b = reader.read()) != -1)
        {
            sb.append((char) b);
        }
        System.out.println("str:" + sb.toString());
    }

}
