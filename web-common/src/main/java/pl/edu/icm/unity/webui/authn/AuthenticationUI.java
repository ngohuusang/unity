/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.Cookie;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.CookieHelper;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.VaadinEndpointProperties.ScaleMode;
import pl.edu.icm.unity.webui.VaadinEndpointProperties.TileMode;
import pl.edu.icm.unity.webui.authn.AuthNTile.SelectionChangedListener;
import pl.edu.icm.unity.webui.authn.SelectedAuthNPanel.AuthenticationListener;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.forms.reg.InsecureRegistrationFormLauncher;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormChooserDialog;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormsChooserComponent;



/**
 * Vaadin UI of the authentication application. Displays configured authenticators and 
 * coordinates authentication.
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component("AuthenticationUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
@PreserveOnRefresh
public class AuthenticationUI extends UnityUIBase implements UnityWebUI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticationUI.class);
	private static final String LAST_AUTHN_COOKIE = "lastAuthnUsed";
	/**
	 * Query param allowing for selecting IdP in request to the endpoint
	 */
	public static final String IDP_SELECT_PARAM = "uy_select_authn";

	protected LocaleChoiceComponent localeChoice;
	protected WebAuthenticationProcessor authnProcessor;
	protected RegistrationFormsChooserComponent formsChooser;
	protected InsecureRegistrationFormLauncher formLauncher;
	protected ExecutorsService execService;
	protected AuthenticationTopHeader headerUIComponent;
	protected SelectedAuthNPanel authenticationPanel;
	protected AuthNTiles selectorPanel;
	protected List<AuthenticationOption> authenticators;
	protected EndpointRegistrationConfiguration registrationConfiguration;
	protected EntityManagement idsMan;
	private InputTranslationEngine inputTranslationEngine;
	private VerticalLayout topLevelLayout;
	private ObjectFactory<OutdatedCredentialDialog> outdatedCredentialDialogFactory;
	
	@Autowired
	public AuthenticationUI(UnityMessageSource msg, LocaleChoiceComponent localeChoice,
			WebAuthenticationProcessor authnProcessor,
			RegistrationFormsChooserComponent formsChooser,
			InsecureRegistrationFormLauncher formLauncher,
			ExecutorsService execService, @Qualifier("insecure") EntityManagement idsMan,
			InputTranslationEngine inputTranslationEngine,
			ObjectFactory<OutdatedCredentialDialog> outdatedCredentialDialogFactory)
	{
		super(msg);
		this.localeChoice = localeChoice;
		this.authnProcessor = authnProcessor;
		this.formsChooser = formsChooser;
		this.formLauncher = formLauncher;
		this.execService = execService;
		this.idsMan = idsMan;
		this.inputTranslationEngine = inputTranslationEngine;
		this.outdatedCredentialDialogFactory = outdatedCredentialDialogFactory;
	}


	@Override
	public void configure(ResolvedEndpoint description,
			List<AuthenticationOption> authenticators,
			EndpointRegistrationConfiguration registrationConfiguration,
			Properties genericEndpointConfiguration)
	{
		super.configure(description, authenticators, registrationConfiguration, genericEndpointConfiguration);
		this.authenticators = new ArrayList<>(authenticators);
		this.registrationConfiguration = registrationConfiguration;
	}
	
	@Override
	protected void appInit(final VaadinRequest request)
	{
		authenticationPanel = createSelectedAuthNPanel();
		authenticationPanel.addStyleName(Styles.minHeightAuthn.toString());

		
		List<AuthNTile> tiles = prepareTiles(authenticators);
		selectorPanel = new AuthNTiles(msg, tiles, authenticationPanel);

		switchAuthnOptionIfRequested(tiles.get(0).getFirstOptionId());

		//language choice and registration
		HorizontalLayout topBar = new HorizontalLayout();
		topBar.setWidth(100, Unit.PERCENTAGE);
		topBar.setSpacing(false);
		topBar.setMargin(false);
		Button registrationButton = buildRegistrationButton();
		if (registrationButton != null)
		{
			topBar.addComponent(registrationButton);
			topBar.setComponentAlignment(registrationButton, Alignment.TOP_RIGHT);
		}
		
		VerticalLayout main = new VerticalLayout();
		main.addComponent(topBar);
		main.setMargin(new MarginInfo(false, true, false, true));
		main.setWidth(100, Unit.PERCENTAGE);
		main.setHeightUndefined();

		main.addComponent(authenticationPanel);
		main.setComponentAlignment(authenticationPanel, Alignment.TOP_CENTER);
		
		authenticationPanel.setAuthenticationListener(new AuthenticationListener()
		{
			@Override
			public void authenticationStateChanged(boolean started)
			{
				if (tiles.size() > 1 || tiles.get(0).size() > 1)
					selectorPanel.setEnabled(!started);
			}

			@Override
			public void clearUI()
			{
				setContent(new VerticalLayout());
			}
		});
		
		if (tiles.size() > 1 || tiles.get(0).size() > 1)
		{
			HorizontalLayout tilesWrapper = new HorizontalLayout();
			tilesWrapper.addComponent(selectorPanel);
			tilesWrapper.setSpacing(false);
			tilesWrapper.setMargin(false);
			main.addComponent(tilesWrapper);
			main.setComponentAlignment(tilesWrapper, Alignment.TOP_CENTER);
			main.setExpandRatio(tilesWrapper, 1.0f);
		}
		
		topLevelLayout = new VerticalLayout();
		headerUIComponent = new AuthenticationTopHeader(msg.getMessage("AuthenticationUI.login", 
				endpointDescription.getEndpoint().getConfiguration().getDisplayedName().getValue(msg)), 
				localeChoice, msg);
		topLevelLayout.addComponents(headerUIComponent, main);
		topLevelLayout.setHeightUndefined();
		topLevelLayout.setWidth(100, Unit.PERCENTAGE);
		topLevelLayout.setExpandRatio(main, 1.0f);
		topLevelLayout.setSpacing(false);
		topLevelLayout.setMargin(false);
		
		setContent(topLevelLayout);
		setSizeFull();
		
		if (showOutdatedCredentialDialog())
			return;
		
		//Extra safety - it can happen that we entered the UI in pipeline of authentication,
		// if this UI expired in the meantime. Shouldn't happen often as heart of authentication UI
		// is beating very slowly but in case of very slow user we may still need to refresh.
		refresh(VaadinService.getCurrentRequest());
	}
	
	/**
	 * We may end up in authentication UI also after being properly logged in,
	 * when the credential is outdated. The credential change dialog must be displayed then.
	 * @return
	 */
	private boolean showOutdatedCredentialDialog()
	{
		WrappedSession vss = VaadinSession.getCurrent().getSession();
		LoginSession ls = (LoginSession) vss.getAttribute(LoginToHttpSessionBinder.USER_SESSION_KEY);
		if (ls != null && ls.isUsedOutdatedCredential())
		{
			setContent(new VerticalLayout());
			outdatedCredentialDialogFactory.getObject().init(authnProcessor).show();
			return true;
		}
		return false;
	}
	
	/**
	 * Overridden in sandboxed version
	 * @return
	 */
	protected SelectedAuthNPanel createSelectedAuthNPanel()
	{
		return new SelectedAuthNPanel(msg, authnProcessor, idsMan, formLauncher, 
				execService, cancelHandler, endpointDescription.getRealm(),
				getSandboxServletURLForAssociation(), sandboxRouter, 
				inputTranslationEngine, endpointDescription.getEndpoint().getContextAddress());
	}
	
	private List<AuthNTile> prepareTiles(List<AuthenticationOption> authenticators)
	{
		List<AuthNTile> ret = new ArrayList<>();
		List<AuthenticationOption> authNCopy = new ArrayList<>(authenticators);
		Set<String> tileKeys = config.getStructuredListKeys(VaadinEndpointProperties.AUTHN_TILES_PFX);
		int defPerRow = config.getIntValue(VaadinEndpointProperties.DEFAULT_PER_LINE);
		
		SelectionChangedListener selectionChangedListener = new SelectionChangedListener()
		{
			@Override
			public void selectionChanged(VaadinAuthenticationUI selectedAuthn,
					AuthenticationOption selectedOption, String globalId)
			{
				authenticationPanel.setAuthenticator(selectedAuthn, selectedOption, globalId);
				authenticationPanel.setVisible(true);
			}
		};
		
		for (String tileKey: tileKeys)
		{
			
			ScaleMode scaleMode = config.getScaleMode(tileKey);
			
			Integer perRow = config.getIntValue(tileKey + VaadinEndpointProperties.AUTHN_TILE_PER_LINE);
			if (perRow == null)
				perRow = defPerRow;
			
			TileMode tileMode = config.getEnumValue(tileKey + VaadinEndpointProperties.AUTHN_TILE_TYPE,
					TileMode.class);
			String displayedName = config.getLocalizedValue(tileKey + 
					VaadinEndpointProperties.AUTHN_TILE_DISPLAY_NAME, msg.getLocale());
			
			String spec = config.getValue(tileKey + VaadinEndpointProperties.AUTHN_TILE_CONTENTS);
			String[] specSplit = spec.split("[ ]+");
			List<AuthenticationOption> authNs = new ArrayList<>();
			Iterator<AuthenticationOption> authNIt = authNCopy.iterator();
			while (authNIt.hasNext())
			{
				AuthenticationOption ao = authNIt.next();
				for (String matching: specSplit)
				{
					if (ao.getId().startsWith(matching))
					{
						authNs.add(ao);
						authNIt.remove();
					}
				}
			}
			AuthNTile tile = tileMode == TileMode.simple ? 
				new AuthNTileSimple(authNs, scaleMode, perRow, selectionChangedListener, displayedName) : 
				new AuthNTileGrid(authNs, msg, selectionChangedListener, displayedName);
			log.debug("Adding tile with authenticators {}", tile.getAuthenticators().keySet());
			ret.add(tile);
		}
		
		if (!authNCopy.isEmpty())
		{
			AuthNTile defaultTile = new AuthNTileSimple(authNCopy, config.getDefaultScaleMode(), 
					defPerRow, selectionChangedListener, null);
			ret.add(defaultTile);
		}
		
		return ret;
	}

	public static Cookie createLastIdpCookie(String endpointPath, String idpKey)
	{
		Cookie selectedIdp = new Cookie(LAST_AUTHN_COOKIE, idpKey);
		selectedIdp.setMaxAge(3600*24*30);
		selectedIdp.setPath(endpointPath);
		selectedIdp.setHttpOnly(true);
		return selectedIdp;
	}
	
	private String getLastIdpFromRequestParam()
	{
		VaadinRequest req = VaadinService.getCurrentRequest();
		if (req == null)
			return null;
		return req.getParameter(IDP_SELECT_PARAM);
	}
	
	private String getLastIdpFromCookie()
	{
		VaadinRequest req = VaadinService.getCurrentRequest();
		if (req == null)
			return null;
		return CookieHelper.getCookie(req.getCookies(), LAST_AUTHN_COOKIE);
	}
	
	private Button buildRegistrationButton()
	{
		if (!registrationConfiguration.isShowRegistrationOption())
			return null;
		if (registrationConfiguration.getEnabledForms().size() > 0)
			formsChooser.setAllowedForms(registrationConfiguration.getEnabledForms());
		formsChooser.initUI(TriggeringMode.manualAtLogin);
		if (formsChooser.getDisplayedForms().size() == 0)
			return null;
		
		Button register = new Button(msg.getMessage("RegistrationFormChooserDialog.register"));
		register.addStyleName(Styles.vButtonLink.toString());
		
		register.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				showRegistrationDialog();
			}
		});
		register.setId("AuthenticationUI.registerButton");
		return register;
	}
	
	private void showRegistrationDialog()
	{
		if (formsChooser.getDisplayedForms().size() == 1)
		{
			RegistrationForm form = formsChooser.getDisplayedForms().get(0);
			formLauncher.showRegistrationDialog(form, 
					RemotelyAuthenticatedContext.getLocalContext(),
					TriggeringMode.manualAtLogin, 
					error -> handleRegistrationError(error, form.getName()));
		} else
		{
			RegistrationFormChooserDialog chooser = new RegistrationFormChooserDialog(
				msg, msg.getMessage("RegistrationFormChooserDialog.selectForm"), formsChooser);
			chooser.show();
		}
	}
	
	private void handleRegistrationError(Exception e, String formName)
	{
		log.info("Can't initialize registration form '" + formName + "' UI. "
				+ "It can be fine in some cases, but often means "
				+ "that the form should not be marked "
				+ "as public or its configuration is invalid: " + e.toString());
		if (log.isDebugEnabled())
			log.debug("Deatils: ", e);
		NotificationPopup.showError(msg, msg.getMessage("error"), 
				msg.getMessage("AuthenticationUI.registrationFormInitError"));
	}
	
	private void switchAuthnOptionIfRequested(String defaultVal)
	{
		String lastIdp = getLastIdpFromRequestParam(); 
		if (lastIdp == null)
			lastIdp = getLastIdpFromCookie();
		String initialOption = null;
		if (lastIdp != null)
		{
			AuthenticationOption lastAuthnOption = selectorPanel.getAuthenticationOptionById(lastIdp);
			if (lastAuthnOption != null)
				initialOption = lastIdp;
		}
		log.debug("Requested/cookie idp={}, is available={}", lastIdp, initialOption!=null);
		if (initialOption == null)
			initialOption = defaultVal;

		if (initialOption != null)
		{
			AuthenticationOption initAuthnOption = selectorPanel.getAuthenticationOptionById(initialOption);
			authenticationPanel.setAuthenticator(selectorPanel.getAuthenticatorById(initialOption), 
					initAuthnOption, initialOption);
			authenticationPanel.setVisible(true);
		}
	}
	
	@Override
	protected void refresh(VaadinRequest request) 
	{
		setContent(topLevelLayout);		//in case somebody refreshes UI which was previously replaced with empty
							//may happen that the following code will clean it but it is OK.
		switchAuthnOptionIfRequested(null);
		authenticationPanel.refresh(request);
	}
	
	protected void setHeaderTitle(String title) 
	{
		if (headerUIComponent != null)
		{
			headerUIComponent.setHeaderTitle(title);
		}
	}

}
