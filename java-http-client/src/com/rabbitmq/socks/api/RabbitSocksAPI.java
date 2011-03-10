package com.rabbitmq.socks.api;

import java.util.List;

/**
 * 
 * @author tfox
 *
 */
public interface RabbitSocksAPI
{
	Endpoint createEndpoint(Endpoint endpoint) throws RabbitSocksAPIException;
	
	void deleteEndpoint(String endpointName) throws RabbitSocksAPIException;
	
	Endpoint getEndpoint(String endpointName) throws RabbitSocksAPIException;
	
	List<ConnectionInfo> listConnectionsForEndpoint(String endpointName)
		throws RabbitSocksAPIException;
	
	List<String> listEndpointNames() throws RabbitSocksAPIException;
	
	String generateTicket(String endpointName, String identity,
			              long timeout) throws RabbitSocksAPIException;
}
