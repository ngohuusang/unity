/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import com.vaadin.v7.data.Validator.InvalidValueException;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.v7.ui.TextField;
import com.vaadin.v7.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Asks user for a registration code.
 *
 * @author Krzysztof Benedyczak
 */
class GetRegistrationCodeDialog extends AbstractDialog
{
	private TextField code;
	private GetRegistrationCodeDialog.Callback callback;

	public GetRegistrationCodeDialog(UnityMessageSource msg, GetRegistrationCodeDialog.Callback callback)
	{
		super(msg, msg.getMessage("GetRegistrationCodeDialog.title"));
		this.callback = callback;
		setSize(65, 40);
	}
	
	@Override
	protected Component getContents() throws Exception
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.addComponent(new Label(msg.getMessage("GetRegistrationCodeDialog.information")));
		FormLayout sub = new FormLayout();
		code = new RequiredTextField(msg.getMessage("GetRegistrationCodeDialog.code"), msg);
		code.setValidationVisible(false);
		code.setColumns(Styles.WIDE_TEXT_FIELD);
		sub.addComponent(code);
		main.addComponent(sub);
		return main;
	}
	
	@Override
	protected void onConfirm()
	{
		code.setValidationVisible(true);
		try
		{
			code.validate();
		} catch (InvalidValueException e)
		{
			return;
		}
		callback.onCodeGiven(code.getValue());
		close();
	}
	
	@Override
	protected void onCancel()
	{
		callback.onCancel();
		super.onCancel();
	}

	public interface Callback
	{
		public void onCodeGiven(String code);
		public void onCancel();
	}
}