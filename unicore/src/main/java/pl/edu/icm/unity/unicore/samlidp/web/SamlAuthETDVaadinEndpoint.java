/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import java.util.Map;

import javax.servlet.Servlet;

import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.web.SamlAuthVaadinEndpoint;
import pl.edu.icm.unity.saml.idp.web.filter.ErrorHandler;
import pl.edu.icm.unity.saml.metadata.cfg.MetaDownloadManager;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessorFactory;
import pl.edu.icm.unity.saml.slo.SLOReplyInstaller;


/**
 * Extends a simple {@link SamlAuthVaadinEndpoint}, changing the SAML parse servlet to 
 * {@link SamlETDParseServlet}. 
 * 
 * @author K. Benedyczak
 */
public class SamlAuthETDVaadinEndpoint extends SamlAuthVaadinEndpoint
{
	public static final String SAML_CONSUMER_SERVLET_PATH = "/saml2unicoreIdp-web";
	
	public SamlAuthETDVaadinEndpoint(NetworkServer server, ApplicationContext applicationContext,
			FreemarkerHandler freemarkerHandler,
			PKIManagement pkiManagement, ExecutorsService executorsService,
			Map<String, RemoteMetaManager> remoteMetadataManagers, MetaDownloadManager downloadManager, 
			UnityServerConfiguration mainConfig, SAMLLogoutProcessorFactory logoutProcessorFactory, 
			SLOReplyInstaller sloReplyInstaller, UnicoreIdpConsentDeciderServletFactory dispatcherServletFactory,
			UnityMessageSource msg)
	{
		super(SAML_CONSUMER_SERVLET_PATH, 
				server, applicationContext, freemarkerHandler, SamlUnicoreIdPWebUI.class, pkiManagement, 
				executorsService, mainConfig, dispatcherServletFactory, 
				remoteMetadataManagers, downloadManager, 
				logoutProcessorFactory, 
				sloReplyInstaller, msg);
	}

	@Override
	protected Servlet getSamlParseServlet(String endpointURL, String uiUrl)
	{
		return new SamlETDParseServlet(myMetadataManager, 
				endpointURL, uiUrl, new ErrorHandler(aTypeSupport, freemarkerHandler),
				samlProperties.getBooleanValue(CommonIdPProperties.ASSUME_FORCE));
	}
}
