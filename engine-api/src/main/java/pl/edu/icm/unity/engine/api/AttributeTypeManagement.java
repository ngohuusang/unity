/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.Collection;
import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Attribute types management API.
 * @author K. Benedyczak
 */
public interface AttributeTypeManagement
{
	/**
	 * @return identifiers of all attribute value types which are supported by server. 
	 * The list is constant for the lifetime of the server as is constructed from the available implementations.
	 * @throws EngineException
	 */
	String[] getSupportedAttributeValueTypes() throws EngineException;
	
	/**
	 * Adds a new attribute type.
	 * @param at
	 * @throws EngineException
	 */
	void addAttributeType(AttributeType at) throws EngineException;

	/**
	 * Updates an existing attribute type. Fails if the change break constraints of attributes
	 * already having this attribute set.
	 * @param at
	 * @throws EngineException
	 */
	void updateAttributeType(AttributeType at) throws EngineException;

	/**
	 * Removes attribute type by id.
	 * @param id
	 * @param deleteInstances if false then operation will succeed only if no attributes of this type are
	 * defined. If true then also all instances of this type are removed. 
	 * @throws EngineException
	 */
	void removeAttributeType(String id, boolean deleteInstances) throws EngineException;

	/**
	 * @return all attribute types
	 * @throws EngineException
	 */
	Collection<AttributeType> getAttributeTypes() throws EngineException;

	/**
	 * @return all attribute types map with names as keys
	 * @throws EngineException
	 */
	Map<String, AttributeType> getAttributeTypesAsMap() throws EngineException;
}
