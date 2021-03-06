/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.idpcommon;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.webui.common.ExpandCollapseButton;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.SelectableAttributeWithValues;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

/**
 * Component showing all attributes that are going to be sent to the requesting service. User
 * can select attributes which should be hidden.
 * By default attributes are collapsed.
 * @author K. Benedyczak
 */
public class ExposedSelectableAttributesComponent extends CustomComponent
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry handlersRegistry;
	
	private Map<String, DynamicAttribute> attributes;
	private Map<String, SelectableAttributeWithValues> attributesHiding;
	private AttributeTypeManagement aTypeMan;
	private AttributeTypeSupport aTypeSupport;
	private boolean enableEdit;
	

	public ExposedSelectableAttributesComponent(UnityMessageSource msg, AttributeHandlerRegistry handlersRegistry,
			AttributeTypeManagement aTypeMan, AttributeTypeSupport aTypeSupport,
			Collection<DynamicAttribute> attributesCol, boolean enableEdit) throws EngineException
	{
		super();
		this.handlersRegistry = handlersRegistry;
		this.msg = msg;
		this.aTypeMan = aTypeMan;
		this.aTypeSupport = aTypeSupport;

		attributes = new HashMap<>();
		for (DynamicAttribute a: attributesCol)
			attributes.put(a.getAttribute().getName(), a);
		this.enableEdit = enableEdit;
		initUI();
	}
	
	/**
	 * @return collection of attributes without the ones hidden by the user.
	 */
	public Map<String, Attribute> getUserFilteredAttributes()
	{
		Map<String, Attribute> ret = new HashMap<>();
		for (Entry<String, SelectableAttributeWithValues> entry : attributesHiding.entrySet())
			if (!entry.getValue().isHidden())
				ret.put(entry.getKey(), entry.getValue().getWithoutHiddenValues());
		return ret;
	}

	/**
	 * @return collection of attributes with values hidden by the user.
	 */
	public Map<String, Attribute> getHiddenAttributes()
	{
		Map<String, Attribute> ret = new HashMap<>();
		for (Entry<String, SelectableAttributeWithValues> entry : attributesHiding.entrySet())
		{
			Attribute hiddenValues = entry.getValue().getHiddenValues();
			if (hiddenValues != null)
				ret.put(entry.getKey(), hiddenValues);
		}
		return ret;
	}
	
	public void setInitialState(Map<String, Attribute> savedState)
	{
		for (Entry<String, Attribute> entry : savedState.entrySet())
		{
			SelectableAttributeWithValues selectableAttributeWithValues = 
					attributesHiding.get(entry.getKey());
			if (selectableAttributeWithValues != null)
				selectableAttributeWithValues.setHiddenValues(entry.getValue());
		}
	}
	
	private void initUI() throws EngineException
	{
		VerticalLayout contents = new VerticalLayout();
		contents.setSpacing(true);
		contents.setMargin(false);

		final VerticalLayout details = new VerticalLayout();
		details.setSpacing(false);
		details.setMargin(false);
		final ExpandCollapseButton showDetails = new ExpandCollapseButton(true, details);
		showDetails.setId("ExposedSelectableAttributes.showDetails");
		
		Label attributesL = new Label(msg.getMessage("ExposedAttributesComponent.attributes"));
		attributesL.addStyleName(Styles.bold.toString());
		
		HtmlLabel credInfo = new HtmlLabel(msg);
		credInfo.setHtmlValue("ExposedAttributesComponent.credInfo");
		credInfo.addStyleName(Styles.vLabelSmall.toString());
		
		contents.addComponent(attributesL);
		contents.addComponent(showDetails);
		contents.addComponent(details);
		
		details.addComponent(credInfo);
		if (enableEdit)
		{
			HtmlLabel attributesInfo = new HtmlLabel(msg,
					"ExposedAttributesComponent.attributesInfo");
			attributesInfo.addStyleName(Styles.vLabelSmall.toString());
			details.addComponent(attributesInfo);
		}
		details.addComponent(getAttributesListComponent());
		setCompositionRoot(contents);
	}
	
	public Component getAttributesListComponent() throws EngineException
	{
		VerticalLayout attributesList = new VerticalLayout();
		attributesList.setSpacing(false);
		attributesList.setMargin(false);
		Label hideL = new Label(msg.getMessage("ExposedAttributesComponent.hide"));
		
		attributesHiding = new HashMap<>();
		Map<String, AttributeType> attributeTypes = aTypeMan.getAttributeTypesAsMap();
		boolean first = true;
		for (DynamicAttribute dat: attributes.values())
		{
			SelectableAttributeWithValues attributeComponent = 
					getAttributeComponent(dat, attributeTypes, hideL);
			if (first)
			{
				first = false;
				hideL = null;
			}
			
			attributesHiding.put(dat.getAttribute().getName(), attributeComponent);
			attributesList.addComponent(attributeComponent);
		}
		
		return attributesList;	
		
	}
	
	public SelectableAttributeWithValues getAttributeComponent(DynamicAttribute dat, 
			Map<String, AttributeType> attributeTypes, Label hideL)
	{
		Attribute at = dat.getAttribute();
		AttributeType attributeType = dat.getAttributeType();
		
		WebAttributeHandler handler;
		if (attributeType == null)
			handler = handlersRegistry.getHandlerWithStringFallback(at);
		else
			handler = handlersRegistry.getHandlerWithStringFallback(attributeType);
		if (attributeType == null)
			attributeType = attributeTypes.get(at.getName());
		if (attributeType == null) //can happen for dynamic attributes from output translation profile
			attributeType = new AttributeType(at.getName(), StringAttributeSyntax.ID);
		
		SelectableAttributeWithValues attributeComponent = new SelectableAttributeWithValues(
				null, enableEdit ? hideL : null, at, getAttributeDisplayedName(dat, attributeType),
				getAttributeDescription(dat, attributeType), !dat.isMandatory() && enableEdit,
				attributeType, handler, msg, aTypeSupport);
		attributeComponent.setWidth(100, Unit.PERCENTAGE);
		
		return attributeComponent;
	
	}
	
	private String getAttributeDescription(DynamicAttribute dat, AttributeType attributeType)
	{
		String attrDescription = dat.getDescription();
		if (attrDescription == null || attrDescription.isEmpty())
		{
			attrDescription = attributeType.getDescription() != null
					? attributeType.getDescription().getValue(msg)
					: dat.getAttribute().getName();
		}
		
		return attrDescription;
	}

	private String getAttributeDisplayedName(DynamicAttribute dat, AttributeType attributeType)
	{
		String attrDisplayedName = dat.getDisplayedName();
		if (attrDisplayedName == null || attrDisplayedName.isEmpty())
		{
			attrDisplayedName = attributeType.getDisplayedName() != null
					? attributeType.getDisplayedName().getValue(msg)
					: dat.getAttribute().getName();
		}
		
		return attrDisplayedName;
	}

}
