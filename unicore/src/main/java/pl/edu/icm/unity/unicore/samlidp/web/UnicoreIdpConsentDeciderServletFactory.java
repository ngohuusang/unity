/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.web.filter.IdpConsentDeciderServletFactory;

/**
 * Creates {@link UnicoreIdpConsentDeciderServlet}s.
 * 
 * @author K. Benedyczak
 */
@Component
public class UnicoreIdpConsentDeciderServletFactory implements IdpConsentDeciderServletFactory
{
	protected PreferencesManagement preferencesMan;
	protected IdPEngine idpEngine;
	protected FreemarkerHandler freemarker;
	protected SessionManagement sessionMan;
	private AttributeTypeSupport aTypeSupport;

	@Autowired
	public UnicoreIdpConsentDeciderServletFactory(PreferencesManagement preferencesMan,
			IdPEngine idpEngine, FreemarkerHandler freemarker,
			SessionManagement sessionMan,
			AttributeTypeSupport aTypeSupport)
	{
		this.preferencesMan = preferencesMan;
		this.idpEngine = idpEngine;
		this.freemarker = freemarker;
		this.sessionMan = sessionMan;
		this.aTypeSupport = aTypeSupport;
	}

	@Override
	public UnicoreIdpConsentDeciderServlet getInstance(String uiServletPath)
	{
		return new UnicoreIdpConsentDeciderServlet(aTypeSupport,
				preferencesMan, 
				idpEngine, freemarker, sessionMan, uiServletPath);
	}
}
