/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.web.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.xml.security.utils.Base64;

import pl.edu.icm.unity.samlidp.FreemarkerHandler;
import pl.edu.icm.unity.samlidp.saml.SAMLProcessingException;
import pl.edu.icm.unity.samlidp.saml.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.samlidp.saml.processor.AuthnResponseProcessor;

import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;
import eu.unicore.samly2.exceptions.SAMLServerException;

/**
 * Creates an appropriate SAML error response. Additionally allows to show a plain error page, 
 * if SAML error can not be returned. 
 * 
 * @author K. Benedyczak
 */
public class ErrorHandler
{
	private FreemarkerHandler freemarker;
	
	public ErrorHandler(FreemarkerHandler freemarker)
	{
		this.freemarker = freemarker;
	}

	/**
	 * Creates a Base64 encoded SAML error response, which can be returned to the requester
	 * @param samlCtx
	 * @param error
	 * @return
	 * @throws SAMLProcessingException
	 */
	private String processError(AuthnResponseProcessor errorResponseProcessor, SAMLServerException error) 
	{
		ResponseDocument responseDoc = errorResponseProcessor.getErrorResponse(error);
		String assertion = responseDoc.xmlText();
		String encodedAssertion = Base64.encode(assertion.getBytes());
		return encodedAssertion;
	}
	
	/**
	 * Create an appropriate error response for the exception, and invokes freemarker view which automatically
	 * returns it to the SAML requester via web browser redirect.
	 * @param samlCtx
	 * @param error
	 * @param response
	 * @throws SAMLProcessingException
	 * @throws IOException
	 */
	public void commitErrorResponse(SAMLAuthnContext samlCtx, SAMLServerException error, 
			HttpServletResponse response) throws SAMLProcessingException, IOException
	{
		String serviceUrl = samlCtx.getRequestDocument().getAuthnRequest().getAssertionConsumerServiceURL();
		if (serviceUrl == null)
			throw new SAMLProcessingException("No return URL in the SAML request. " +
					"Can't return the SAML error response.", error);

		AuthnResponseProcessor errorResponseProcessor = new AuthnResponseProcessor(samlCtx);
		String encodedSamlError = processError(errorResponseProcessor, error);
		
		Map<String, String> data = new HashMap<String, String>();
		data.put("SAMLResponse", encodedSamlError);
		data.put("samlService", serviceUrl);
		data.put("samlError", error.getMessage());
		if (samlCtx.getRelayState() != null)
			data.put("RelayState", samlCtx.getRelayState());
		
		PrintWriter w = response.getWriter();
		freemarker.process("finishSaml.ftl", data, w);
	}
	
	public void showErrorPage(SAMLProcessingException reason, HttpServletResponse response) throws IOException
	{
		PrintWriter w = response.getWriter();
		Map<String, String> data = new HashMap<String, String>();
		data.put("error", reason.getMessage());
		if (reason.getCause() != null)
			data.put("errorCause", reason.getCause().toString());
		freemarker.process("finishError.ftl", data, w);
	}

	public void showHoldOnPage(String request, String relayState, HttpServletResponse response) throws IOException
	{
		PrintWriter w = response.getWriter();
		Map<String, String> data = new HashMap<String, String>();
		data.put("originalRequest", request);
		if (relayState != null)
			data.put("RelayState", relayState);
		freemarker.process("holdonError.ftl", data, w);
	}
}