/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.v7.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;


/**
 * Extension of {@link TextField} with a one change: the field is required 
 * and the exclamation mark is shown for all empty fields except for the initial rendering.
 * @deprecated use regular text field, whether is required is set in Binder/Validation. 
 *  
 * @author K. Benedyczak
 */
@Deprecated
public class RequiredTextField extends TextField
{
	public RequiredTextField(UnityMessageSource msg)
	{
		super();
		setRequired(true);
		setRequiredError(msg.getMessage("fieldRequired"));
	}

	public RequiredTextField(String caption, String value, UnityMessageSource msg)
	{
		super(caption, value);
		setRequired(true);
		setRequiredError(msg.getMessage("fieldRequired"));
	}

	public RequiredTextField(String caption, UnityMessageSource msg)
	{
		super(caption);
		setRequired(true);
		setRequiredError(msg.getMessage("fieldRequired"));
	}
	
	@Override
	protected boolean shouldHideErrors() {
		return false;
	}
}
