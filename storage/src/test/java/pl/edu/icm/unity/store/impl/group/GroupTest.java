/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.group;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.store.impl.AbstractNamedDAOTest;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic2.Attribute2;
import pl.edu.icm.unity.types.basic2.AttributeStatement2;
import pl.edu.icm.unity.types.basic2.AttributeStatement2.ConflictResolution;
import pl.edu.icm.unity.types.basic2.Group;

public class GroupTest extends AbstractNamedDAOTest<Group>
{
	@Autowired
	private GroupDAO dao;
	
	@Autowired
	private AttributeTypeDAO atDao;

	@Before
	public void createReferenced()
	{
		tx.runInTransaction(() -> {
			atDao.create(new AttributeType("dynAt", "syntax"));
			atDao.create(new AttributeType("at", "syntax"));
			atDao.create(new AttributeType("dynAt2", "syntax"));
			atDao.create(new AttributeType("at2", "syntax"));
		});
	}
	
	
	@Test
	public void childGroupsAreRemovedOnParentRemoval()
	{
		tx.runInTransaction(() -> {
			long key = dao.create(new Group("/A"));
			dao.create(new Group("/A/B"));
			dao.create(new Group("/A/B/C"));
			dao.create(new Group("/A/D"));

			dao.deleteByKey(key);

			assertThat(dao.exists("/A"), is(false));
			assertThat(dao.exists("/A/B"), is(false));
			assertThat(dao.exists("/A/B/C"), is(false));
			assertThat(dao.exists("/A/D"), is(false));
		});
	}
	
	@Test
	public void childGroupNamesAreUpdatedOnParentRename()
	{
		tx.runInTransaction(() -> {
			long key = dao.create(new Group("/A"));
			dao.create(new Group("/A/B"));
			dao.create(new Group("/A/B/C"));
			dao.create(new Group("/A/D"));

			dao.updateByKey(key, new Group("/S"));

			assertThat(dao.exists("/A"), is(false));
			assertThat(dao.exists("/A/B"), is(false));
			assertThat(dao.exists("/A/B/C"), is(false));
			assertThat(dao.exists("/A/D"), is(false));
			
			assertThat(dao.exists("/S"), is(true));
			assertThat(dao.exists("/S/B"), is(true));
			assertThat(dao.exists("/S/B/C"), is(true));
			assertThat(dao.exists("/S/D"), is(true));
		});

	}
	
	@Test
	public void changingNotTheLastNamePartIsForbidden()
	{
		tx.runInTransaction(() -> {
			dao.create(new Group("/A"));
			dao.create(new Group("/A/B"));
			long key = dao.create(new Group("/A/B/C"));

			catchException(dao).updateByKey(key, new Group("/S/B/C"));

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
	
	
	
	@Override
	protected NamedCRUDDAO<Group> getDAO()
	{
		return dao;
	}

	@Override
	protected Group getObject(String name)
	{
		Group ret = new Group("/" + name);
		ret.setDescription(new I18nString("desc"));
		ret.setDisplayedName(new I18nString("dname"));
		ret.setAttributesClasses(Sets.newHashSet("ac1", "ac2"));
		
		Attribute2 fixedAt = new Attribute2("at", "syntax", 
				"/A/" + name, Lists.newArrayList("v1"));
		ret.setAttributeStatements(new AttributeStatement2[] {
			new AttributeStatement2("cnd1", "/A", ConflictResolution.overwrite, 
					fixedAt),
			new AttributeStatement2("cnd2", "/A", ConflictResolution.skip, 
					AttributeVisibility.full, "dynAt", "dynAExpr")
		});
		return ret;
	}

	@Override
	protected void mutateObject(Group ret)
	{
		ret.setDescription(new I18nString("desc2"));
		ret.setDisplayedName(new I18nString("dname2"));
		ret.setAttributesClasses(Sets.newHashSet("ac1"));
		
		Attribute2 fixedAt = new Attribute2("at2", "syntax", 
				 ret.getName(), Lists.newArrayList("v2"));
		ret.setAttributeStatements(new AttributeStatement2[] {
			new AttributeStatement2("cnd3", "/A", ConflictResolution.merge, 
					fixedAt),
			new AttributeStatement2("cnd4", "/A", ConflictResolution.merge, 
					AttributeVisibility.full, "dynAt2", "dynAExpr2")
		});
	}

	@Override
	protected void assertAreEqual(Group obj, Group cmp)
	{
		assertThat(obj.getName(), is(cmp.getName()));
		assertThat(obj.getDescription(), is (cmp.getDescription()));
		assertThat(obj.getDisplayedName(), is (cmp.getDisplayedName()));
		assertThat(obj.getAttributesClasses(), is (cmp.getAttributesClasses()));
		assertArrayEquals(obj.getAttributeStatements(), cmp.getAttributeStatements());
	}
}