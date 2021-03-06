/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
import pl.edu.icm.unity.engine.api.endpoint.BindingAuthn;

/**
 * Non UI code which is invoked as a part of authentication pipeline for unauthenticated clients.
 * This filter checks whether automated proxy authentication is enabled for the protected endpoint
 * and if so invokes it. This may be triggered by a special Unity specific query parameter
 * or with endpoint's configuration. 
 * <p>
 * The automated proxy authentication can be used with a single configured authN option, which 
 * is additionally remote, redirect based. This mechanism is useful in cases where Unity should
 * act as invisible intermediate authN proxy. 
 * 
 * @author K. Benedyczak
 */
public class ProxyAuthenticationFilter implements Filter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			ProxyAuthenticationFilter.class);
	private static final String TRIGGERING_PARAM = "uy_auto_login";
	
	private Map<String, BindingAuthn> authenticators;
	private String endpointPath;
	private boolean triggerByDefault;
	
	public ProxyAuthenticationFilter(List<AuthenticationOption> authenticators, 
			String endpointPath, boolean triggerByDefault)
	{
		this.endpointPath = endpointPath;
		this.triggerByDefault = triggerByDefault;
		updateAuthenticators(authenticators);
	}

	public void updateAuthenticators(List<AuthenticationOption> authenticators)
	{
		Map<String, BindingAuthn> newMap = new HashMap<>();
		for (AuthenticationOption ao: authenticators)
			newMap.put(ao.getPrimaryAuthenticator().getAuthenticatorId(), 
					ao.getPrimaryAuthenticator());
		this.authenticators = newMap;
	}
	
	/**
	 * Removes the automated authentication triggering param and idp selection and 
	 * dumps the rest of params to query string.
	 * @param httpRequest
	 * @return
	 */
	private static String filteredQuery(HttpServletRequest httpRequest)
	{
		Map<String, String[]> parameterMap = httpRequest.getParameterMap();
		URIBuilder uriBuilder = new URIBuilder();
		for (Map.Entry<String, String[]> entry: parameterMap.entrySet())
			if (!entry.getKey().equals(ProxyAuthenticationFilter.TRIGGERING_PARAM) &&
					!entry.getKey().equals(AuthenticationUI.IDP_SELECT_PARAM))
			{
				for (String value: entry.getValue())
					uriBuilder.addParameter(entry.getKey(), value);
			}
		return uriBuilder.toString();
	}
	
	public static String getCurrentRelativeURL(HttpServletRequest httpRequest)
	{
		String origReqUri = (String)httpRequest.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
		String servletPath = origReqUri == null ? "/" : origReqUri;
		String query = httpRequest.getQueryString() == null ? "" : 
			ProxyAuthenticationFilter.filteredQuery(httpRequest);
		return servletPath + query;
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException
	{
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		
		if (!triggerProxyAuthentication(httpRequest, httpResponse))
			chain.doFilter(request, response);
	}
	
	private boolean triggerProxyAuthentication(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse)
	{
		if (isAutomatedAuthenticationDesired(httpRequest))
		{
			String selectedAuthn = httpRequest.getParameter(AuthenticationUI.IDP_SELECT_PARAM);
			if (selectedAuthn == null && authenticators.size() > 1)
			{
				log.error("There are more multiple authenticators installed, "
						+ "and automated login was requested without specifying (with " 
						+ AuthenticationUI.IDP_SELECT_PARAM + ") which one should be used. "
						+ "Automatic login is skipped.");
				return false;
			}
			String authenticatorId = selectedAuthn == null ?
					authenticators.keySet().iterator().next() :
					AuthenticationOptionKeyUtils.decodeAuthenticator(selectedAuthn);
			
			BindingAuthn authenticator = authenticators.get(authenticatorId);
			if (authenticator == null)
			{
				log.error("There is no authenticator which was provided as the "
					+ "one which should perform automated proxy authentication: {}", 
					authenticatorId);
				return false;
			}
			return triggerProxyAuthenticator(authenticator, httpRequest, httpResponse,
					selectedAuthn);
		}
		return false;
	}
	
	private boolean isAutomatedAuthenticationDesired(HttpServletRequest httpRequest)
	{
		if (triggerByDefault)
			return true;
		String autoLogin = httpRequest.getParameter(TRIGGERING_PARAM);
		if (autoLogin != null && Boolean.parseBoolean(autoLogin))
			return true;
		return false;
	}
	
	private boolean triggerProxyAuthenticator(BindingAuthn authenticatorParam,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse,
			String selectedAuthn)
	{
		if (authenticatorParam instanceof ProxyAuthenticationCapable)
		{
			ProxyAuthenticationCapable authenticator = (ProxyAuthenticationCapable) 
					authenticatorParam;
			try
			{
				log.debug("Invoking automated proxy authentication handler of {}",
						authenticator.getAuthenticatorId());
				if (selectedAuthn != null)
				{
					Cookie lastIdpCookie = AuthenticationUI.createLastIdpCookie(
							endpointPath, selectedAuthn);
					httpResponse.addCookie(lastIdpCookie);
				}
				boolean result = authenticator.triggerAutomatedAuthentication(
						httpRequest, httpResponse);
				if (result)
				{
					log.debug("Automated proxy authentication of {} handled the request",
							authenticator.getAuthenticatorId());
				} else
					log.trace("Automated proxy authentication of {} ignored the request",
							authenticator.getAuthenticatorId());
				return result;
			} catch (Exception e)
			{
				log.error("Can not invoke automated proxy authentication", e);
				return false;
			}
		} else
		{
			log.error("The authenticator {} configured for automated "
					+ "proxy authentication is not supporting this feature", 
					authenticatorParam.getAuthenticatorId());
			return false;
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}

	@Override
	public void destroy()
	{
	}
}
