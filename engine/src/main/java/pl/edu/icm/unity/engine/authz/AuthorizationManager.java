/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authz;

import java.util.Set;

import pl.edu.icm.unity.types.basic.Group;


/**
 * Authorizes operations on the engine.
 * @author K. Benedyczak
 */
public interface AuthorizationManager
{
	public Set<String> getRoleNames();

	/**
	 * As {@link #checkAuthorization(boolean, Group, AuthzCapability...)} with the first argument
	 * false and the second being the root group.
	 * @param group
	 * @param requiredCapabilities
	 */
	public void checkAuthorization(AuthzCapability... requiredCapabilities);

	/**
	 * As {@link #checkAuthorization(boolean, Group, AuthzCapability...)} with the second argument being the root group
	 * @param selfAccess
	 * @param requiredCapabilities
	 */
	public void checkAuthorization(boolean selfAccess, AuthzCapability... requiredCapabilities);
	
	/**
	 * As {@link #checkAuthorization(boolean, Group, AuthzCapability...)} with the first argument
	 * false.
	 * @param group
	 * @param requiredCapabilities
	 */
	public void checkAuthorization(String group, AuthzCapability... requiredCapabilities);

	/**
	 * Checks the authorization in a specified group. It is checked if the current caller has all the 
	 * requiredCapabilities in the scope of the specified group.
	 * @param selfAccess if this operation is invoked on the the caller itself
	 * @param group
	 * @param requiredCapabilities
	 */
	public void checkAuthorization(boolean selfAccess, String group, AuthzCapability... requiredCapabilities);
	
	/**
	 * Returns true only if the argument is the same entity as the current caller.
	 * @param subject
	 * @return
	 */
	public boolean isSelf(long subject);
}