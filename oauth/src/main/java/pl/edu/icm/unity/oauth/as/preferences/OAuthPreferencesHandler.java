/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.preferences;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;
import pl.edu.icm.unity.webui.common.preferences.PreferencesEditor;
import pl.edu.icm.unity.webui.common.preferences.PreferencesHandler;

/**
 * Handler of OAuth preferences.
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component
public class OAuthPreferencesHandler implements PreferencesHandler
{
	private final Set<String> SUPPORTED_ENDPOINTS = new HashSet<String>();
	private UnityMessageSource msg;
	private EntityManagement idsMan;
	private IdentityTypeSupport idTypeSupport;
	
	@Autowired
	public OAuthPreferencesHandler(UnityMessageSource msg, EntityManagement idsMan, 
			IdentityTypeSupport idTypeSupport)
	{
		this.msg = msg;
		this.idsMan = idsMan;
		this.idTypeSupport = idTypeSupport;
		SUPPORTED_ENDPOINTS.add(OAuthAuthzWebEndpoint.NAME);
	}

	@Override
	public String getPreferenceLabel()
	{
		return msg.getMessage("OAuthPreferences.label");
	}

	@Override
	public String getPreferenceId()
	{
		return OAuthPreferences.ID;
	}

	@Override
	public PreferencesEditor getPreferencesEditor(String value)
	{
		OAuthPreferences preferences = new OAuthPreferences();
		if (value != null)
			preferences.setSerializedConfiguration(JsonUtil.parse(value));
		return new OAuthPreferencesEditor(msg, preferences, idsMan, idTypeSupport);
	}

	@Override
	public Set<String> getSupportedEndpoints()
	{
		return SUPPORTED_ENDPOINTS;
	}
}
