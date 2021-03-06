/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import pl.edu.icm.unity.store.rdbms.BaseBean;


/**
 * In DB identity representation.
 * @author K. Benedyczak
 */
public class IdentityBean extends BaseBean
{
	private Long entityId;
	private Long typeId;
	private String typeName;
	
	public IdentityBean() 
	{
	}
	
	public Long getEntityId()
	{
		return entityId;
	}
	public void setEntityId(Long entityId)
	{
		this.entityId = entityId;
	}

	public Long getTypeId()
	{
		return typeId;
	}

	public void setTypeId(Long typeId)
	{
		this.typeId = typeId;
	}

	public String getTypeName()
	{
		return typeName;
	}

	public void setTypeName(String typeName)
	{
		this.typeName = typeName;
	}
}
