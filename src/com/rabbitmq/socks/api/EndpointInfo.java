package com.rabbitmq.socks.api;

import java.util.Map;

/**
 *
 * @author tfox
 *
 */
public interface EndpointInfo
{
    String getName();

    String getKey();

    void setKey(String key);

    Map<String, String> getProtocols();

    Map<String, ChannelDefinition> getChannelDefinitions();

    EndpointInfo putChannelDefinition(String channelName,
                                      ChannelType channelType,
                                      String resource);

    EndpointInfo putProtocolURL(String protocolName, String url);
}
