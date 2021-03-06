/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmation.facilities;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.confirmation.ConfirmationRedirectURLBuilder.ConfirmedElementType;
import pl.edu.icm.unity.engine.api.confirmation.ConfirmationStatus;
import pl.edu.icm.unity.engine.api.confirmation.states.AttribiuteConfirmationState;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.AttributeExt;

/**
 * Attribute confirmation facility.
 * 
 * @author P. Piernik
 */
@Component
public class AttributeFacility extends UserFacility<AttribiuteConfirmationState>
{
	private AttributeDAO dbAttributes;
	private AttributeTypeHelper atHelper;

	@Autowired
	public AttributeFacility(AttributeDAO dbAttributes, EntityDAO dbIdentities,
			AttributeTypeHelper atHelper)
	{
		super(dbIdentities);
		this.dbAttributes = dbAttributes;
		this.atHelper = atHelper;
	}

	@Override
	public String getName()
	{
		return AttribiuteConfirmationState.FACILITY_ID;
	}

	@Override
	public String getDescription()
	{
		return "Confirms attributes from entity with verifiable values";
	}

	@Override
	protected ConfirmationStatus confirmElements(AttribiuteConfirmationState attrState) 
			throws EngineException
	{
		ConfirmationStatus status;
		List<AttributeExt> allAttrs = dbAttributes.getEntityAttributes(attrState.getOwnerEntityId(), 
				attrState.getType(), attrState.getGroup());

		Collection<AttributeExt> confirmedList = confirmAttributes(
				allAttrs, attrState.getType(),
				attrState.getGroup(), attrState.getValue(), atHelper);

		for (AttributeExt attr : confirmedList)
		{
			StoredAttribute confirmed = new StoredAttribute(attr, attrState.getOwnerEntityId());
			dbAttributes.updateAttribute(confirmed);
		}
		boolean confirmed = (confirmedList.size() > 0);
		status = new ConfirmationStatus(confirmed, 
				confirmed ? getSuccessRedirect(attrState) : getErrorRedirect(attrState),
						confirmed ? "ConfirmationStatus.successAttribute"
								: "ConfirmationStatus.attributeChanged",
								attrState.getType());
		return status;
	}

	@Override
	@Transactional
	public void processAfterSendRequest(String state) throws EngineException
	{
		AttribiuteConfirmationState attrState = new AttribiuteConfirmationState(state);
		Collection<AttributeExt> allAttrs = dbAttributes.getEntityAttributes(
				attrState.getOwnerEntityId(),
				attrState.getType(), attrState.getGroup());
		for (AttributeExt attr : allAttrs)
		{
			AttributeValueSyntax<?> syntax = atHelper.getUnconfiguredSyntax(attr.getValueSyntax());
			if (syntax.isVerifiable())
			{
				updateConfirmationForAttributeValues(attr.getValues(), syntax, attrState.getValue());
				StoredAttribute updated = new StoredAttribute(attr, attrState.getOwnerEntityId());
				dbAttributes.updateAttribute(updated);
			}
		}
	}

	@Override
	public AttribiuteConfirmationState parseState(String state)
	{
		return new AttribiuteConfirmationState(state);
	}

	@Override
	protected ConfirmedElementType getConfirmedElementType(AttribiuteConfirmationState state)
	{
		return ConfirmedElementType.attribute;
	}
}
