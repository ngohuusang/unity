/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.annotations.Theme;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.saml.idp.web.filter.IdpConsentDeciderServlet;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.UnityEndpointUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.TopHeaderLight;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiresDialogLauncher;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import pl.edu.icm.unity.webui.idpcommon.ExposedSelectableAttributesComponent;
import pl.edu.icm.unity.webui.idpcommon.IdPButtonsBar;
import pl.edu.icm.unity.webui.idpcommon.IdPButtonsBar.Action;
import pl.edu.icm.unity.webui.idpcommon.IdentitySelectorComponent;
import pl.edu.icm.unity.webui.idpcommon.SPInfoComponent;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * The main UI of the SAML web IdP. Fairly simple: shows who asks, what is going to be sent,
 * and optionally allows for some customization. This UI is shown always after the user was authenticated
 * and when the SAML request was properly pre-processed.
 *  
 * @author K. Benedyczak
 */
@Component("SamlIdPWebUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class SamlIdPWebUI extends UnityEndpointUIBase implements UnityWebUI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlIdPWebUI.class);
	protected UnityMessageSource msg;
	protected IdPEngine idpEngine;
	protected FreemarkerHandler freemarkerHandler;
	protected AttributeHandlerRegistry handlersRegistry;
	protected IdentityTypeSupport identityTypeSupport;
	protected PreferencesManagement preferencesMan;
	protected WebAuthenticationProcessor authnProcessor;
	protected SessionManagement sessionMan;
	protected IdentitySelectorComponent idSelector;
	protected ExposedSelectableAttributesComponent attrsPresenter;
	
	protected AuthnResponseProcessor samlProcessor;
	protected SamlResponseHandler samlResponseHandler;
	protected CheckBox rememberCB;
	private AttributeTypeManagement attrsMan;
	protected AttributeTypeSupport aTypeSupport;

	@Autowired
	public SamlIdPWebUI(UnityMessageSource msg, FreemarkerHandler freemarkerHandler,
			AttributeHandlerRegistry handlersRegistry, PreferencesManagement preferencesMan,
			WebAuthenticationProcessor authnProcessor, IdPEngine idpEngine,
			IdentityTypeSupport identityTypeSupport, SessionManagement sessionMan, 
			AttributeTypeManagement attrsMan, 
			EnquiresDialogLauncher enquiryDialogLauncher,
			AttributeTypeSupport aTypeSupport)
	{
		super(msg, enquiryDialogLauncher);
		this.msg = msg;
		this.freemarkerHandler = freemarkerHandler;
		this.handlersRegistry = handlersRegistry;
		this.preferencesMan = preferencesMan;
		this.authnProcessor = authnProcessor;
		this.idpEngine = idpEngine;
		this.identityTypeSupport = identityTypeSupport;
		this.sessionMan = sessionMan;
		this.attrsMan = attrsMan;
		this.aTypeSupport = aTypeSupport;
	}

	protected TranslationResult getUserInfo(SAMLAuthnContext samlCtx, AuthnResponseProcessor processor) 
			throws EngineException
	{
		String profile = samlCtx.getSamlConfiguration().getValue(CommonIdPProperties.TRANSLATION_PROFILE);
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		return idpEngine.obtainUserInformationWithEnrichingImport(new EntityParam(ae.getEntityId()), 
				processor.getChosenGroup(), profile, 
				samlProcessor.getIdentityTarget(), Optional.empty(), 
				"SAML2", SAMLConstants.BINDING_HTTP_REDIRECT,
				processor.isIdentityCreationAllowed(),
				samlCtx.getSamlConfiguration());
	}

	@Override
	protected void appInit(VaadinRequest request)
	{
		SAMLAuthnContext samlCtx = SAMLContextSupport.getContext();
		samlProcessor = new AuthnResponseProcessor(aTypeSupport, samlCtx, 
				Calendar.getInstance(TimeZone.getTimeZone("UTC")));
		samlResponseHandler = new SamlResponseHandler(freemarkerHandler, samlProcessor);
		
		VerticalLayout vmain = new VerticalLayout();
		vmain.setMargin(false);
		vmain.setSpacing(false);
		I18nString displayedName = endpointDescription.getEndpoint().getConfiguration().getDisplayedName();
		TopHeaderLight header = new TopHeaderLight(displayedName.getValue(msg), msg);
		vmain.addComponent(header);

		
		VerticalLayout contents = new VerticalLayout();
		contents.addStyleName(Styles.maxWidthColumn.toString());
		vmain.addComponent(contents);
		vmain.setComponentAlignment(contents, Alignment.TOP_CENTER);
		
		try
		{
			createInfoPart(samlCtx, contents);

			createExposedDataPart(samlCtx, contents);

			createButtonsPart(samlCtx, contents);

			setContent(vmain);

			loadPreferences(samlCtx);
		} catch (EopException e)
		{
			//OK
		}
	}

	protected void createInfoPart(SAMLAuthnContext samlCtx, VerticalLayout contents)
	{
		String samlRequester = samlCtx.getRequest().getIssuer().getStringValue();
		String returnAddress = samlCtx.getRequest().getAssertionConsumerServiceURL();
		if (returnAddress == null)
			returnAddress = samlCtx.getSamlConfiguration().getReturnAddressForRequester(
					samlCtx.getRequest().getIssuer());

		Label info1 = new Label(msg.getMessage("SamlIdPWebUI.info1"));
		info1.addStyleName(Styles.vLabelH1.toString());
		SPInfoComponent spInfo = new SPInfoComponent(msg, null, samlRequester, returnAddress);
		Label spc1 = HtmlTag.br();
		Label info2 = new Label(msg.getMessage("SamlIdPWebUI.info2"));
		
		contents.addComponents(info1, spInfo, spc1, info2);
	}

	protected void createExposedDataPart(SAMLAuthnContext samlCtx, VerticalLayout contents) throws EopException
	{
		SafePanel exposedInfoPanel = new SafePanel();
		contents.addComponent(exposedInfoPanel);
		VerticalLayout eiLayout = new VerticalLayout();
		eiLayout.setWidth(100, Unit.PERCENTAGE);
		exposedInfoPanel.setContent(eiLayout);
		try
		{
			TranslationResult translationResult = getUserInfo(samlCtx, samlProcessor);
			handleRedirectIfNeeded(translationResult);
			createIdentityPart(translationResult, eiLayout);
			eiLayout.addComponent(HtmlTag.br());
			createAttributesPart(translationResult, eiLayout, samlCtx.getSamlConfiguration().getBooleanValue(
					SamlIdpProperties.USER_EDIT_CONSENT));
		} catch (SAMLRequesterException e)
		{
			//we kill the session as the user may want to log as different user if has access to several entities.
			log.debug("SAML problem when handling client request", e);
			samlResponseHandler.handleException(e, true);
			return;
		}  catch (EopException eop) 
		{
			throw eop;
		} catch (Exception e)
		{
			log.error("Engine problem when handling client request", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			samlResponseHandler.handleException(e, true);
			return;
		}
		
		rememberCB = new CheckBox(msg.getMessage("SamlIdPWebUI.rememberSettings"));
		contents.addComponent(rememberCB);
	}
	
	private void handleRedirectIfNeeded(TranslationResult userInfo) 
			throws IOException, EopException
	{
		String redirectURL = userInfo.getRedirectURL();
		if (redirectURL != null)
		{
			Page.getCurrent().open(redirectURL, null);
			throw new EopException();
		}
	}
	
	protected void createIdentityPart(TranslationResult translationResult, VerticalLayout contents) 
			throws EngineException, SAMLRequesterException
	{
		List<IdentityParam> validIdentities = samlProcessor.getCompatibleIdentities(
				translationResult.getIdentities());
		idSelector = new IdentitySelectorComponent(msg, identityTypeSupport, validIdentities);
		contents.addComponent(idSelector);
	}
	
	protected void createAttributesPart(TranslationResult translationResult, 
			VerticalLayout contents, boolean userCanEdit) throws EngineException
	{
		attrsPresenter = new ExposedSelectableAttributesComponent(msg, handlersRegistry, attrsMan, 
				aTypeSupport, translationResult.getAttributes(), userCanEdit);
		contents.addComponent(attrsPresenter);
	}
	
	protected void createButtonsPart(final SAMLAuthnContext samlCtx, VerticalLayout contents)
	{
		IdPButtonsBar buttons = new IdPButtonsBar(msg, authnProcessor, new IdPButtonsBar.ActionListener()
		{
			@Override
			public void buttonClicked(Action action)
			{
				try
				{
					if (Action.ACCEPT == action)
						confirm(samlCtx);
					else if (Action.DENY == action)
						decline();
				} catch (EopException e) 
				{
					//OK
				}
			}
		});
		
		contents.addComponent(buttons);
		contents.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);
	}
	
	
	protected void loadPreferences(SAMLAuthnContext samlCtx) throws EopException
	{
		try
		{
			SamlPreferences preferences = SamlPreferences.getPreferences(preferencesMan);
			SPSettings settings = preferences.getSPSettings(samlCtx.getRequest().getIssuer());
			updateUIFromPreferences(settings, samlCtx);
		} catch (EopException e)
		{
			throw e;
		} catch (Exception e)
		{
			log.error("Engine problem when processing stored preferences", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			samlResponseHandler.handleException(e, true);
			return;
		}
	}
	
	protected void updateUIFromPreferences(SPSettings settings, SAMLAuthnContext samlCtx) throws EngineException, EopException
	{
		if (settings == null)
			return;
		Map<String, Attribute> attribtues = settings.getHiddenAttribtues();
		attrsPresenter.setInitialState(attribtues);
		String selId = settings.getSelectedIdentity();
		idSelector.setSelected(selId);

		if (settings.isDoNotAsk())
		{
			if (settings.isDefaultAccept())
				confirm(samlCtx);
			else
				decline();
		}
	}
	
	/**
	 * Applies UI selected values to the given preferences object
	 * @param preferences
	 * @param samlCtx
	 * @param defaultAccept
	 * @throws EngineException
	 */
	protected void updatePreferencesFromUI(SamlPreferences preferences, SAMLAuthnContext samlCtx, boolean defaultAccept) 
			throws EngineException
	{
		if (!rememberCB.getValue())
			return;
		NameIDType reqIssuer = samlCtx.getRequest().getIssuer();
		SPSettings settings = preferences.getSPSettings(reqIssuer);
		settings.setDefaultAccept(defaultAccept);
		settings.setDoNotAsk(true);
		settings.setHiddenAttribtues(attrsPresenter.getHiddenAttributes());

		String identityValue = idSelector.getSelectedIdentityForPreferences();
		if (identityValue != null)
			settings.setSelectedIdentity(identityValue);
		preferences.setSPSettings(reqIssuer, settings);
	}
	
	protected void storePreferences(boolean defaultAccept)
	{
		try
		{
			SAMLAuthnContext samlCtx = SAMLContextSupport.getContext();
			SamlPreferences preferences = SamlPreferences.getPreferences(preferencesMan);
			updatePreferencesFromUI(preferences, samlCtx, defaultAccept);
			SamlPreferences.savePreferences(preferencesMan, preferences);
		} catch (EngineException e)
		{
			log.error("Unable to store user's preferences", e);
		}
	}

	protected void decline() throws EopException
	{
		storePreferences(false);
		AuthenticationException ea = new AuthenticationException("Authentication was declined");
		samlResponseHandler.handleException(ea, false);
	}
	
	protected void confirm(SAMLAuthnContext samlCtx) throws EopException
	{
		storePreferences(true);
		ResponseDocument respDoc;
		try
		{
			respDoc = samlProcessor.processAuthnRequest(idSelector.getSelectedIdentity(), 
					getExposedAttributes());
		} catch (Exception e)
		{
			samlResponseHandler.handleException(e, false);
			return;
		}
		addSessionParticipant(samlCtx, samlProcessor.getAuthenticatedSubject().getNameID(), 
				samlProcessor.getSessionId());
		samlResponseHandler.returnSamlResponse(respDoc);
	}
	
	protected Collection<Attribute> getExposedAttributes()
	{
		return attrsPresenter.getUserFilteredAttributes().values();
	}
	
	protected void addSessionParticipant(SAMLAuthnContext samlCtx, NameIDType returnedSubject,
			String sessionId)
	{
		IdpConsentDeciderServlet.addSessionParticipant(samlCtx, returnedSubject, sessionId, sessionMan);
	}
}
