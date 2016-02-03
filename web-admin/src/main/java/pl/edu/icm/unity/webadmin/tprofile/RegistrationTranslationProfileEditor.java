/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.registries.RegistrationTranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.TranslationProfileInstance;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ProfileType;

/**
 * Component to edit or add a registration form translation profile
 * 
 * @author K. Benedyczak
 * 
 */
public class RegistrationTranslationProfileEditor extends TranslationProfileEditor
{
	public RegistrationTranslationProfileEditor(UnityMessageSource msg,
			RegistrationTranslationActionsRegistry registry, RegistrationTranslationProfile toEdit,
			AttributesManagement attrsMan, IdentitiesManagement idMan, AuthenticationManagement authnMan,
			GroupsManagement groupsMan) throws EngineException
	{
		super(msg, registry, ProfileType.REGISTRATION, toEdit, attrsMan, idMan, authnMan, groupsMan);
	}

	@Override
	protected void initUI(TranslationProfileInstance<?, ?> toEdit)
	{
		super.initUI(toEdit);
		name.setVisible(false);
		description.setVisible(false);
	}
}
