/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.endpoint;

import java.net.URL;

import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Typical boilerplate for all {@link WebAppEndpointInstance}s.
 * @author K. Benedyczak
 */
public abstract class AbstractWebEndpoint extends AbstractEndpoint implements WebAppEndpointInstance
{
	protected NetworkServer httpServer;
	
	public AbstractWebEndpoint(NetworkServer httpServer)
	{
		this.httpServer = httpServer;
	}

	/**
	 * @return the URL where the server listens to. It has no path element.
	 */
	public URL getBaseUrl()
	{
		return httpServer.getAdvertisedAddress();
	}
	
	/**
	 * @param servletPath path of the servlet exposing the endpoint, Only the servlet's path, without context prefix.
	 * @return URL in string form, including the servers address, context address and 
	 * the servlet's address. 
	 */
	public String getServletUrl(String servletPath)
	{
		return getBaseUrl().toExternalForm() +
				getEndpointDescription().getEndpoint().getContextAddress() + 
				servletPath;
	}
	
	@Override
	public final void start() throws EngineException
	{
		startOverridable();
		httpServer.deployEndpoint(this);
	}
	
	protected void startOverridable()
	{
	}
	
	@Override
	public final void destroy() throws EngineException
	{
		super.destroy();
		httpServer.undeployEndpoint(this.getEndpointDescription().getEndpoint().getName());
		destroyOverridable();
	}
	
	protected void destroyOverridable()
	{
	}
}
