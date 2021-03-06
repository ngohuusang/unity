/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;
import com.vaadin.v7.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportManagement;
import pl.edu.icm.unity.engine.api.authn.SandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteSandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.AuthNTile;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.OutdatedCredentialDialog;
import pl.edu.icm.unity.webui.authn.SelectedAuthNPanel;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.forms.reg.InsecureRegistrationFormLauncher;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormsChooserComponent;

/**
 * Base class of Vaadin UI of the sandbox application. This servlet is based on {@link AuthenticationUI} 
 * but authenticators are overwritten with those provided by a concrete subclass.
 *  
 * @author Roman Krysinski
 */
public abstract class SandboxUIBase extends AuthenticationUI 
{
	private static final String DEBUG_ID = "sbox";
	public static final String PROFILE_VALIDATION = "validate";

	private List<AuthenticationOptionDescription> authnList;
	private boolean debug;
	private boolean validationMode;
	protected AuthenticatorSupportManagement authenticatorsManagement;

	public SandboxUIBase(UnityMessageSource msg,
			LocaleChoiceComponent localeChoice,
			WebAuthenticationProcessor authnProcessor,
			RegistrationFormsChooserComponent formsChooser,
			InsecureRegistrationFormLauncher formLauncher,
			ExecutorsService execService,
			AuthenticatorSupportManagement authenticatorsManagement,
			EntityManagement idsMan,
			ObjectFactory<OutdatedCredentialDialog> outdatedCredentialDialogFactory)
	{
		super(msg, localeChoice, authnProcessor, formsChooser, formLauncher, execService, idsMan,
				null, outdatedCredentialDialogFactory);
		this.authenticatorsManagement = authenticatorsManagement;
	}

	@Override
	protected SelectedAuthNPanel createSelectedAuthNPanel()
	{
		return new SandboxSelectedAuthNPanel(msg, authnProcessor, idsMan, formLauncher, 
				execService, cancelHandler, endpointDescription.getRealm(),
				endpointDescription.getEndpoint().getContextAddress());
	}

	protected abstract List<AuthenticationOptionDescription> getAllVaadinAuthenticators(
			List<AuthenticationOption> endpointAuthenticators);
	
	@Override
	public void configure(ResolvedEndpoint description,
			List<AuthenticationOption> authenticators,
			EndpointRegistrationConfiguration regCfg, Properties endpointProperties) 
	{
		
		this.authnList      = getAllVaadinAuthenticators(authenticators);
		this.authenticators = getAuthenticatorUIs(authnList);
		
		this.registrationConfiguration = new EndpointRegistrationConfiguration(false);
		this.endpointDescription = description;
		//TODO - check if this was needed?
		//this.endpointDescription.setAuthenticatorSets(authnList);
		config = prepareConfiguration(endpointProperties);
	}
	
	/**
	 * @param endpointProperties
	 * @return configuration of the sandbox based on the properties of the base endpoint
	 */
	protected VaadinEndpointProperties prepareConfiguration(Properties endpointProperties)
	{
		Properties stripDown = new Properties();
		Map<Object, Object> reduced = endpointProperties.entrySet().stream().filter(entry -> {
			String key = (String) entry.getKey();
			return !(key.endsWith(VaadinEndpointProperties.ENABLE_REGISTRATION) || 
					key.endsWith(VaadinEndpointProperties.ENABLED_REGISTRATION_FORMS) ||
					key.endsWith(VaadinEndpointProperties.PRODUCTION_MODE) ||
					key.endsWith(VaadinEndpointProperties.WEB_CONTENT_PATH));
		}).collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
		stripDown.putAll(reduced);
		return new VaadinEndpointProperties(stripDown);
	}
	
	
	@Override
	protected void appInit(VaadinRequest request) 
	{
		if (authenticators.size() == 0)
		{
			noRemoteAuthnUI();
			return;
		}
		
		super.appInit(request);
		
		setSandboxCallbackForAuthenticators();
		
		setHeaderTitle(msg.getMessage("SandboxUI.headerTitle"));
		
		VaadinRequest vaadinRequest = VaadinService.getCurrentRequest();
		validationMode = vaadinRequest.getParameter(PROFILE_VALIDATION) != null;
		debug = vaadinRequest.getParameter(DEBUG_ID) == null;
		customizeUI();
	}
	
	protected void customizeUI()
	{
	}
	
	private void noRemoteAuthnUI()
	{
		VerticalLayout main = new VerticalLayout();
		Label errorLabel = new Label(msg.getMessage("SandboxUI.noRemoteAuthNTitle"));
		Label errorDescLabel = new Label(msg.getMessage("SandboxUI.noRemoteAuthNDesc"));
		errorLabel.addStyleName(Styles.error.toString());
		errorLabel.addStyleName(Styles.textXLarge.toString());

		Button close = new Button(msg.getMessage("close"));
		close.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				Exception errorE = new Exception(msg.getMessage("SandboxUI.noRemoteAuthNTitle"));
				RemotelyAuthenticatedInput dummy = new RemotelyAuthenticatedInput("no-idp");
				SandboxAuthnContext error = new RemoteSandboxAuthnContext(errorE, "", dummy);
				sandboxRouter.fireEvent(new SandboxAuthnEvent(error));
				JavaScript.getCurrent().execute("window.close();");
			}
		});
		main.addComponents(errorLabel, errorDescLabel, close);
		setContent(main);
	}
	
	private class SandboxAuthnResultCallbackImpl implements SandboxAuthnResultCallback
	{
		@Override
		public void sandboxedAuthenticationDone(SandboxAuthnContext ctx)
		{
			sandboxRouter.fireEvent(new SandboxAuthnEvent(ctx));
			cancelAuthentication();
			if (isDebug()) 
			{
				JavaScript.getCurrent().execute("window.close();");
			}
		}
	}
	
	private void setSandboxCallbackForAuthenticators() 
	{
		if (authenticators == null)
			return;
		for (AuthNTile tile: selectorPanel.getTiles())
		{
			Collection<VaadinAuthenticationUI> authnUIs = tile.getAuthenticators().values();
			for (VaadinAuthenticationUI authUI : authnUIs)
			{
				authUI.setSandboxAuthnResultCallback(new SandboxAuthnResultCallbackImpl());
			}
		}
	}

	private void cancelAuthentication() 
	{
		for (AuthNTile tile: selectorPanel.getTiles())
		{
			Collection<VaadinAuthenticationUI> authnUIs = tile.getAuthenticators().values();
			for (VaadinAuthenticationUI authUI : authnUIs)
			{
				authUI.cancelAuthentication();
			}
		}
	}
	
	private List<AuthenticationOption> getAuthenticatorUIs(List<AuthenticationOptionDescription> authnList) 
	{
		try
		{
			return authenticatorsManagement.getAuthenticatorUIs(authnList);
		} catch (EngineException e)
		{
			throw new IllegalStateException("Can not initialize sandbox UI", e);
		}
	}
	
	/**
	 * If sandbox URL contains {@value DEBUG_ID} then it's assumed that
	 * it works in debug mode - just for testing purposes when running sandbox
	 * "by hand" in browser.
	 * 
	 * @return false if debug mode assumed
	 */
	private boolean isDebug() 
	{
		return debug;
	}	
	
	protected boolean isProfileValidation()
	{
		return validationMode;
	}
}
