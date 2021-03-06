/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.AuthenticatorInstanceDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;

/**
 * Implementation of {@link AuthenticatorsManagement}
 * 
 * @author K. Benedyczak
 */
@Component
public class AuthenticatorsManagementImpl implements AuthenticatorSupportManagement
{
	private AuthenticatorLoader authnLoader;
	private AuthenticatorInstanceDB authenticatorDB;
	
	@Autowired
	public AuthenticatorsManagementImpl(AuthenticatorLoader authnLoader,
			AuthenticatorInstanceDB authenticatorDB)
	{
		this.authnLoader = authnLoader;
		this.authenticatorDB = authenticatorDB;
	}


	@Override
	@Transactional
	public List<AuthenticationOption> getAuthenticatorUIs(List<AuthenticationOptionDescription> authnList) 
			throws EngineException
	{
		return authnLoader.getAuthenticators(authnList);
	}
	
	@Override
	@Transactional
	public void removeAllPersistedAuthenticators() throws EngineException
	{
		authenticatorDB.deleteAll();
	}
}
