/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.utils.EmailUtils;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

/**
 * Verifiable email attribute value syntax.
 * @author P. Piernik
 */
public class VerifiableEmailAttributeSyntax implements AttributeValueSyntax<VerifiableEmail> 
{
	public static final String ID = "verifiableEmail";
	
	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public boolean areEqual(VerifiableEmail value, Object another)
	{
		return value.equals(another);
	}

	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	@Override
	public void validate(VerifiableEmail value) throws IllegalAttributeValueException
	{
		if (value == null)
			throw new IllegalAttributeValueException("null value is illegal");
		String error = EmailUtils.validate(value.getValue());
		if (error != null)
			throw new IllegalAttributeValueException(value.getValue() + ": " + error);
	}

	@Override
	public JsonNode getSerializedConfiguration()
	{
		return Constants.MAPPER.createObjectNode();
	}

	@Override
	public void setSerializedConfiguration(JsonNode json)
	{
	      //OK
	}

	@Override
	public boolean isVerifiable()
	{
		return true;
	}

	@Override
	public VerifiableEmail convertFromString(String stringRepresentation)
	{
		return new VerifiableEmail(JsonUtil.parse(stringRepresentation));
	}

	@Override
	public String convertToString(VerifiableEmail value)
	{
		return JsonUtil.serialize(value.toJson());
	}

	@Override
	public String serializeSimple(VerifiableEmail value)
	{
		return value.getValue();
	}

	@Override
	public VerifiableEmail deserializeSimple(String value) throws IllegalAttributeValueException
	{
		VerifiableEmail ret = EmailUtils.convertFromString(value);
		validate(ret);
		return ret;
	}
	
	
	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<VerifiableEmail>
	{
		public Factory()
		{
			super(VerifiableEmailAttributeSyntax.ID, VerifiableEmailAttributeSyntax::new);
		}
	}
}
