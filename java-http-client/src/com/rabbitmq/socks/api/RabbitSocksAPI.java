package com.rabbitmq.socks.api;

import java.util.List;

/**
 * 
 * @author tfox
 *
 */
public interface RabbitSocksAPI
{
	void createEndpoint(Endpoint endpoint) throws Exception;
	
	void deleteEndpoint(String endpointName) throws Exception;
	
	Endpoint getEndpoint(String endpointName) throws Exception;
	
	List<Connection> listConnectionsForEndpoint(String endpointName)
		throws Exception;
	
	List<String> listEndpointNames() throws Exception;
	
	String generateTicket(String endpointName, String identity,
			              long timeout) throws Exception;
}
