/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Properties;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.basic.MessageTemplate;

public class MessageTemplateLoaderTest
{
	@Test
	public void shouldLoadWithDefaultLocale() throws EngineException
	{
		MessageTemplateManagement man = mock(MessageTemplateManagement.class);
		when(man.listTemplates()).thenReturn(Collections.emptyMap());
		ArgumentCaptor<MessageTemplate> captor = 
				ArgumentCaptor.forClass(MessageTemplate.class);
		MessageTemplateLoader loader = new MessageTemplateLoader(man);
		
		Properties props = new Properties();
		props.setProperty("msg1.subject", "sub");
		props.setProperty("msg1.body", "body");
		loader.initializeMsgTemplates(props);
		
		verify(man).addTemplate(captor.capture());
		
		I18nMessage msg = captor.getValue().getMessage();
		assertThat(msg.getSubject().getDefaultValue(), is("sub"));
		assertThat(msg.getSubject().getMap().size(), is(0));
		assertThat(msg.getBody().getDefaultValue(), is("body"));
		assertThat(msg.getBody().getMap().size(), is(0));
	}

	@Test
	public void shouldLoadWithNonDefaultLocale() throws EngineException
	{
		MessageTemplateManagement man = mock(MessageTemplateManagement.class);
		when(man.listTemplates()).thenReturn(Collections.emptyMap());
		ArgumentCaptor<MessageTemplate> captor = 
				ArgumentCaptor.forClass(MessageTemplate.class);
		MessageTemplateLoader loader = new MessageTemplateLoader(man);
		
		Properties props = new Properties();
		props.setProperty("msg1.subject.en", "sub");
		props.setProperty("msg1.body.en", "body");
		loader.initializeMsgTemplates(props);
		
		verify(man).addTemplate(captor.capture());
		
		I18nMessage msg = captor.getValue().getMessage();
		assertThat(msg.getSubject().getDefaultValue(), is(nullValue()));
		assertThat(msg.getBody().getDefaultValue(), is(nullValue()));
		assertThat(msg.getSubject().getValueRaw("en"), is("sub"));
		assertThat(msg.getBody().getValueRaw("en"), is("body"));
		assertThat(msg.getSubject().getMap().size(), is(1));
		assertThat(msg.getBody().getMap().size(), is(1));
	}

	@Test
	public void shouldLoadTwoLocalesAndDef() throws EngineException
	{
		MessageTemplateManagement man = mock(MessageTemplateManagement.class);
		when(man.listTemplates()).thenReturn(Collections.emptyMap());
		ArgumentCaptor<MessageTemplate> captor = 
				ArgumentCaptor.forClass(MessageTemplate.class);
		MessageTemplateLoader loader = new MessageTemplateLoader(man);
		
		Properties props = new Properties();
		props.setProperty("msg1.subject", "sub");
		props.setProperty("msg1.body.pl", "body-pl");
		props.setProperty("msg1.body.en", "body-en");
		props.setProperty("msg1.body", "body");
		loader.initializeMsgTemplates(props);
		
		verify(man).addTemplate(captor.capture());
		
		I18nMessage msg = captor.getValue().getMessage();
		assertThat(msg.getSubject().getDefaultValue(), is("sub"));
		assertThat(msg.getBody().getDefaultValue(), is("body"));
		assertThat(msg.getBody().getValueRaw("pl"), is("body-pl"));
		assertThat(msg.getBody().getValueRaw("en"), is("body-en"));
		assertThat(msg.getSubject().getMap().size(), is(0));
		assertThat(msg.getBody().getMap().size(), is(2));
	}

	@Test
	public void shouldLoadOverrwriteInlineBodyWithFileBody() throws EngineException
	{
		MessageTemplateManagement man = mock(MessageTemplateManagement.class);
		when(man.listTemplates()).thenReturn(Collections.emptyMap());
		ArgumentCaptor<MessageTemplate> captor = 
				ArgumentCaptor.forClass(MessageTemplate.class);
		MessageTemplateLoader loader = new MessageTemplateLoader(man);
		
		Properties props = new Properties();
		props.setProperty("msg1.subject", "sub");
		props.setProperty("msg1.body.en", "body-inline");
		props.setProperty("msg1.bodyFile.en", "src/test/resources/templateBody.txt");
		loader.initializeMsgTemplates(props);
		
		verify(man).addTemplate(captor.capture());
		
		I18nMessage msg = captor.getValue().getMessage();
		assertThat(msg.getSubject().getDefaultValue(), is("sub"));
		assertThat(msg.getBody().getDefaultValue(), is(nullValue()));
		assertThat(msg.getBody().getValueRaw("en"), is("FILE"));
		assertThat(msg.getSubject().getMap().size(), is(0));
		assertThat(msg.getBody().getMap().toString(), 
				msg.getBody().getMap().size(), is(1));

	}
}
