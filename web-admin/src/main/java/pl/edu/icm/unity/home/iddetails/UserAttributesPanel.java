/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.AbstractOrderedLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.home.HomeEndpointProperties;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewer;
import pl.edu.icm.unity.webui.common.attributes.FixedAttributeEditor;

/**
 * Shows (optionally in edit mode) all configured attributes.
 * 
 * @author K. Benedyczak
 */
public class UserAttributesPanel
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, UserAttributesPanel.class);
	private UnityMessageSource msg;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private AttributesManagement attributesMan;
	private HomeEndpointProperties config;
	private long entityId;
	
	private List<FixedAttributeEditor> attributeEditors;

	private AbstractOrderedLayout parent;
	private List<AttributeViewer> viewers;
	private EntityManagement idsMan;
	private AttributeSupport atMan;
	
	public UserAttributesPanel(UnityMessageSource msg,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributesManagement attributesMan, EntityManagement idsMan,
			AttributeSupport atMan,
			HomeEndpointProperties config,
			long entityId) throws EngineException
	{
		this.msg = msg;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.attributesMan = attributesMan;
		this.idsMan = idsMan;
		this.atMan = atMan;
		this.config = config;
		this.entityId = entityId;
	}

	public void addIntoLayout(AbstractOrderedLayout layout) throws EngineException
	{
		this.parent = layout;
		initUI();
	}
	
	private void initUI() throws EngineException
	{
		attributeEditors = new ArrayList<>();
		viewers = new ArrayList<>();
		Set<String> keys = config.getStructuredListKeys(HomeEndpointProperties.ATTRIBUTES);
		
		Map<String, AttributeType> atTypes = atMan.getAttributeTypesAsMap();
		Set<String> groups = idsMan.getGroupsForPresentation(new EntityParam(entityId)).
				stream().map(g -> g.toString()).collect(Collectors.toSet());
		for (String aKey: keys)
			addAttribute(atTypes, aKey, groups);
	}
	
	private void addAttribute(Map<String, AttributeType> atTypes, String key, Set<String> groups)
	{		
		String group = config.getValue(key+HomeEndpointProperties.GWA_GROUP);
		String attributeName = config.getValue(key+HomeEndpointProperties.GWA_ATTRIBUTE);
		boolean showGroup = config.getBooleanValue(key+HomeEndpointProperties.GWA_SHOW_GROUP);
		boolean editable = config.getBooleanValue(key+HomeEndpointProperties.GWA_EDITABLE);
		
		AttributeType at = atTypes.get(attributeName);
		if (at == null)
		{
			log.warn("No attribute type " + attributeName + " defined in the system.");
			return;
		}
		AttributeExt attribute = getAttribute(attributeName, group);

		if (!groups.contains(group))
			return;
		
		if (editable && at.isSelfModificable())
		{
			FixedAttributeEditor editor = new FixedAttributeEditor(msg, attributeHandlerRegistry, 
				at, showGroup, group, 
				null, null, false, false, parent);
			if (attribute != null)
				editor.setAttributeValues(attribute.getValues());
			attributeEditors.add(editor);
		} else
		{
			if (attribute == null)
				return;
			
			AttributeViewer viewer = new AttributeViewer(msg, attributeHandlerRegistry, at, 
					attribute, showGroup);
			viewers.add(viewer);
			viewer.addToLayout(parent);
		}
	}
	
	private void clear()
	{
		for (AttributeViewer viewer: viewers)
			viewer.removeFromLayout(parent);
		for (FixedAttributeEditor editor: attributeEditors)
			editor.clear();
	}
	
	public void refresh() throws EngineException
	{
		clear();
		initUI();
	}
	
	private AttributeExt getAttribute(String attributeName, String group)
	{
		Collection<AttributeExt> attributes;
		try
		{
			attributes = attributesMan.getAttributes(
					new EntityParam(entityId), group, attributeName);
		} catch (EngineException e)
		{
			log.debug("Can not resolve attribute " + attributeName + " for entity", e);
			return null;
		}
		if (attributes.isEmpty())
			return null;
		return attributes.iterator().next();
	}
	
	public void validate() throws FormValidationException
	{
		for (FixedAttributeEditor ae: attributeEditors)
			ae.getAttribute();
	}
	
	public void saveChanges() throws Exception
	{
		for (FixedAttributeEditor ae: attributeEditors)
		{
			try
			{
				Attribute a = ae.getAttribute();
				if (a != null)
					updateAttribute(a);
				else
					removeAttribute(ae);
			} catch (FormValidationException e)
			{
				continue;
			}
		}
	}
	
	private void updateAttribute(Attribute a) throws EngineException
	{
		attributesMan.setAttribute(new EntityParam(entityId), a, true);
	}
	
	private void removeAttribute(FixedAttributeEditor ae) throws EngineException
	{
		try
		{
			attributesMan.removeAttribute(new EntityParam(entityId), 
					ae.getGroup(), ae.getAttributeType().getName());
		} catch (IllegalAttributeValueException e)
		{
			//OK - attribute already doesn't exist
		}
	}

	public boolean hasEditable()
	{
		return attributeEditors.size() > 0;
	}
}
