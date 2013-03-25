/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import org.junit.Test;
import static org.junit.Assert.*;

import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;


public class TestAuthorization extends DBIntegrationTestBase
{
	private void setAdminsRole(String role) throws Exception
	{
		EnumAttribute roleAt = new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_LEVEL,
				"/", AttributeVisibility.local, role);
		EntityParam adminEntity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "admin"));
		insecureAttrsMan.setAttribute(adminEntity, roleAt, true);
	}
	
	@Test
	public void test() throws Exception
	{
		setAdminsRole(AuthorizationManagerImpl.CONTENTS_MANAGER_ROLE);
		try
		{
			//tests standard deny
			serverMan.resetDatabase();
			fail("reset db possible for contents man");
		} catch(AuthorizationException e) {}
		
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "user1", true, true);
		Identity added = idsMan.addIdentity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				LocalAuthenticationState.outdated);
		EntityParam entity = new EntityParam(added.getEntityId());
		attrsMan.setAttribute(entity, new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_LEVEL,
				"/", AttributeVisibility.local, AuthorizationManagerImpl.USER_ROLE), false);
		setupUserContext("user1");
		try
		{
			//tests standard deny
			serverMan.resetDatabase();
			fail("reset db possible for user");
		} catch(AuthorizationException e) {}
		try
		{
			//tests standard deny
			groupsMan.addGroup(new Group("/A"));
			fail("addGrp possible for user");
		} catch(AuthorizationException e) {}
		
		//tests self access
		attrsMan.getAttributes(entity, "/", null);
		
		setupUserContext("admin");
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addMemberFromParent("/A", entity);
		attrsMan.removeAttribute(entity, "/", SystemAttributeTypes.AUTHORIZATION_LEVEL);
		
		attrsMan.setAttribute(entity, new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_LEVEL,
				"/A", AttributeVisibility.local, AuthorizationManagerImpl.SYSTEM_MANAGER_ROLE), false);
		setupUserContext("user1");
		try
		{
			//tests standard deny
			serverMan.resetDatabase();
			fail("reset db possible for user");
		} catch(AuthorizationException e) {}
		//tests group authz
		groupsMan.addGroup(new Group("/A/B"));
		//tests searching of the role attribute in the parent group
		groupsMan.removeGroup("/A/B", true);
		
		try
		{
			//tests standard deny
			groupsMan.addGroup(new Group("/B"));
			fail("addGrp possible for no-role");
		} catch(AuthorizationException e) {}
	}
}