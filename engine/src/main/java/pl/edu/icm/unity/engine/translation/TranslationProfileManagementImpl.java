/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.attribute.AttributeValueConverter;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.InputTranslationProfileDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.api.generic.OutputTranslationProfileDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Implementation of {@link TranslationProfileManagement}
 * 
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
@Transactional
public class TranslationProfileManagementImpl implements TranslationProfileManagement
{
	private AuthorizationManager authz;
	private InputTranslationProfileDB itpDB;
	private OutputTranslationProfileDB otpDB;
	private InputTranslationActionsRegistry inputActionReg;
	private OutputTranslationActionsRegistry outputActionReg;
	private AttributeValueConverter attrConverter;

	
	@Autowired
	public TranslationProfileManagementImpl(AuthorizationManager authz,
			InputTranslationProfileDB itpDB, OutputTranslationProfileDB otpDB,
			InputTranslationActionsRegistry inputActionReg,
			OutputTranslationActionsRegistry outputActionReg,
			AttributeValueConverter attrConverter)
	{
		this.authz = authz;
		this.itpDB = itpDB;
		this.otpDB = otpDB;
		this.inputActionReg = inputActionReg;
		this.outputActionReg = outputActionReg;
		this.attrConverter = attrConverter;
	}

	private NamedCRUDDAOWithTS<TranslationProfile> getDAO(TranslationProfile profile)
	{
		return getDAO(profile.getProfileType());
	}
	
	private NamedCRUDDAOWithTS<TranslationProfile> getDAO(ProfileType type)
	{
		if (type == ProfileType.INPUT)
			return itpDB;
		else if (type == ProfileType.OUTPUT)
			return otpDB;
		else
			throw new IllegalArgumentException("Only input and output "
					+ "profiles can be created with this API");
	}
	
	@Override
	public void addProfile(TranslationProfile toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		checkProfile(toAdd);
		getDAO(toAdd).create(toAdd);
	}

	@Override
	public void removeProfile(ProfileType type, String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		getDAO(type).delete(name);
	}

	@Override
	public void updateProfile(TranslationProfile updated) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		checkProfile(updated);
		getDAO(updated).update(updated);
	}

	@Override
	public Map<String, TranslationProfile> listInputProfiles() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return getDAO(ProfileType.INPUT).getAllAsMap();
	}

	@Override
	public Map<String, TranslationProfile> listOutputProfiles() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return getDAO(ProfileType.OUTPUT).getAllAsMap();
	}

	@Override
	public TranslationProfile getInputProfile(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return itpDB.get(name);
	}

	@Override
	public TranslationProfile getOutputProfile(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return otpDB.get(name);
	}
	
	private void checkProfile(TranslationProfile profile)
	{
		TranslationProfileInstance<?, ?> instance;
		if (profile.getProfileType() == ProfileType.INPUT)
			instance = new InputTranslationProfile(profile, this, inputActionReg);
		else if (profile.getProfileType() == ProfileType.OUTPUT)
			instance = new OutputTranslationProfile(profile, this, outputActionReg, attrConverter);
		else
			throw new IllegalArgumentException("Unsupported profile type: " + profile.getProfileType());
		if (instance.hasInvalidActions())
			throw new IllegalArgumentException("Profile definition is invalid");
	}
}
