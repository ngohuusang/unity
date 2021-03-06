/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.util.HashMap;

import org.springframework.stereotype.Component;

/**
 * Singleton, coordinating pairs of co-working OAuth Unity endpoints. Each web authorization endpoint should have
 * a single matching token (rest) endpoint. This class allows for discovering authz endpoint path by the token 
 * endpoint (namely its discovery subsystem). In future should detect misconfigurations (missing peer, 
 * doubled peers).
 * <p>
 * Thread-safe.
 * 
 * @author K. Benedyczak
 */
@Component
public class OAuthEndpointsCoordinator
{
	private HashMap<String, EndpointsPair> pairs = new HashMap<>(); 
	
	public synchronized void registerAuthzEndpoint(String issuer, String path)
	{
		EndpointsPair pair = pairs.get(issuer);
		if (pair == null)
		{
			pair = new EndpointsPair();
			pairs.put(issuer, pair);
		}
		pair.setAuthZPath(path);
	}
	
	public synchronized void registerTokenEndpoint(String issuer, String path)
	{
		EndpointsPair pair = pairs.get(issuer);
		if (pair == null)
		{
			pair = new EndpointsPair();
			pairs.put(issuer, pair);
		}
		pair.setTokenPath(path);
	}
	
	public synchronized String getAuthzEndpoint(String issuer)
	{
		EndpointsPair pair = pairs.get(issuer);
		if (pair == null)
			throw new IllegalArgumentException("There is no authorization endpoint for the OAuth issuer "
					+ issuer);
		return pair.getAuthZPath();
	}
	
	public static class EndpointsPair
	{
		private String authZPath;
		private String tokenPath;
		
		public String getAuthZPath()
		{
			return authZPath;
		}

		public void setAuthZPath(String authZPath)
		{
			this.authZPath = authZPath;
		}

		public String getTokenPath()
		{
			return tokenPath;
		}

		public void setTokenPath(String tokenPath)
		{
			this.tokenPath = tokenPath;
		}
	}
}
