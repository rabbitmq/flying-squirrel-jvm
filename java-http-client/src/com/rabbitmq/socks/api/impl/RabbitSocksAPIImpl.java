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

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import com.rabbitmq.socks.api.ChannelDefinition;
import com.rabbitmq.socks.api.ChannelType;
import com.rabbitmq.socks.api.Connection;
import com.rabbitmq.socks.api.Endpoint;
import com.rabbitmq.socks.api.ProtocolURL;
import com.rabbitmq.socks.api.RabbitSocksAPI;
import com.rabbitmq.socks.api.RabbitSocksAPIException;

/**
 * 
 * @author tfox
 * 
 */
public class RabbitSocksAPIImpl implements RabbitSocksAPI
{
    private final String        endpointURL;

    private static final String GET_METHOD                     = "GET";
    private static final String POST_METHOD                    = "POST";
    private static final String PUT_METHOD                     = "PUT";
    private static final String DELETE_METHOD                  = "DELETE";

    private static final String ENDPOINT_KEY_FIELD             = "key";
    private static final String ENDPOINT_DEFS_FIELD            = "def";
    private static final String ENDPOINT_URLS_FIELD            = "urls";

    private static final String CONNECTION_CONNECTION_ID_FIELD = "connection-id";
    private static final String CONNECTION_PROTOCOL_FIELD      = "protocol";
    private static final String CONNECTION_META_DATA_FILED     = "meta-data";

    private static final String TICKET_IDENTITY_FIELD          = "identity";
    private static final String TICKET_TIMEOUT_FIELD           = "timeout";

    public RabbitSocksAPIImpl(final String host, final int port)
    {
        endpointURL = "http://" + host + ":" + port + "/socks-api/endpoints/";
    }

    public void createEndpoint(final Endpoint endpoint)
        throws RabbitSocksAPIException
    {
        try
        {
            String uri = endpointURL + endpoint.getName();
            HttpURLConnection conn = doRequest(uri, PUT_METHOD, true);
            Writer writer = createWriter(conn);
            JsonFactory f = new JsonFactory();
            JsonGenerator g = f.createJsonGenerator(writer);
            g.writeStartObject();
            for (ChannelDefinition def : endpoint.getChannelDefinitions())
            {
                g.writeArrayFieldStart(def.getName());
                g.writeString(def.getType().toString());
                g.writeString(def.getResource());
                g.writeEndArray();
            }
            g.writeEndObject();
            g.close();
            writer.close();
            int respCode = conn.getResponseCode();
            if (respCode == 200)
            {
                // OK
                return;
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
        }
        catch (IOException e)
        {
            throw new RabbitSocksAPIException(e);
        }
    }

    public Endpoint getEndpoint(final String endpointName)
        throws RabbitSocksAPIException
    {
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
            Endpoint endpoint = new EndpointImpl(endpointName);
            JsonFactory f = new JsonFactory();
            
            JsonParser jp = f.createJsonParser(createReader(conn));
            jp.nextToken();
            while (jp.nextToken() != null)
            {
                String fieldName = jp.getCurrentName();
                if (ENDPOINT_KEY_FIELD.equals(fieldName))
                {
                    jp.nextToken();
                    endpoint.setKey(jp.getText());
                }
                else if (ENDPOINT_DEFS_FIELD.equals(fieldName))
                {
                    while (true)
                    {
                        jp.nextToken();
                        JsonToken tok = jp.nextToken();
                        if (tok == JsonToken.END_OBJECT)
                        {
                            break;
                        }
                        String channelName = jp.getCurrentName();
                        jp.nextToken();
                        jp.nextToken();
                        ChannelType channelType = ChannelType.fromString(jp
                                .getText());
                        jp.nextToken();
                        endpoint.addChannelDefinition(channelName, channelType,
                                jp.getText());
                    }
                }
                else if (ENDPOINT_URLS_FIELD.equals(fieldName))
                {
                    jp.nextToken();
                    JsonToken tok;
                    while (true)
                    {
                        tok = jp.nextToken();
                        if (tok == JsonToken.END_OBJECT)
                        {
                            break;
                        }
                        jp.nextToken();
                        ProtocolURL purl = new ProtocolURL(jp.getCurrentName(),
                                jp.getText());
                        endpoint.addProtocolURL(purl);
                    }
                }
            }
            return endpoint;
        }
        catch (IOException e)
        {
            throw new RabbitSocksAPIException(e);
        }
    }

    public void deleteEndpoint(final String endpointName)
        throws RabbitSocksAPIException
    {
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

    public List<Connection> listConnectionsForEndpoint(final String endpointName)
            throws RabbitSocksAPIException
    {
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
            List<Connection> conns = new ArrayList<Connection>();
            jp.nextToken(); // Start array
            while (jp.nextToken() != JsonToken.END_ARRAY)
            {
                jp.nextToken(); // Start object
                jp.nextToken(); // Field name
                String fieldName = jp.getCurrentName();
                String connectionID = null;
                String protocol = null;
                byte[] metaData = null;
                if (CONNECTION_CONNECTION_ID_FIELD.equals(fieldName))
                {
                    jp.nextToken();
                    connectionID = jp.getText();
                }
                else if (CONNECTION_PROTOCOL_FIELD.equals(fieldName))
                {
                    jp.nextToken();
                    protocol = jp.getText();
                }
                else if (CONNECTION_META_DATA_FILED.equals(fieldName))
                {
                    jp.nextToken();
                    metaData = jp.getBinaryValue();
                }
                conns.add(new ConnectionImpl(connectionID, endpointName, protocol,
                        metaData));
                jp.nextToken();
            }
            return conns;
        }
        catch (IOException e)
        {
            throw new RabbitSocksAPIException(e);
        }
    }

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
                    String resource = jp.getText();
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

    public String generateTicket(final String endpointName,
            final String identity, final long timeout)
        throws RabbitSocksAPIException
    {
        try
        {
            HttpURLConnection conn = doRequest(endpointURL + endpointName
                    + "/tickets", POST_METHOD, true);
            Writer writer = createWriter(conn);
            JsonFactory f = new JsonFactory();
            JsonGenerator g = f.createJsonGenerator(writer);
            g.writeStartObject();
            g.writeStringField(TICKET_IDENTITY_FIELD, identity);
            g.writeNumberField(TICKET_TIMEOUT_FIELD, timeout);
            g.writeEndObject();
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
    
    private Reader createReader(final HttpURLConnection conn) throws IOException
    {
        return new BufferedReader(new InputStreamReader(conn.getInputStream()));
    }

    private Writer createWriter(final HttpURLConnection conn) throws IOException
    {
        return new BufferedWriter(
                new OutputStreamWriter(conn.getOutputStream()));
    }
    
}
