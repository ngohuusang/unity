/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.MapComboBox;

/**
 * Allows to select an attribute name
 * @author K. Benedyczak
 * 
 * @deprecated use {@link AttributeSelectionComboBox2}
 */
@Deprecated
public class AttributeSelectionComboBox extends MapComboBox<AttributeType>
{
	public AttributeSelectionComboBox(String caption, AttributeTypeManagement aTypeMan) throws EngineException
	{
		Collection<AttributeType> attributeTypes = aTypeMan.getAttributeTypes();
		initContents(caption, attributeTypes);		
	}
	
	public AttributeSelectionComboBox(String caption, Collection<AttributeType> attributeTypes)
	{
		initContents(caption, attributeTypes);
	}
	
	private void initContents(String caption, Collection<AttributeType> attributeTypes)
	{
		setNullSelectionAllowed(false);
		setSizeUndefined();
		setCaption(caption);
		
		SortedMap<String, AttributeType> typesByName = new TreeMap<>();
		for (AttributeType at: attributeTypes)
		{
			if (!at.isInstanceImmutable())
				typesByName.put(at.getName(), at);
		}
		String chosen = typesByName.size() > 0 ? typesByName.keySet().iterator().next() : null;
		
		init(typesByName, chosen);
	}
}
