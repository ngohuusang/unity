/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.endpoint;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.authn.AuthenticatorLoader;
import pl.edu.icm.unity.store.api.generic.RealmDB;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

/**
 * Loads a composite {@link EndpointInstance} out of an {@link Endpoint} object which is stored in database.
 * @author K. Benedyczak
 */
@Component
public class EndpointInstanceLoader
{
	private EndpointFactoriesRegistry endpointFactoriesReg;
	private RealmDB realmDB;
	private AuthenticatorLoader authnLoader;

	@Autowired
	public EndpointInstanceLoader(EndpointFactoriesRegistry endpointFactoriesReg,
			RealmDB realmDB, AuthenticatorLoader authnLoader)
	{
		this.endpointFactoriesReg = endpointFactoriesReg;
		this.realmDB = realmDB;
		this.authnLoader = authnLoader;
	}

	/**
	 * Creates {@link EndpointInstance} out of given {@link Endpoint} description. 
	 * @param src
	 * @return
	 */
	public EndpointInstance createEndpointInstance(Endpoint src)
	{
		EndpointFactory factory = endpointFactoriesReg.getById(src.getTypeId());
		EndpointInstance instance = factory.newInstance();
		List<AuthenticationOptionDescription> authnOptions = src.getConfiguration().getAuthenticationOptions();
		
		String endpointConfig = src.getConfiguration().getConfiguration();
		List<AuthenticationOption> authenticators = authnLoader.getAuthenticators(authnOptions);
		ResolvedEndpoint fullInfo = resolveEndpoint(src);
		
		instance.initialize(fullInfo, authenticators, endpointConfig);
		return instance;
	}
	
	private ResolvedEndpoint resolveEndpoint(Endpoint src)
	{
		EndpointFactory factory = endpointFactoriesReg.getById(src.getTypeId());
		AuthenticationRealm realm = realmDB.get(src.getConfiguration().getRealm());
		return new ResolvedEndpoint(src, realm, factory.getDescription());		
	}
}
