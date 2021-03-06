/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.mvel2.MVEL;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Allows for adding an additional identity to the requester
 * 
 * @author K. Benedyczak
 */
@Component
public class AddIdentityActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "addIdentity";
	
	public AddIdentityActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition(
						"identityType",
						"RegTranslationAction.addIdentity.paramDesc.identityType",
						Type.UNITY_ID_TYPE),
				new ActionParameterDefinition(
						"identity",
						"RegTranslationAction.addIdentity.paramDesc.identity",
						Type.EXPRESSION)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new AddIdentityAction(getActionType(), parameters);
	}
	
	public static class AddIdentityAction extends RegistrationTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				AddIdentityActionFactory.AddIdentityAction.class);
		private String identityType;
		private Serializable expressionCompiled;
		
		public AddIdentityAction(TranslationActionType description, String[] parameters) 
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			Object value = MVEL.executeExpression(expressionCompiled, mvelCtx, new HashMap<>());
			if (value == null)
			{
				log.debug("Identity value evaluated to null, skipping");
				return;
			}
			
			IdentityParam identity = new IdentityParam(identityType, value.toString(), null, currentProfile);
			log.debug("Mapped identity: " + identity);
			state.addIdentity(identity);
		}

		private void setParameters(String[] parameters)
		{
			if (parameters.length != 2)
				throw new IllegalArgumentException("Action requires exactly 2 parameters");
			identityType = parameters[0];
			expressionCompiled = MVEL.compileExpression(parameters[1]);
		}
	}
}
