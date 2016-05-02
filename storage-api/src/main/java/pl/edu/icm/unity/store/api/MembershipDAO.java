/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.List;

import pl.edu.icm.unity.types.basic.GroupMembership;


/**
 * Group membership DAO
 * @author K. Benedyczak
 */
public interface MembershipDAO
{
	String DAO_ID = "MembershipDAO";
	String NAME = "group membership";
	
	void create(GroupMembership obj);
	
	void deleteByKey(long entityId, String group);

	boolean isMember(long entityId, String group);

	List<GroupMembership> getEntityMembership(long entityId);

	List<GroupMembership> getMembers(String group);
}
