/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.group;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;

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
			dao.create(new Group("/AAA"));

			dao.updateByKey(key, new Group("/S"));

			assertThat(dao.exists("/A"), is(false));
			assertThat(dao.exists("/A/B"), is(false));
			assertThat(dao.exists("/A/B/C"), is(false));
			assertThat(dao.exists("/A/D"), is(false));
			
			assertThat(dao.exists("/S"), is(true));
			assertThat(dao.exists("/S/B"), is(true));
			assertThat(dao.exists("/S/B/C"), is(true));
			assertThat(dao.exists("/S/D"), is(true));
			assertThat(dao.exists("/AAA"), is(true));
		});
	}
	
	@Test
	public void childGroupNamesAreUpdatedByNameOnParentRename()
	{
		tx.runInTransaction(() -> {
			dao.create(new Group("/A"));
			dao.create(new Group("/A/B"));
			dao.create(new Group("/A/B/C"));
			dao.create(new Group("/A/D"));
			dao.create(new Group("/AAA"));

			dao.updateByName("/A", new Group("/S"));

			assertThat(dao.exists("/A"), is(false));
			assertThat(dao.exists("/A/B"), is(false));
			assertThat(dao.exists("/A/B/C"), is(false));
			assertThat(dao.exists("/A/D"), is(false));
			
			assertThat(dao.exists("/S"), is(true));
			assertThat(dao.exists("/S/B"), is(true));
			assertThat(dao.exists("/S/B/C"), is(true));
			assertThat(dao.exists("/S/D"), is(true));
			assertThat(dao.exists("/AAA"), is(true));
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
	
	/**
	 * Overridden as we always have the '/' extra added.
	 */
	@Test
	@Override
	public void importExportIsIdempotent()
	{
		Group obj = getObject("name1");
		ByteArrayOutputStream os = tx.runInTransactionRet(() -> {
			dao.create(obj);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try
			{
				ie.store(baos);
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Export failed " + e);
			}
			return baos;
		});
		
		tx.runInTransaction(() -> {
			dbCleaner.reset();
		});

		tx.runInTransaction(() -> {
			String dump = new String(os.toByteArray(), StandardCharsets.UTF_8);
			ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
			try
			{
				ie.load(is);
			} catch (Exception e)
			{
				e.printStackTrace();
				
				fail("Import failed " + e + "\nDump:\n" + dump);
			}

			List<Group> all = dao.getAll();

			assertThat(all.size(), is(2));
			Group g = all.get(0);
			if (g.getName().equals("/"))
				g = all.get(1);
			assertThat(g, is(obj));
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
		
		Attribute fixedAt = new Attribute("at", "syntax", 
				"/A/" + name, Lists.newArrayList("v1"));
		ret.setAttributeStatements(new AttributeStatement[] {
			new AttributeStatement("cnd1", "/A", ConflictResolution.overwrite, 
					fixedAt),
			new AttributeStatement("cnd2", "/A", ConflictResolution.skip, 
					"dynAt", "dynAExpr")
		});
		return ret;
	}

	@Override
	protected Group mutateObject(Group ret)
	{
		ret.setDescription(new I18nString("desc2"));
		ret.setDisplayedName(new I18nString("dname2"));
		ret.setAttributesClasses(Sets.newHashSet("ac1"));
		
		Attribute fixedAt = new Attribute("at2", "syntax", 
				 ret.getName(), Lists.newArrayList("v2"));
		ret.setAttributeStatements(new AttributeStatement[] {
			new AttributeStatement("cnd3", "/A", ConflictResolution.merge, 
					fixedAt),
			new AttributeStatement("cnd4", "/A", ConflictResolution.merge, 
					"dynAt2", "dynAExpr2")
		});
		return ret;
	}
}
