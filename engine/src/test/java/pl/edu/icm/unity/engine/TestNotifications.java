/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.notification.NotificationStatus;
import pl.edu.icm.unity.engine.notifications.EmailFacility;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.PasswordResetTemplateDef;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.NotificationChannel;

/**
 * Tests the core notifications mechanism and the email facility.
 * @author K. Benedyczak
 */
public class TestNotifications extends DBIntegrationTestBase
{
	@Autowired
	private NotificationProducer notProducer;
	
	
	//@Test
	public void testEmailNotification() throws Exception
	{
		String emailCfg = "mail.from=...\n" +
				"mail.smtp.host=...\n" +
				"mail.smtp.starttls.enable=true\n" +
				"mail.smtp.port=587\n" + //or 25
				"mailx.smtp.auth.username=...\n" +
				"mailx.smtp.auth.password=...\n" +
				"mail.smtp.auth=true\n" +
				"mail.smtp.timeoutSocket=15000\n" +
				"mail.smtp.connectiontimeout=15000\n" +
				"mailx.smtp.trustAll=true";
		String destinationAddress = "...";
		
		notMan.addNotificationChannel(new NotificationChannel("ch1", "", emailCfg, EmailFacility.NAME));
		EntityParam admin = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "admin"));
		
		aTypeMan.addAttributeType(new AttributeType("email", VerifiableEmailAttributeSyntax.ID));

		Map<String, String> params = new HashMap<String, String>();
		params.put(PasswordResetTemplateDef.VAR_CODE, "AAAA");
		params.put(PasswordResetTemplateDef.VAR_USER, "some user");
		
		try
		{
			notProducer.sendNotification(admin, "ch1", PasswordResetTemplateDef.NAME, params, null, null);
			fail("Managed to send email for an entity without email attribute");
		} catch(IllegalIdentityValueException e){}

		Attribute emailA = StringAttribute.of("email", 
				"/", destinationAddress);
		attrsMan.setAttribute(admin, emailA, false);
		Future<NotificationStatus> statusFuture = notProducer.sendNotification(admin, "ch1", 
				PasswordResetTemplateDef.NAME, params, null, null);
		NotificationStatus status = statusFuture.get();
		if (!status.isSuccessful())
			status.getProblem().printStackTrace();
		assertTrue(status.isSuccessful());
	}
	
	
	@Test
	public void testManagement() throws Exception
	{
		String emailCfg = "";
		String emailCfg2 = "a=b";
		assertEquals(3, notMan.getNotificationFacilities().size());
		assertTrue(notMan.getNotificationFacilities().contains(EmailFacility.NAME));
		assertEquals(0, notMan.getNotificationChannels().size());
		notMan.addNotificationChannel(new NotificationChannel("ch1", "", emailCfg, EmailFacility.NAME));
		Map<String, NotificationChannel> channels = notMan.getNotificationChannels();
		assertEquals(1, channels.size());
		assertTrue(channels.containsKey("ch1"));
		assertEquals(emailCfg, channels.get("ch1").getConfiguration());
		
		try
		{
			notMan.updateNotificationChannel("wrong", emailCfg2);
			fail("Managed to update not existing channel");
		} catch (IllegalArgumentException e)
		{
		}
		notMan.updateNotificationChannel("ch1", emailCfg2);
		channels = notMan.getNotificationChannels();
		assertEquals(1, channels.size());
		assertTrue(channels.containsKey("ch1"));
		assertEquals(emailCfg2, channels.get("ch1").getConfiguration());

		try
		{
			notMan.removeNotificationChannel("wrong");
			fail("Managed to remove not existing channel");
		} catch (IllegalArgumentException e)
		{
		}
		notMan.removeNotificationChannel("ch1");
		assertEquals(0, notMan.getNotificationChannels().size());

		notMan.addNotificationChannel(new NotificationChannel("ch1", "", emailCfg, EmailFacility.NAME));
		channels = notMan.getNotificationChannels();
		assertEquals(1, channels.size());
		assertTrue(channels.containsKey("ch1"));
		assertEquals(emailCfg, channels.get("ch1").getConfiguration());
	}
}
