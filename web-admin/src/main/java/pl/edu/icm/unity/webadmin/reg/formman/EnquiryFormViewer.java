/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.common.CompactFormLayout;

/**
 * Read only UI displaying a {@link EnquiryForm}.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class EnquiryFormViewer extends BaseFormViewer
{
	private UnityMessageSource msg;
	private MessageTemplateManagement msgTempMan;
	
	private TabSheet tabs;
	
	private Label type;
	private Label targetGroups;
	private Label publicLink;
	
	private EnquiryFormNotificationsViewer notViewer;
	private RegistrationTranslationProfileViewer translationProfile;
	private RegistrationActionsRegistry registrationActionsRegistry;
	private SharedEndpointManagement sharedEndpointMan;
	
	@Autowired
	public EnquiryFormViewer(UnityMessageSource msg, RegistrationActionsRegistry registrationActionsRegistry,
			MessageTemplateManagement msgTempMan, SharedEndpointManagement sharedEndpointMan)
	{
		super(msg);
		this.msg = msg;
		this.registrationActionsRegistry = registrationActionsRegistry;
		this.msgTempMan = msgTempMan;
		this.sharedEndpointMan = sharedEndpointMan;
		initUI();
	}
	
	public void setInput(EnquiryForm form)
	{
		super.setInput(form);

		if (form == null)
		{
			tabs.setVisible(false);
			return;
		}
		tabs.setVisible(true);
		
		type.setValue(msg.getMessage("EnquiryType." + form.getType().name()));
		targetGroups.setValue(Arrays.stream(form.getTargetGroups()).
				collect(Collectors.joining(", ")));
		publicLink.setValue(PublicRegistrationURLSupport.getWellknownEnquiryLink(form.getName(), sharedEndpointMan));
		
		EnquiryFormNotifications notCfg = form.getNotificationsConfiguration();
		if (notCfg != null)
			notViewer.setValue(notCfg);
		else
			notViewer.clear();
		
		TranslationProfile tProfile = new TranslationProfile(
				form.getTranslationProfile().getName(),
				"",
				ProfileType.REGISTRATION,
				form.getTranslationProfile().getRules());
		translationProfile.setInput(tProfile, registrationActionsRegistry);
	}
	
	private void initUI()
	{
		tabs = new TabSheet();
		initMainTab();
		initCollectedTab();
		initAssignedTab();
		initLayoutTab();
		addComponent(tabs);
	}
	
	private void initCollectedTab()
	{
		FormLayout main = new CompactFormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		wrapper.setSpacing(true);
		
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.collectedTab"));

		setupCommonFormInformationComponents();
		
		main.addComponents(displayedName, formInformation, collectComments);
		main.addComponent(getCollectedDataInformation());
	}
	
	private void initAssignedTab()
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(true);
		wrapper.setSpacing(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.assignedTab"));
		
		translationProfile = new RegistrationTranslationProfileViewer(msg);
		
		wrapper.addComponent(translationProfile);
	}
	
	private void initMainTab()
	{
		FormLayout main = new CompactFormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		wrapper.setSpacing(false);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.mainTab"));
		
		setupNameAndDesc();
		
		type = new Label();
		type.setCaption(msg.getMessage("EnquiryFormViewer.type"));
		
		targetGroups = new Label();
		targetGroups.setCaption(msg.getMessage("EnquiryFormViewer.targetGroups"));
		
		publicLink = new Label();
		publicLink.setCaption(msg.getMessage("RegistrationFormViewer.publicLink"));
		
		notViewer = new EnquiryFormNotificationsViewer(msg, msgTempMan);
		main.addComponents(name, description, type, targetGroups, publicLink);
		notViewer.addToLayout(main);
	}
	
	
	private void initLayoutTab()
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(true);
		wrapper.setSpacing(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.layoutTab"));
		wrapper.addComponent(layout);
	}
}
