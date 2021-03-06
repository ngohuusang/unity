/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

/**
 * Creates instances of {@link RegistrationRequestEditor}. May ask for a registration code if needed first.
 * @author Krzysztof Benedyczak
 */
@PrototypeComponent
public class RequestEditorCreator
{
	private UnityMessageSource msg;
	private RegistrationForm form;
	private RemotelyAuthenticatedContext remotelyAuthenticated;
	private IdentityEditorRegistry identityEditorRegistry;
	private CredentialEditorRegistry credentialEditorRegistry;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private InvitationManagement invitationMan;
	private AttributeTypeManagement aTypeMan;
	private GroupsManagement groupsMan;
	private CredentialManagement credMan;

	@Autowired
	public RequestEditorCreator(UnityMessageSource msg, 
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			@Qualifier("insecure") InvitationManagement invitationMan, 
			@Qualifier("insecure") AttributeTypeManagement aTypeMan,
			@Qualifier("insecure") GroupsManagement groupsMan, 
			@Qualifier("insecure") CredentialManagement credMan)
	{
		this.msg = msg;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.invitationMan = invitationMan;
		this.aTypeMan = aTypeMan;
		this.groupsMan = groupsMan;
		this.credMan = credMan;
	}
	
	public RequestEditorCreator init(RegistrationForm form,
			RemotelyAuthenticatedContext remotelyAuthenticated)
	{
		this.form = form;
		this.remotelyAuthenticated = remotelyAuthenticated;
		return this;
	}
	
	public void invoke(RequestEditorCreatedCallback callback)
	{
		String registrationCode = RegistrationFormDialogProvider.getCodeFromURL();
		if (registrationCode == null && form.isByInvitationOnly())
		{
			askForCode(callback);
		} else
		{
			doCreateEditor(registrationCode, callback);
		}
	}
	
	private void askForCode(RequestEditorCreatedCallback callback)
	{
		GetRegistrationCodeDialog askForCodeDialog = new GetRegistrationCodeDialog(msg, 
				new GetRegistrationCodeDialog.Callback()
		{
			@Override
			public void onCodeGiven(String code)
			{
				doCreateEditor(code, callback);
			}
			
			@Override
			public void onCancel()
			{
				callback.onCancel();
			}
		});
		askForCodeDialog.show();
	}

	private void doCreateEditor(String registrationCode, RequestEditorCreatedCallback callback)
	{
		try
		{
			RegistrationRequestEditor editor = new RegistrationRequestEditor(msg, form, 
					remotelyAuthenticated, identityEditorRegistry, 
					credentialEditorRegistry, attributeHandlerRegistry, 
					aTypeMan, credMan, groupsMan, 
					registrationCode, invitationMan);
			callback.onCreated(editor);
		} catch (Exception e)
		{
			callback.onCreationError(e);
		}
	}
	
	public interface RequestEditorCreatedCallback
	{
		void onCreated(RegistrationRequestEditor editor);
		void onCreationError(Exception e);
		void onCancel();
	}
}
