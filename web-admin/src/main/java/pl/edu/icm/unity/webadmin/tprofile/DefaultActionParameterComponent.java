/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import com.vaadin.data.Binder;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;

/**
 * Trivial, {@link TextField} based implementation of {@link ActionParameterComponent}. 
 * @author K. Benedyczak
 */
public class DefaultActionParameterComponent extends TextField implements ActionParameterComponent
{
	protected Binder<StringValueBean> binder;

	public DefaultActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg)
	{
		this(desc, msg, false);
	}
	
	public DefaultActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg, boolean required)
	{
		setCaption(desc.getName() + ":");
		setDescription(msg.getMessage(desc.getDescriptionKey()));
		binder = new Binder<>(StringValueBean.class);
		configureBinding(msg, required);
	}
	
	protected void configureBinding(UnityMessageSource msg, boolean required)
	{	
		if (required)
			binder.forField(this).asRequired(msg.getMessage("fieldRequired"))
			.bind("value");
		else 
			binder.forField(this).bind("value");
		binder.setBean(new StringValueBean());	
	}
		
	@Override
	public String getActionValue()
	{
		return binder.getBean().getValue();
	}

	@Override
	public void setActionValue(String value)
	{
		binder.setBean(new StringValueBean(value));
	}

	@Override
	public void addValueChangeCallback(Runnable callback)
	{
		binder.addValueChangeListener((e) -> { callback.run(); });		
	}

	@Override
	public boolean isValid()
	{
		binder.validate();
		return binder.isValid();
	}
}
