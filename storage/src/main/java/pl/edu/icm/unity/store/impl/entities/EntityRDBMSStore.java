/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.entities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.rdbms.BaseBean;
import pl.edu.icm.unity.store.rdbms.GenericRDBMSCRUD;
import pl.edu.icm.unity.types.basic.EntityInformation;


/**
 * RDBMS storage of {@link StoredEntity}
 * @author K. Benedyczak
 */
@Repository(EntityRDBMSStore.BEAN)
public class EntityRDBMSStore extends GenericRDBMSCRUD<EntityInformation, BaseBean> 
					implements EntityDAO
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public EntityRDBMSStore(EntityJsonSerializer jsonSerializer)
	{
		super(EntitiesMapper.class, jsonSerializer, NAME);
	}
	
	@Override
	public long create(EntityInformation obj)
	{
		long ret = super.create(obj);
		obj.setId(ret);
		return ret;
	}
}
