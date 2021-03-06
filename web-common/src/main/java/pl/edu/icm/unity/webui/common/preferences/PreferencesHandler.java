/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.preferences;

import java.util.Set;

/**
 * Provides UI for managing preferences of a specific type as SAML preferences.
 * 
 * Important: implementations must be immutable. Only one instance is always used.
 * @author K. Benedyczak
 */
public interface PreferencesHandler
{
	/**
	 * @return string (localized) which is user friendly representation of the supported preference type.
	 */
	public String getPreferenceLabel();
	
	/**
	 * 
	 * @return id of the supported preference type
	 */
	public String getPreferenceId();
	
	/**
	 * @param value a preference value to show & edit. Can be null - then a default preference value must be used.
	 * @return a component allowing to show and edit the preference
	 */
	public PreferencesEditor getPreferencesEditor(String value);
	
	/**
	 * @return set of endpoint identifiers. The preferences are assumed to be relevant for 
	 * all of the returned endpoints. The core code may decide not to expose this preferences handler,
	 * if none of the supported endpoints is deployed. If the method returns null or empty set,
	 * then it is assumed that the preferences are relevant regardless of the deployed endpoints.
	 */
	public Set<String> getSupportedEndpoints();
	
}
