/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.saml.processor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlAnySimpleType;
import org.apache.xmlbeans.XmlObject;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.Assertion;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.proto.AssertionResponse;
import pl.edu.icm.unity.samlidp.saml.SAMLProcessingException;
import pl.edu.icm.unity.samlidp.saml.ctx.SAMLAttributeQueryContext;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import xmlbeans.org.oasis.saml2.assertion.AttributeType;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.assertion.SubjectType;
import xmlbeans.org.oasis.saml2.protocol.AttributeQueryDocument;
import xmlbeans.org.oasis.saml2.protocol.AttributeQueryType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Extends {@link StatusResponseProcessor} to produce SAML Response documents, 
 * which are returned in the Attribute Query exchange of the Assertion Query protocol.
 * @author K. Benedyczak
 */
public class AttributeQueryResponseProcessor extends BaseResponseProcessor<AttributeQueryDocument, AttributeQueryType>
{
	public AttributeQueryResponseProcessor(SAMLAttributeQueryContext context)
	{
		this(context, Calendar.getInstance());
	}
	
	public AttributeQueryResponseProcessor(SAMLAttributeQueryContext context, Calendar authnTime)
	{
		super(context, authnTime);
	}

	public IdentityTaV getSubjectsIdentity()
	{
		NameIDType subject = context.getRequest().getSubject().getNameID();
		String nFormat = subject.getFormat();
		String nContents = subject.getStringValue();
		if (nFormat == null || nFormat.equals(SAMLConstants.NFORMAT_ENTITY))
		{
			return new IdentityTaV(PersistentIdentity.ID, nContents);
		}
		if (nFormat.equals(SAMLConstants.NFORMAT_DN))
		{
			return new IdentityTaV(X500Identity.ID, nContents);
		}
		throw new IllegalStateException("Unsupported subject format, which passed request validation - " +
				"it's a bug. " + nFormat + " :: " + nContents);
	}
	
	public ResponseDocument processAtributeRequest(Collection<Attribute<?>> attributes) 
			throws SAMLRequesterException, SAMLProcessingException
	{
		AssertionResponse resp = getOKResponseDocument();
		if (attributes != null)
		{
			SubjectType subjectWithConf = setSenderVouchesSubjectConfirmation(
					context.getRequest().getSubject());
			Assertion assertion = createAttributeAssertion(subjectWithConf, attributes);
			if (assertion != null)
				resp.addAssertion(assertion);
		}
		return resp.getXMLBeanDoc();
	}
	
	/**
	 * Filters attributes, so only the requested attributes are left.
	 * @param converted
	 */
	@Override
	protected void filterRequested(List<AttributeType> converted)
	{
		AttributeQueryType query = context.getRequest();
		AttributeType[] requested = query.getAttributeArray();
		//nothing requested - return all
		if (requested == null || requested.length == 0)
			return;
		Map<String, AttributeType> requestedMap = new HashMap<String, AttributeType>(requested.length);
		for (AttributeType r: requested)
			requestedMap.put(r.getName(), r);
		
		for (int i=0; i<converted.size(); i++)
		{
			AttributeType a = converted.get(i);
			AttributeType reqa = requestedMap.get(a.getName());
			//not among requested attributes - skip it
			if (reqa == null)
			{
				converted.remove(i--);
				continue;
			}
			
			//among requested without a value - leave it with all values
			if (reqa.sizeOfAttributeValueArray() == 0)
				continue;
			
			//among requested with given values - filter them
			XmlObject[] aVals = a.getAttributeValueArray();
			if (aVals == null || aVals.length == 0) //no values - no problem
				continue;
			XmlObject[] reqVals = reqa.getAttributeValueArray();
			List<Integer> toBeRemoved = new ArrayList<Integer>();
			for (int j=aVals.length-1; j>=0; j--)
			{
				XmlObject aVal = aVals[j];
				if (!isAmongValues(aVal, reqVals))
					toBeRemoved.add(j);
			}
			for (int remove: toBeRemoved)
				a.removeAttributeValue(remove);
		}
	}
	
	//FIXME - this is not supporting any profile-defined attribute equality
	protected boolean isAmongValues(XmlObject tested, XmlObject[] permitted)
	{
		String testedVal;
		if (tested instanceof XmlAnySimpleType)
			testedVal = ((XmlAnySimpleType)tested).getStringValue();
		else
			return false; //unsupported...
		for (XmlObject p: permitted)
		{
			if (p instanceof XmlAnySimpleType)
				if (testedVal.equals(((XmlAnySimpleType) p).getStringValue()))
					return true;
		}
		return false;
	}
}