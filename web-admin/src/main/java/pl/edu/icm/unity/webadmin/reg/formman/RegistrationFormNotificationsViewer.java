/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.webadmin.msgtemplate.SimpleMessageTemplateViewer;

/**
 * Viewer of {@link RegistrationFormNotifications}
 * @author K. Benedyczak
 */
public class RegistrationFormNotificationsViewer extends BaseFormNotificationsViewer
{
	private SimpleMessageTemplateViewer invitationTemplate;
	private SimpleMessageTemplateViewer submittedTemplate;
	
	public RegistrationFormNotificationsViewer(UnityMessageSource msg,
			MessageTemplateManagement msgTempMan)
	{
		super(msg, msgTempMan);
		initMyUI();
	}

	private void initMyUI()
	{
		submittedTemplate = new SimpleMessageTemplateViewer(msg.getMessage(
				"RegistrationFormViewer.submittedTemplate"),
				msg, msgTempMan);
		invitationTemplate = new SimpleMessageTemplateViewer(
				msg.getMessage("RegistrationFormViewer.invitationTemplate"),
				msg, msgTempMan);
		addComponents(submittedTemplate, invitationTemplate);
	}
	
	public void clear()
	{
		super.clear();
		invitationTemplate.clearContent();
		submittedTemplate.clearContent();
	}
	
	public void setValue(RegistrationFormNotifications notCfg)
	{
		super.setValue(notCfg);
		invitationTemplate.setInput(notCfg.getInvitationTemplate());
		submittedTemplate.setInput(notCfg.getSubmittedTemplate());
	}
}
