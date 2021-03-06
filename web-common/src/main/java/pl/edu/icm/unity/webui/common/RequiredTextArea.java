/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.v7.ui.TextArea;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * Extension of {@link TextArea} with a two changes: the field is required, wordwrap is true 
 * and the exclamation mark is shown for all empty fields except for the initial rendering. 
 * @author P. Piernik
 *
 */
public class RequiredTextArea extends TextArea
{

	public RequiredTextArea(UnityMessageSource msg)
	{
		super();
		setRequired(true);
		setRequiredError(msg.getMessage("fieldRequired"));
		setWordwrap(true);
	}

	public RequiredTextArea(String caption, String value, UnityMessageSource msg)
	{
		super(caption, value);
		setRequired(true);
		setRequiredError(msg.getMessage("fieldRequired"));
		setWordwrap(true);
	}

	public RequiredTextArea(String caption, UnityMessageSource msg)
	{
		super(caption);
		setRequired(true);
		setRequiredError(msg.getMessage("fieldRequired"));
		setWordwrap(true);
	}
	
	@Override
	protected boolean shouldHideErrors() {
		return false;
	}
}
