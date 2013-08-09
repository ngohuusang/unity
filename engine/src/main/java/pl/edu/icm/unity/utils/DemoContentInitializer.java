/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.utils;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.ServerInitializer;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.utils.InitializerCommon;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Populates DB with some demo contents.
 * @author K. Benedyczak
 */
@Component
public class DemoContentInitializer implements ServerInitializer
{
	private static Logger log = Log.getLogger(Log.U_SERVER, DemoContentInitializer.class);
	public static final String NAME = "demoInitializer";
	private GroupsManagement groupsMan;
	private IdentitiesManagement idsMan;
	private AttributesManagement attrMan;
	private InitializerCommon commonInitializer;
	
	@Autowired
	public DemoContentInitializer(@Qualifier("insecure") GroupsManagement groupsMan, 
			@Qualifier("insecure") IdentitiesManagement idsMan,
			@Qualifier("insecure") AttributesManagement attrMan, InitializerCommon commonInitializer)
	{
		this.groupsMan = groupsMan;
		this.idsMan = idsMan;
		this.attrMan = attrMan;
		this.commonInitializer = commonInitializer;
	}
	
	@Override
	public void run()
	{
		try
		{
			commonInitializer.initializeCommonAttributeTypes();
			
			groupsMan.addGroup(new Group("/A"));
			groupsMan.addGroup(new Group("/A/B"));
			groupsMan.addGroup(new Group("/A/B/C"));
			groupsMan.addGroup(new Group("/D"));
			groupsMan.addGroup(new Group("/D/E"));
			groupsMan.addGroup(new Group("/D/G"));
			groupsMan.addGroup(new Group("/D/F"));

			AttributeType userPicture = new AttributeType("picture", new JpegImageAttributeSyntax());
			((JpegImageAttributeSyntax)userPicture.getValueType()).setMaxSize(1400000);
			((JpegImageAttributeSyntax)userPicture.getValueType()).setMaxWidth(900);
			((JpegImageAttributeSyntax)userPicture.getValueType()).setMaxHeight(900);
			userPicture.setMaxElements(1);
			userPicture.setDescription("Picture of the user");
			attrMan.addAttributeType(userPicture);

			AttributeType postalcode = new AttributeType("postalcode", new StringAttributeSyntax());
			postalcode.setMinElements(0);
			postalcode.setMaxElements(Integer.MAX_VALUE);
			postalcode.setDescription("Postal code");
			((StringAttributeSyntax)postalcode.getValueType()).setRegexp("[0-9][0-9]-[0-9][0-9][0-9]");
			((StringAttributeSyntax)postalcode.getValueType()).setMaxLength(6);
			attrMan.addAttributeType(postalcode);
			
			
			
			AttributeType height = new AttributeType("height", new FloatingPointAttributeSyntax());
			height.setMinElements(1);
			height.setDescription("He\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjh");
			attrMan.addAttributeType(height);

			IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "foo", true);
			Identity base = idsMan.addEntity(toAdd, "Password requirement", EntityState.valid, false);

			IdentityParam toAddDn = new IdentityParam(X500Identity.ID, "CN=test foo", true);
			idsMan.addIdentity(toAddDn, new EntityParam(base.getEntityId()), true);

			groupsMan.addMemberFromParent("/A", new EntityParam(base.getEntityId()));

			EnumAttribute a = new EnumAttribute("sys:AuthorizationRole", "/", AttributeVisibility.local, "Regular User");
			attrMan.setAttribute(new EntityParam(base.getEntityId()), a, false);

			StringAttribute orgA = new StringAttribute("o", "/A", AttributeVisibility.full, "Example organization");
			attrMan.setAttribute(new EntityParam(base.getEntityId()), orgA, false);

			StringAttribute emailA = new StringAttribute("email", "/A", AttributeVisibility.full, "some@email.com");
			attrMan.setAttribute(new EntityParam(base.getEntityId()), emailA, false);

			StringAttribute cnA = new StringAttribute("cn", "/A", AttributeVisibility.full, "Hiper user");
			attrMan.setAttribute(new EntityParam(base.getEntityId()), cnA, false);

			idsMan.setEntityCredential(new EntityParam(base.getEntityId()), "Password credential", "a");
		} catch (Exception e)
		{
			log.error("Error loading demo contents", e);
		}
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
