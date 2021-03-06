/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.msgtemplate;

import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.MessageTemplate;

/**
 * Simple component allowing to view message template (name, subject, body).
 * @author P. Piernik
 *
 */
public class SimpleMessageTemplateViewer extends MessageTemplateViewerBase
{
	private MessageTemplateManagement msgTempMan;
	private Label notSet;
	
	public SimpleMessageTemplateViewer(String caption, UnityMessageSource msg,
			MessageTemplateManagement msgTempMan)
	{
		super(msg);
		this.msgTempMan = msgTempMan;
		setCaption(caption);
	}
	
	@Override
	protected void initUI()
	{	
		notSet = new Label();
		notSet.setVisible(false);
		addComponents(notSet);		
	}
	
	public void setInput(String template)
	{
		clearContent();
		notSet.setValue("");
		if (template == null)
		{	
			main.setVisible(false);
			notSet.setValue(msg.getMessage("MessageTemplateViewer.notSet"));
			notSet.setVisible(true);
			return;
		}
		notSet.setVisible(false);	
		try
		{
			MessageTemplate templateC = msgTempMan.getPreprocessedTemplate(template);
			setInput(templateC);
			
		} catch (EngineException e)
		{
			notSet.setValue(msg.getMessage("MessageTemplateViewer.errorMissingTemplate", template));
			notSet.setVisible(true);
			main.setVisible(false);
		}
	}
}
