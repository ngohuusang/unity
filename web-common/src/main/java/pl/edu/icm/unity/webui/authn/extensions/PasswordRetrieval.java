/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.extensions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.Reindeer;

import eu.unicore.util.configuration.ConfigurationException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.PasswordExchange;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.credreset.CredentialReset1Dialog;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * Retrieves passwords using a Vaadin widget.
 * 
 * @author K. Benedyczak
 */
public class PasswordRetrieval implements CredentialRetrieval, VaadinAuthentication
{
	private UsernameProvider usernameProvider;
	private PasswordExchange credentialExchange;
	private PasswordField passwordField;
	
	private UnityMessageSource msg;
	private CredentialEditor credEditor;
	private String name;
	
	public PasswordRetrieval(UnityMessageSource msg, CredentialEditor credEditor)
	{
		this.msg = msg;
		this.credEditor = credEditor;
	}
	
	@Override
	public String getBindingName()
	{
		return VaadinAuthentication.NAME;
	}

	@Override
	public String getSerializedConfiguration()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("name", name);
		try
		{
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize web-based password retrieval configuration to JSON", e);
		}
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		try
		{
			JsonNode root = Constants.MAPPER.readTree(json);
			name = root.get("name").asText();
		} catch (Exception e)
		{
			throw new ConfigurationException("The configuration of the web-" +
					"based password retrieval can not be parsed", e);
		}
	}

	@Override
	public void setCredentialExchange(CredentialExchange e)
	{
		this.credentialExchange = (PasswordExchange) e;
	}

	@Override
	public boolean needsCommonUsernameComponent()
	{
		return true;
	}

	@Override
	public Component getComponent()
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(true);
		String label = name.trim().equals("") ? msg.getMessage("WebPasswordRetrieval.password") : name;
		passwordField = new PasswordField(label);
		ret.addComponent(passwordField);
		
		if (credentialExchange.getCredentialResetBackend().getSettings().isEnabled())
		{
			Button reset = new Button(msg.getMessage("WebPasswordRetrieval.forgottenPassword"));
			reset.setStyleName(Reindeer.BUTTON_LINK);
			ret.addComponent(reset);
			ret.setComponentAlignment(reset, Alignment.TOP_RIGHT);
			reset.addClickListener(new ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					showResetDialog();
				}
			});
		}
		
		return ret;
	}

	@Override
	public void setUsernameCallback(UsernameProvider usernameCallback)
	{
		this.usernameProvider = usernameCallback;
	}

	@Override
	public AuthenticationResult getAuthenticationResult()
	{
		String username = usernameProvider.getUsername();
		String password = passwordField.getValue();
		if (username.equals("") && password.equals(""))
		{
			passwordField.setComponentError(new UserError(
					msg.getMessage("WebPasswordRetrieval.noPassword")));
			return new AuthenticationResult(Status.notApplicable, null);
		}
		try
		{
			AuthenticatedEntity authenticatedEntity = credentialExchange.checkPassword(username, password);
			passwordField.setComponentError(null);
			return new AuthenticationResult(Status.success, authenticatedEntity);
		} catch (Exception e)
		{
			passwordField.setComponentError(new UserError(
					msg.getMessage("WebPasswordRetrieval.wrongPassword")));
			passwordField.setValue("");
			return new AuthenticationResult(Status.deny, null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getLabel()
	{
		return name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Resource getImage()
	{
		return null;
	}
	
	private void showResetDialog()
	{
		CredentialReset1Dialog dialog = new CredentialReset1Dialog(msg, 
				credentialExchange.getCredentialResetBackend(), credEditor);
		dialog.show();
	}
}










