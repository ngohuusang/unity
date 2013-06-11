/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.attrstmnt.CopyParentAttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.CopySubgroupAttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.EverybodyStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.HasParentAttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.HasSubgroupAttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.MemberOfStatement;

public class TestAttributeStatements extends DBIntegrationTestBase
{
	private final int systemAttributes = 2;
	private EntityParam entity;
	private Group groupA;
	private Group groupAB;
	private Group groupAD;
	private Group groupAZ;
	private Group groupABC;
	
	@Test
	public void testSimple() throws Exception
	{
		setupStateForConditions();
		
		// test if simple attributes are properly returned

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 0, 0,  0, 0); //a2
	}
	
	@Test
	public void testEverybody() throws Exception
	{
		setupStateForConditions();
		
		// test with one statement added to /A for everybody
		groupA.setAttributeStatements(new AttributeStatement[] {new EverybodyStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				ConflictResolution.skip)});
		groupsMan.updateGroup("/A", groupA);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 1, 0, 0,  0, 0); //a2
	}
	
	
	@Test
	public void testMemberOf() throws Exception
	{
		setupStateForConditions();
		// statement at /A/B for all members of /A/B/C (entity is member)
		groupAB.setAttributeStatements(new AttributeStatement[] {new MemberOfStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "va1"), 
				"/A/B/C", 
				ConflictResolution.skip)});
		groupsMan.updateGroup("/A/B", groupAB);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 1, 0,  0, 0); //a2

		// condition added to /A/B/C for all members of /A/Z (entity is not a member, should be no change)		
		groupABC.setAttributeStatements(new AttributeStatement[] {new MemberOfStatement(
				new StringAttribute("a2", "/A/B/C", AttributeVisibility.local, "va1"), 
				"/A/Z",
				ConflictResolution.skip)});
		groupsMan.updateGroup("/A/B/C", groupABC);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 1, 0,  0, 0); //a2
	}

	@Test
	public void testCopyParentAttr() throws Exception
	{
		setupStateForConditions();
	
		// added to /A/B/C for all having an "a1" attribute in parent. 
		// Entity has this attribute as regular attribute there.
		AttributeStatement statement1 = new CopyParentAttributeStatement(
				new StringAttribute("a1", "/A/B", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupABC.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A/B/C", groupABC);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 1,  0, 0,  //a1
				0, 0, 0, 0,  0, 0); //a2
	}

	
	@Test
	public void testParentAttr() throws Exception
	{
		setupStateForConditions();
	
		// added to /A/B for all having an "a1" attribute in parent. 
		// Entity has this attribute as regular attribute there.
		AttributeStatement statement1 = new HasParentAttributeStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "va1"), 
				new StringAttribute("a1", "/A", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A/B", groupAB);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 1, 0,  0, 0); //a2
	}
	
	@Test
	public void testParentAttr2() throws Exception
	{
		setupStateForConditions();
	
		// added to /A/B for all having an "a2" attribute in parent. 
		// Entity doesn't have this attribute there
		AttributeStatement statement1 = new HasParentAttributeStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "va1"), 
				new StringAttribute("a2", "/A", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A/B", groupAB);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 0, 0,  0, 0); //a2

		// now a2 is added in /A using a statement
		groupA.setAttributeStatements(new AttributeStatement[] {new EverybodyStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				ConflictResolution.skip)});
		groupsMan.updateGroup("/A", groupA);
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 1, 1, 0,  0, 0); //a2
	}
	
	@Test
	public void testParentAttr3() throws Exception
	{
		setupStateForConditions();
	
		// added to /A/B for all having an "a2" attribute in parent. 
		AttributeStatement statement1 = new HasParentAttributeStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "va1"), 
				new StringAttribute("a2", "/A", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A/B", groupAB);

		// added to /A for all having an "a2" attribute in parent. 
		AttributeStatement statement2 = new HasParentAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a2", "/", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement2});
		groupsMan.updateGroup("/A", groupA);
		
		// added to / for everybody
		Group root = new Group("/");
		AttributeStatement statement3 = new EverybodyStatement(
				new StringAttribute("a2", "/", AttributeVisibility.local, "va1"), 
				ConflictResolution.skip);
		root.setAttributeStatements(new AttributeStatement[] {statement3});
		groupsMan.updateGroup("/", root);
		
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				1, 1, 1, 0,  0, 0); //a2
	}	
	
	@Test
	public void testCopySubgroupAttr() throws Exception
	{
		setupStateForConditions();

		// added to / for all having an "a1" attribute in /A. 
		// Entity has this attribute as regular attribute there.
		AttributeStatement statement1 = new CopySubgroupAttributeStatement(
				new StringAttribute("a1", "/A", AttributeVisibility.local), 
				ConflictResolution.skip);
		Group groupR = groupsMan.getContents("/", GroupContents.METADATA).getGroup();
		groupR.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/", groupR);

		//              /  A  AB ABC AD AZ
		testCorrectness(1, 1, 1, 0,  0, 0,  //a1
				0, 0, 0, 0,  0, 0); //a2
	}
	
	@Test
	public void testSubgroupAttr() throws Exception
	{
		setupStateForConditions();
	
		// added to /A for all having an "a1" attribute in /A/B. 
		// Entity has this attribute as regular attribute there.
		AttributeStatement statement1 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a1", "/A/B", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A", groupA);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 1, 0, 0,  0, 0); //a2
	}
	
	@Test
	public void testSubgroupAttr2() throws Exception
	{
		setupStateForConditions();
	
		// added to /A for all having an "a2" attribute in /A/B. 
		// Entity doesn't have this attribute there
		AttributeStatement statement1 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a2", "/A/B", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A", groupA);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 0, 0,  0, 0); //a2

		// now a2 is added in /A/B using a statement
		groupAB.setAttributeStatements(new AttributeStatement[] {new EverybodyStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "va1"), 
				ConflictResolution.skip)});
		groupsMan.updateGroup("/A/B", groupAB);
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 1, 1, 0,  0, 0); //a2
	}
	
	@Test
	public void testSubgroupAttr3() throws Exception
	{
		setupStateForConditions();
	
		// added to /A for all having an "a2" attribute in /A/B. 
		AttributeStatement statement1 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a2", "/A/B", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A", groupA);

		// added to /A/B for all having an "a2" attribute in /A/B/C. 
		AttributeStatement statement2 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "va1"), 
				new StringAttribute("a2", "/A/B/C", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement2});
		groupsMan.updateGroup("/A/B", groupAB);
		
		// added to /A/B/B for everybody
		AttributeStatement statement3 = new EverybodyStatement(
				new StringAttribute("a2", "/A/B/C", AttributeVisibility.local, "va1"), 
				ConflictResolution.skip);
		groupABC.setAttributeStatements(new AttributeStatement[] {statement3});
		groupsMan.updateGroup("/A/B/C", groupABC);
		
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 1, 1, 1,  0, 0); //a2
	}	

	
	@Test
	public void testSubgroupAttrWithValue() throws Exception
	{
		setupStateForConditions();
	
		// added to /A for all having an "a1" with value "foo" attribute in /A/B. 
		// Entity has this attribute as regular attribute there but with other value.
		AttributeStatement statement1 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a1", "/A/B", AttributeVisibility.local, "foo"), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A", groupA);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 0, 0,  0, 0); //a2

		//again using the existing value
		AttributeStatement statement2 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a1", "/A/B", AttributeVisibility.local, "va1"), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement2});
		groupsMan.updateGroup("/A", groupA);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 1, 0, 0,  0, 0); //a2
	}
	
	@Test
	public void testParentgroupAttrWithValue() throws Exception
	{
		setupStateForConditions();
	
		// added to /A/B for all having an "a1" with value "foo" attribute in /A. 
		// Entity has this attribute as regular attribute there but with other value.
		AttributeStatement statement1 = new HasParentAttributeStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "va1"), 
				new StringAttribute("a1", "/A", AttributeVisibility.local, "foo"), 
				ConflictResolution.skip);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A/B", groupAB);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 0, 0,  0, 0); //a2

		//again using the existing value
		AttributeStatement statement2 = new HasParentAttributeStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "va1"), 
				new StringAttribute("a1", "/A", AttributeVisibility.local, "va1"), 
				ConflictResolution.skip);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement2});
		groupsMan.updateGroup("/A/B", groupAB);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 1, 0,  0, 0); //a2
	}
	
	@Test
	public void testConflictResolution() throws Exception
	{
		setupStateForConditions();
	
		// skip
		AttributeStatement statement1 = new EverybodyStatement(
				new StringAttribute("a1", "/A/B", AttributeVisibility.local, "updated"), 
				ConflictResolution.skip);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A/B", groupAB);
		Collection<AttributeExt<?>> aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a1", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("va1", aRet.iterator().next().getValues().get(0));

		// skip and then overwrite
		AttributeStatement statement2 = new EverybodyStatement(
				new StringAttribute("a1", "/A/B", AttributeVisibility.local, "updated2"), 
				ConflictResolution.overwrite);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1, statement2});
		groupsMan.updateGroup("/A/B", groupAB);
		
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a1", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("updated2", aRet.iterator().next().getValues().get(0));
		
		// skip, overwrite, merge which should skip
		AttributeStatement statement3 = new EverybodyStatement(
				new StringAttribute("a1", "/A/B", AttributeVisibility.local, "merge"), 
				ConflictResolution.merge);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1, statement2, statement3});
		groupsMan.updateGroup("/A/B", groupAB);
		
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a1", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("updated2", aRet.iterator().next().getValues().get(0));


		//add two rules to test merge working
		AttributeStatement statement4 = new EverybodyStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "merge1"), 
				ConflictResolution.skip);
		AttributeStatement statement5 = new EverybodyStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "merge2"), 
				ConflictResolution.merge);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1, statement2, statement3,
				statement4, statement5});
		groupsMan.updateGroup("/A/B", groupAB);

		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a2", false);
		assertEquals(1, aRet.size());
		assertEquals(2, aRet.iterator().next().getValues().size());
		assertEquals("merge1", aRet.iterator().next().getValues().get(0));
		assertEquals("merge2", aRet.iterator().next().getValues().get(1));
	}
	
	@Test
	public void testCleanup() throws Exception
	{
		setupStateForConditions();
		
		AttributeStatement statement1 = new EverybodyStatement(
				new StringAttribute("a1", "/A", AttributeVisibility.local, "updated"), 
				ConflictResolution.overwrite);
		AttributeStatement statement3 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "foo"), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement1, statement3});
		groupsMan.updateGroup("/A", groupA);
		
		assertEquals(0, statementsCleaner.updateGroups());
		
		GroupContents c = groupsMan.getContents("/A", GroupContents.METADATA);
		assertEquals(2, c.getGroup().getAttributeStatements().length);
		
		attrsMan.removeAttributeType("a1", true);
		c = groupsMan.getContents("/A", GroupContents.METADATA);
		assertEquals(1, c.getGroup().getAttributeStatements().length);
		
		assertEquals(1, statementsCleaner.updateGroups());
		
		groupsMan.removeGroup("/A/B", true);
		c = groupsMan.getContents("/A", GroupContents.METADATA);
		assertEquals(0, c.getGroup().getAttributeStatements().length);

		assertEquals(1, statementsCleaner.updateGroups());
		
		c = groupsMan.getContents("/A", GroupContents.METADATA);
		assertEquals(0, c.getGroup().getAttributeStatements().length);
	}
	
	@Test
	public void testInvalidArgs() throws Exception
	{
		setupStateForConditions();
	
		AttributeStatement statement1 = new EverybodyStatement(
				new StringAttribute("a1", "/A/D", AttributeVisibility.local, "updated"), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement1});
		try
		{
			groupsMan.updateGroup("/A", groupA);
			fail("Managed to update group with wrong attribute");
		} catch (IllegalAttributeValueException e) {}
		
		AttributeStatement statement2 = new HasParentAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a1", "/A/D", AttributeVisibility.local, "foo"), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement2});
		try
		{
			groupsMan.updateGroup("/A", groupA);
			fail("Managed to update group with wrong attribute statement 1");
		} catch (IllegalAttributeValueException e) {}
		
		AttributeStatement statement3 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a1", "/A", AttributeVisibility.local, "foo"), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement3});
		try
		{
			groupsMan.updateGroup("/A", groupA);
			fail("Managed to update group with wrong attribute statement 2");
		} catch (IllegalAttributeValueException e) {}
	}	
	
	
	private void setupStateForConditions() throws Exception
	{
		setupMockAuthn();
		
		AttributeType at = createSimpleAT("a1");
		attrsMan.addAttributeType(at);
		AttributeType at1 = createSimpleAT("a2");
		at1.setMaxElements(Integer.MAX_VALUE);
		attrsMan.addAttributeType(at1);
		
		groupA = new Group("/A");
		groupsMan.addGroup(groupA);
		
		groupAB = new Group("/A/B");
		groupsMan.addGroup(groupAB);

		groupAD = new Group("/A/D");
		groupsMan.addGroup(groupAD);

		groupAZ = new Group("/A/Z");
		groupsMan.addGroup(groupAZ);
		
		groupABC = new Group("/A/B/C");
		groupsMan.addGroup(groupABC);
		
		Identity id = idsMan.addIdentity(new IdentityParam(X500Identity.ID, "cn=golbi", true, true), "crMock", 
				LocalAuthenticationState.disabled, false);
		entity = new EntityParam(id);
		groupsMan.addMemberFromParent("/A", entity);
		groupsMan.addMemberFromParent("/A/B", entity);
		groupsMan.addMemberFromParent("/A/D", entity);
		groupsMan.addMemberFromParent("/A/B/C", entity);
		
		attrsMan.setAttribute(entity, new StringAttribute("a1", "/A", AttributeVisibility.local, "va1"), false);
		attrsMan.setAttribute(entity, new StringAttribute("a1", "/A/B", AttributeVisibility.local, "va1"), false);
	}
	
	private AttributeType createSimpleAT(String name)
	{
		AttributeType at = new AttributeType();
		at.setValueType(new StringAttributeSyntax());
		at.setDescription("desc");
		at.setFlags(0);
		at.setMaxElements(5);
		at.setMinElements(1);
		at.setName(name);
		at.setSelfModificable(true);
		at.setVisibility(AttributeVisibility.local);
		return at;
	}
	
	private void testCorrectness(int a1InRoot, int a1InA, int a1InAB, int a1InABC, int a1InAD, int a1InAZ,
			int a2InRoot, int a2InA, int a2InAB, int a2InABC, int a2InAD, int a2InAZ) throws Exception
	{
		Collection<AttributeExt<?>> aRet = attrsMan.getAllAttributes(entity, true, "/", "a1", false);
		assertEquals(aRet.toString(), a1InRoot, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/", "a2", false);
		assertEquals(aRet.toString(), a2InRoot, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/", null, false);
		assertEquals(aRet.toString(), a2InRoot+a1InRoot+systemAttributes, aRet.size());
		
		aRet = attrsMan.getAllAttributes(entity, true, "/A", "a1", false);
		assertEquals(aRet.toString(), a1InA, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A", "a2", false);
		assertEquals(aRet.toString(), a2InA, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A", null, false);
		assertEquals(aRet.toString(), a1InA+a2InA, aRet.size());
		
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a1", false);
		assertEquals(aRet.toString(), a1InAB, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a2", false);
		assertEquals(aRet.toString(), a2InAB, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", null, false);
		assertEquals(aRet.toString(), a1InAB+a2InAB, aRet.size());

		aRet = attrsMan.getAllAttributes(entity, true, "/A/B/C", "a1", false);
		assertEquals(aRet.toString(), a1InABC, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B/C", "a2", false);
		assertEquals(aRet.toString(), a2InABC, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B/C", null, false);
		assertEquals(aRet.toString(), a1InABC+a2InABC, aRet.size());

		aRet = attrsMan.getAllAttributes(entity, true, "/A/D", "a1", false);
		assertEquals(aRet.toString(), a1InAD, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/D", "a2", false);
		assertEquals(aRet.toString(), a2InAD, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/D", null, false);
		assertEquals(aRet.toString(), a1InAD+a2InAD, aRet.size());
		
		aRet = attrsMan.getAllAttributes(entity, true, "/A/Z", "a1", false);
		assertEquals(aRet.toString(), a1InAZ, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/Z", "a2", false);
		assertEquals(aRet.toString(), a2InAZ, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/Z", null, false);
		assertEquals(aRet.toString(), a1InAZ+a2InAZ, aRet.size());
		
		aRet = attrsMan.getAllAttributes(entity, true, null, null, false);
		assertEquals(aRet.toString(), a1InRoot+a1InA+a1InAB+a1InABC+a1InAD+a1InAZ+
				a2InRoot+a2InA+a2InAB+a2InABC+a2InAD+a2InAZ+systemAttributes, aRet.size());

		aRet = attrsMan.getAllAttributes(entity, true, null, "a2", false);
		assertEquals(aRet.toString(), a2InRoot+a2InA+a2InAB+a2InABC+a2InAD+a2InAZ, aRet.size());
		
		aRet = attrsMan.getAllAttributes(entity, true, null, "a1", false);
		assertEquals(aRet.toString(), a1InRoot+a1InA+a1InAB+a1InABC+a1InAD+a1InAZ, aRet.size());
	}
}



















