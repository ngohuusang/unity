/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webadmin.attributetype.AttributeTypesUpdatedEvent;
import pl.edu.icm.unity.webadmin.identities.EntityChangedEvent;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventListener;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorComponent.Level;

import com.vaadin.ui.Panel;
import com.vaadin.ui.themes.Reindeer;


/**
 * Component used for displaying and managing attributes of a single entity. 
 * The contents is managed using {@link AttributesPanel}
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AttributesComponent extends Panel
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AttributesComponent.class);
	private UnityMessageSource msg;
	private AttributesPanel main;
	private AttributesManagement attributesManagement;
	
	@Autowired
	public AttributesComponent(UnityMessageSource msg, AttributesPanel main,
			AttributesManagement attributesManagement)
	{
		super();
		this.msg = msg;
		this.main = main;
		this.attributesManagement = attributesManagement;

		setStyleName(Reindeer.PANEL_LIGHT);
		setSizeFull();
		
		EventsBus bus = WebSession.getCurrent().getEventBus();
		bus.addListener(new EventListener<EntityChangedEvent>()
		{
			@Override
			public void handleEvent(EntityChangedEvent event)
			{
				setInput(event.getEntity() == null ? null :
					new EntityParam(event.getEntity().getId()), event.getGroup());
			}
		}, EntityChangedEvent.class);
		
		bus.addListener(new EventListener<AttributeTypesUpdatedEvent>()
		{
			@Override
			public void handleEvent(AttributeTypesUpdatedEvent event)
			{
				AttributesComponent.this.main.setAttributeTypes(event.getAttributeTypes());
			}
		}, AttributeTypesUpdatedEvent.class);
		setInput(null, "/");
	}
	
	private void setInput(EntityParam owner, String groupPath)
	{
		if (owner == null)
		{
			setCaption(msg.getMessage("Attribute.captionNoEntity"));
			setProblem(msg.getMessage("Attribute.noEntitySelected"), Level.warning);
			return;
		}
		
		setCaption(msg.getMessage("Attribute.caption", owner.getEntityId(), groupPath));
		try
		{
			Collection<AttributeExt<?>> attributesCol = attributesManagement.getAllAttributes(
					owner, true, groupPath, null, true);
			main.setInput(owner, groupPath, attributesCol);
			setContent(main);
		} catch (AuthorizationException e)
		{
			setProblem(msg.getMessage("Attribute.noReadAuthz", groupPath, owner.getEntityId()), 
					Level.error);
		} catch (EngineException e)
		{
			log.fatal("Problem retrieving attributes in the group " + groupPath + " for " 
					+ owner.getEntityId(), e);
			setProblem(msg.getMessage("Attribute.internalError", groupPath), Level.error);
		}
	}
	
	private void setProblem(String message, Level level)
	{
		ErrorComponent errorC = new ErrorComponent();
		errorC.setMessage(message, level);
		setContent(errorC);
	}
}
