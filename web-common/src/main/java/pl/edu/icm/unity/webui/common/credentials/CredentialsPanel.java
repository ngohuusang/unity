/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalPreviousCredentialException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Allows to change a credential.
 * @author K. Benedyczak
 */
public class CredentialsPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, CredentialsPanel.class);
	private CredentialManagement credMan;
	private CredentialRequirementManagement credReqMan;
	private EntityCredentialManagement ecredMan;
	private EntityManagement entityMan;
	private CredentialEditorRegistry credEditorReg;
	private UnityMessageSource msg;
	private boolean changed = false;
	private Entity entity;
	private final long entityId;
	private final boolean simpleMode;
	private boolean askAboutCurrent;
	
	private Map<String, CredentialDefinition> credentials;
	
	private SafePanel statuses;
	private ComboBox<CredentialDefinition> credential;
	private Label status;
	private HtmlConfigurableLabel description;
	private SafePanel credentialStateInfo;
	private SafePanel editor;
	private Button update;
	private Button clear;
	private Button invalidate;
	private CredentialEditor credEditor;
	
	/**
	 * 
	 * @param msg
	 * @param entityId
	 * @param authnMan
	 * @param ecredMan
	 * @param credEditorReg
	 * @param simpleMode if true then admin-only action buttons (credential reset/outdate) are not shown.
	 * @throws Exception
	 */
	public CredentialsPanel(UnityMessageSource msg, long entityId, CredentialManagement credMan, 
			EntityCredentialManagement ecredMan, EntityManagement entityMan,
			CredentialRequirementManagement credReqMan,
			CredentialEditorRegistry credEditorReg, boolean simpleMode) 
					throws Exception
	{
		this.msg = msg;
		this.credMan = credMan;
		this.ecredMan = ecredMan;
		this.entityId = entityId;
		this.entityMan = entityMan;
		this.credReqMan = credReqMan;
		this.credEditorReg = credEditorReg;
		this.simpleMode = simpleMode;
		init();
	}

	private void init() throws Exception
	{
		loadCredentials();
		
		if (credentials.size() == 0)
		{
			addComponent(new Label(msg.getMessage("CredentialChangeDialog.noCredentials")));
			return;
		}
		
		statuses = new SafePanel(msg.getMessage("CredentialChangeDialog.statusAll"));
		statuses.addStyleName(Styles.vBorderLess.toString());
		
		Panel credentialPanel = new SafePanel();
		credentialPanel.addStyleName(Styles.vBorderLess.toString());
		
		credential = new ComboBox<CredentialDefinition>(msg.getMessage("CredentialChangeDialog.credential"));
		credential.setItemCaptionGenerator(item -> item.getDisplayedName().getValue(msg));
		credential.setItems(credentials.values());
		credential.setValue(credentials.values().iterator().next());
		credential.setEmptySelectionAllowed(false);
		credential.addValueChangeListener(e -> updateSelectedCredential());
		
		description = new HtmlConfigurableLabel();
		description.setCaption(msg.getMessage("CredentialChangeDialog.description"));
		status = new Label();
		status.setCaption(msg.getMessage("CredentialChangeDialog.status"));
		credentialStateInfo = new SafePanel(msg.getMessage("CredentialChangeDialog.credentialStateInfo"));
		editor = new SafePanel(msg.getMessage("CredentialChangeDialog.value"));
		
		HorizontalLayout buttonsBar = new HorizontalLayout();
		buttonsBar.setSpacing(true);
		buttonsBar.setMargin(false);
		clear = new Button(msg.getMessage("CredentialChangeDialog.clear"));
		clear.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				changeCredentialStatus(LocalCredentialState.notSet);
			}
		});
		if (!simpleMode)
			buttonsBar.addComponent(clear);
		invalidate = new Button(msg.getMessage("CredentialChangeDialog.invalidate"));
		invalidate.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				changeCredentialStatus(LocalCredentialState.outdated);
			}
		});
		if (!simpleMode)
			buttonsBar.addComponent(invalidate);
		update = new Button(msg.getMessage("CredentialChangeDialog.update"));
		update.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				updateCredential();
			}
		});
		buttonsBar.addComponent(update);

		FormLayout fl = new CompactFormLayout(description, status, credentialStateInfo, editor, buttonsBar);
		fl.setMargin(true);
		credentialPanel.setContent(fl);

		addComponents(statuses);
		
		if (credentials.size() > 1)
		{
			addComponent(credential);
		}
		addComponent(credentialPanel);
		setSpacing(true);
		setMargin(false);
		updateStatus();
		updateSelectedCredential();
	}

	public boolean isChanged()
	{
		return changed;
	}
	
	public boolean isCredentialRequirementEmpty()
	{
		return credentials.isEmpty();
	}	
	
	private void updateSelectedCredential()
	{
		CredentialDefinition chosen = credential.getValue();
		description.setValue(chosen.getDescription().getValue(msg));
		Map<String, CredentialPublicInformation> s = entity.getCredentialInfo().getCredentialsState();
		CredentialPublicInformation credPublicInfo = s.get(chosen.getName());
		status.setValue(msg.getMessage("CredentialStatus."+credPublicInfo.getState().toString()));
		credEditor = credEditorReg.getEditor(chosen.getTypeId());
		FormLayout credLayout = new CompactFormLayout();
		credLayout.setMargin(true);
		
		askAboutCurrent = isCurrentCredentialVerificationRequired(chosen);
		
		ComponentsContainer credEditorComp = credEditor.getEditor(askAboutCurrent, 
				chosen.getConfiguration(), true); 
		credLayout.addComponents(credEditorComp.getComponents());
		editor.setContent(credLayout);
		Component viewer = credEditor.getViewer(credPublicInfo.getExtraInformation());
		if (viewer == null)
		{
			credentialStateInfo.setVisible(false);
		} else
		{
			credentialStateInfo.setContent(viewer);
			credentialStateInfo.setVisible(true);
		}
		if (credPublicInfo.getState() == LocalCredentialState.notSet)
		{
			clear.setEnabled(false);
			invalidate.setEnabled(false);
		} else if (credPublicInfo.getState() == LocalCredentialState.outdated)
		{
			clear.setEnabled(true);
			invalidate.setEnabled(false);
		} else
		{
			clear.setEnabled(true);
			invalidate.setEnabled(true);
		}
	}
	
	private boolean isCurrentCredentialVerificationRequired(CredentialDefinition chosen)
	{
		EntityParam entityP = new EntityParam(entity.getId());
		try
		{
			return ecredMan.isCurrentCredentialRequiredForChange(entityP, chosen.getName());
		} catch (EngineException e)
		{
			log.debug("Got exception when asking about possibility to "
					+ "change the credential without providing the existing one."
					+ " Most probably the subsequent credential change will also fail.", e);
			return true;
		}
	}
	
	private void updateCredential()
	{
		String secrets, currentSecrets = null;
		try
		{
			if (askAboutCurrent)
				currentSecrets = credEditor.getCurrentValue();
			secrets = credEditor.getValue();
		} catch (IllegalCredentialException e)
		{
			return;
		}
		CredentialDefinition credDef = credential.getValue();
		EntityParam entityP = new EntityParam(entity.getId());
		try
		{
			if (askAboutCurrent)
				ecredMan.setEntityCredential(entityP, credDef.getName(), secrets, currentSecrets);
			else
				ecredMan.setEntityCredential(entityP, credDef.getName(), secrets);
		} catch (IllegalPreviousCredentialException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialChangeDialog.credentialUpdateError"), e);
			credEditor.setCredentialError(null);
			credEditor.setPreviousCredentialError(e.getMessage());
			return;
		}  catch (IllegalCredentialException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialChangeDialog.credentialUpdateError"), e);
			credEditor.setPreviousCredentialError(null);
			credEditor.setCredentialError(e);
			return;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialChangeDialog.credentialUpdateError"), e);
			return;
		}
		changed = true;
		loadEntity(entityP);
		updateStatus();
	}

	private void changeCredentialStatus(LocalCredentialState desiredState)
	{
		CredentialDefinition credDef = credential.getValue();
		EntityParam entityP = new EntityParam(entity.getId());
		try
		{
			ecredMan.setEntityCredentialStatus(entityP, credDef.getName(), desiredState);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialChangeDialog.credentialUpdateError"), e);
			return;
		}
		changed = true;
		loadEntity(entityP);
		updateStatus();
	}

	
	private void loadEntity(EntityParam entityP)
	{
		try
		{
			entity = entityMan.getEntity(entityP);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialChangeDialog.entityRefreshError"), e);
		}
	}
	
	private void updateStatus()
	{
		FormLayout contents = new CompactFormLayout();
		contents.setMargin(true);
		contents.setSpacing(true);
		
		Map<String, CredentialPublicInformation> state = entity.getCredentialInfo().getCredentialsState();
		for (Map.Entry<String, CredentialPublicInformation> s: state.entrySet())
		{
			I18nString displayedName = credentials.get(s.getKey()).getDisplayedName();
			Label label = new Label(displayedName.getValue(msg));
			if (s.getValue().getState() == LocalCredentialState.correct)
				label.setIcon(Images.ok.getResource());
			else if (s.getValue().getState() == LocalCredentialState.outdated)
				label.setIcon(Images.warn.getResource());
			else
				label.setIcon(Images.error.getResource());
			label.setDescription(msg.getMessage("CredentialStatus."+
				s.getValue().getState().toString()));
			contents.addComponents(label);
		}
		
		contents.addComponent(HtmlTag.horizontalLine());
		
		statuses.setContent(contents);
		updateSelectedCredential();
	}
	
	private void loadCredentials() throws Exception
	{
		try
		{
			entity = entityMan.getEntity(new EntityParam(entityId));
		} catch (Exception e)
		{
			throw new InternalException(msg.getMessage("CredentialChangeDialog.getEntityError"), e);
		}
		
		CredentialInfo ci = entity.getCredentialInfo();
		String credReqId = ci.getCredentialRequirementId();
		CredentialRequirements credReq = null;
		Collection<CredentialDefinition> allCreds = null;
		try
		{
			Collection<CredentialRequirements> allReqs = credReqMan.getCredentialRequirements();
			for (CredentialRequirements cr: allReqs)
				if (credReqId.equals(cr.getName()))
					credReq = cr;
			
		} catch (Exception e)
		{
			throw new InternalException(msg.getMessage("CredentialChangeDialog.cantGetCredReqs"), e);
		}
		
		if (credReq == null)
		{
			log.fatal("Can not find credential requirement information, for the one set for the entity: " 
					+ credReqId);
			throw new InternalException(msg.getMessage("CredentialChangeDialog.noCredReqDef"));
		}
		
		try
		{
			allCreds = credMan.getCredentialDefinitions();
		} catch (EngineException e)
		{
			throw new InternalException(msg.getMessage("CredentialChangeDialog.cantGetCredDefs"), e);
		}
		
		credentials = new HashMap<>();
		Set<String> required = credReq.getRequiredCredentials();
		for (CredentialDefinition credential: allCreds)
		{
			if (required.contains(credential.getName()))
				credentials.put(credential.getName(), credential);
		}
	}
}
