/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.membership;

import java.util.List;


/**
 * Access to the GroupMembership.xml operations.
 * @author K. Benedyczak
 */
public interface MembershipMapper
{
	long create(GroupElementBean obj);
	
	void deleteByKey(GroupElementBean param);

	GroupElementBean getByKey(GroupElementBean param);

	List<GroupElementBean> getEntityMembership(long entityId);

	List<GroupElementBean> getMembers(long groupId);
	
	List<GroupElementBean> getAll();
}
