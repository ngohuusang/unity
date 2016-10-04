/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.form.action.AddAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AddToGroupActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.utils.InitializerCommon;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseBuilder;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

public class TestEnquiries extends DBIntegrationTestBase
{
	@Autowired
	private InitializerCommon commonInitializer;
	
	@Autowired
	private EnquiryManagement enquiryManagement;


	@Before
	public void init() throws EngineException
	{
		setupPasswordAuthn();
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
	}

	
	@Test 
	public void addedEnquiryIsReturned() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		
		List<EnquiryForm> forms = enquiryManagement.getEnquires();
		
		assertThat(forms.size(), is(1));
		assertThat(forms.get(0), is(form));
	}
	
	@Test 
	public void removedFormIsNotReturned() throws Exception
	{
		initAndCreateEnquiry(null);
		
		enquiryManagement.removeEnquiry("f1", true);
		
		assertThat(enquiryManagement.getEnquires().isEmpty(), is(true));
	}

	@Test 
	public void missingFormCantBeRemoved() throws Exception
	{
		catchException(enquiryManagement).removeEnquiry("missing", true);
		
		assertThat(caughtException(), isA(IllegalArgumentException.class));
	}
	
	@Test 
	public void formWithDuplicateNameCantBeAdded() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		
		catchException(enquiryManagement).addEnquiry(form);
		
		assertThat(caughtException(), isA(IllegalArgumentException.class));
	}
	
	@Test 
	public void formWithMissingAttributeCantBeAdded() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		EnquiryFormBuilder testFormBuilder = getFormBuilder(null);

		AttributeRegistrationParam attrReg = form.getAttributeParams().get(0);
		attrReg.setAttributeType("missing");
		testFormBuilder.withAttributeParams(Collections.singletonList(attrReg));
		
		checkUpdateOrAdd(testFormBuilder.build(), "attr(2)", IllegalArgumentException.class);
	}
	
	@Test 
	public void formWithMissingCredentialCantBeAdded() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		EnquiryFormBuilder testFormBuilder = getFormBuilder(null);

		CredentialRegistrationParam credParam = form.getCredentialParams().get(0);
		credParam.setCredentialName("missing");
		testFormBuilder.withCredentialParams(Collections.singletonList(credParam));
		
		checkUpdateOrAdd(testFormBuilder.build(), "cred", IllegalArgumentException.class);
	}

	
	@Test 
	public void formWithMissingGroupCantBeAdded() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		EnquiryFormBuilder testFormBuilder = getFormBuilder(null);

		GroupRegistrationParam groupParam = form.getGroupParams().get(0);
		groupParam.setGroupPath("/missing");
		testFormBuilder.withGroupParams(Collections.singletonList(groupParam));
		checkUpdateOrAdd(testFormBuilder.build(), "group", IllegalArgumentException.class);
	}

	@Test 
	public void formWithMissingIdentityCantBeAdded() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		EnquiryFormBuilder testFormBuilder = getFormBuilder(null);
		IdentityRegistrationParam idParam = form.getIdentityParams().get(0);
		idParam.setIdentityType("missing");
		testFormBuilder.withIdentityParams(Collections.singletonList(idParam));
		checkUpdateOrAdd(testFormBuilder.build(), "id", IllegalArgumentException.class);
	}

	@Test
	public void artefactsPresentInFormCantBeRemoved() throws Exception
	{
		initAndCreateEnquiry(null);
		
		catchException(aTypeMan).removeAttributeType(InitializerCommon.EMAIL_ATTR, true);
		assertThat(caughtException(), isA(IllegalArgumentException.class));

		catchException(groupsMan).removeGroup("/B", true);
		assertThat(caughtException(), isA(IllegalArgumentException.class));

		catchException(credMan).removeCredentialDefinition(EngineInitialization.DEFAULT_CREDENTIAL);
		assertThat(caughtException(), isA(IllegalArgumentException.class));
	}
	
	
	@Test
	public void requestWithoutOptionalFieldsIsAccepted() throws Exception
	{
		initAndCreateEnquiry("true");
		EnquiryResponse response = new EnquiryResponseBuilder()
			.withFormId("f1")
			.withComments("comments")
			.withAddedAgreement()
				.withSelected(true)
			.endAgreement()
			.withAddedCredential()
				.withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abc").toJson())
			.endCredential()
			.withAddedAttribute(null)
			.withAddedIdentity(null)
			.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "test-user"))
			.withAddedGroupSelection(null)
			.build();
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid, false);
		
		setupUserContext("tuser", false);
		enquiryManagement.submitEnquiryResponse(response, new RegistrationContext(true, false, 
				TriggeringMode.manualStandalone));
		setupAdmin();
		
		
		Entity entity = idsMan.getEntity(new EntityParam(new IdentityParam(UsernameIdentity.ID, "test-user")));
		Collection<Identity> usernames = getIdentitiesByType(entity.getIdentities(), UsernameIdentity.ID);
		assertThat(usernames.size(), is(2));
		assertThat(usernames, hasItem(identity));
	}

	@Test
	public void matchingEnquiryIsPending() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid, false);
		EntityParam entityParam = new EntityParam(identity);
		groupsMan.addMemberFromParent("/A", entityParam);
		EnquiryForm form = new EnquiryFormBuilder()
			.withTargetGroups(new String[] {"/A"})
			.withType(EnquiryType.REQUESTED_OPTIONAL)
			.withName("tenquiry")
			.build();
		enquiryManagement.addEnquiry(form);
		
		List<EnquiryForm> pendingEnquires = enquiryManagement.getPendingEnquires(entityParam);
		
		assertThat(pendingEnquires.size(), is(1));
		assertThat(pendingEnquires.get(0), is(form));
	}

	@Test
	public void enquiryForDifferentGroupIsNotPending() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid, false);
		EntityParam entityParam = new EntityParam(identity);
		EnquiryForm form = new EnquiryFormBuilder()
			.withTargetGroups(new String[] {"/A"})
			.withType(EnquiryType.REQUESTED_OPTIONAL)
			.withName("tenquiry")
			.build();
		enquiryManagement.addEnquiry(form);
		
		List<EnquiryForm> pendingEnquires = enquiryManagement.getPendingEnquires(entityParam);
		
		assertThat(pendingEnquires.isEmpty(), is(true));
	}
	
	@Test
	public void submittedEnquiryIsNotPendingAnymore() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid, false);
		EntityParam entityParam = new EntityParam(identity);
		EnquiryForm form = new EnquiryFormBuilder()
			.withTargetGroups(new String[] {"/"})
			.withType(EnquiryType.REQUESTED_OPTIONAL)
			.withName("enquiry1")
			.build();
		enquiryManagement.addEnquiry(form);

		setupUserContext("tuser", false);
		EnquiryResponse response = new EnquiryResponseBuilder()
			.withFormId("enquiry1")
			.build();
		enquiryManagement.submitEnquiryResponse(response, 
				new RegistrationContext(true, false, TriggeringMode.manualStandalone));
		setupAdmin();
		
		List<EnquiryForm> pendingEnquires = enquiryManagement.getPendingEnquires(entityParam);
		
		assertThat(pendingEnquires.isEmpty(), is(true));
	}
	
	@Test
	public void ignoredEnquiryIsNotPendingAnymore() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid, false);
		EntityParam entityParam = new EntityParam(identity);
		EnquiryForm form = new EnquiryFormBuilder()
			.withName("e1")
			.withType(EnquiryType.REQUESTED_OPTIONAL)
			.withTargetGroups(new String[] {"/"})
			.build();
		enquiryManagement.addEnquiry(form);
		enquiryManagement.ignoreEnquiry("e1", entityParam);
		
		List<EnquiryForm> pendingEnquires = enquiryManagement.getPendingEnquires(entityParam);
		
		assertThat(pendingEnquires.isEmpty(), is(true));
	}
	
	@Test
	public void mandatoryEnquiryCanNotBeIgnored() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid, false);
		EntityParam entityParam = new EntityParam(identity);
		EnquiryForm form = new EnquiryFormBuilder()
			.withName("e1")
			.withTargetGroups(new String[] {"/"})
			.withType(EnquiryType.REQUESTED_MANDATORY)
			.build();
		enquiryManagement.addEnquiry(form);
		
		catchException(enquiryManagement).ignoreEnquiry("e1", entityParam);
		
		assertThat(caughtException(), isA(WrongArgumentException.class));
	}

	private EnquiryFormBuilder getFormBuilder(String autoAcceptCondition)
	{
		TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME, 
				new String[] {AutomaticRequestAction.accept.toString()});
		TranslationAction a2 = new TranslationAction(AddToGroupActionFactory.NAME, 
				new String[] {"'/A'"});
		TranslationAction a3 = new TranslationAction(AddAttributeActionFactory.NAME, 
				new String[] {"cn", "/", "'val'"});
		
		String autoAcceptCnd = autoAcceptCondition == null ? "false" : autoAcceptCondition;
		
		List<TranslationRule> rules = Lists.newArrayList(new TranslationRule(autoAcceptCnd, a1),
				new TranslationRule("true", a2),
				new TranslationRule("true", a3));
		
		TranslationProfile translationProfile = new TranslationProfile("form", "", 
				ProfileType.REGISTRATION, rules);
		
		return new EnquiryFormBuilder()
				.withName("f1")
				.withDescription("desc")
				.withTranslationProfile(translationProfile)
				.withTargetGroups(new String[] {"/"})
				.withType(EnquiryType.REQUESTED_MANDATORY)
				.withCollectComments(true)
				.withFormInformation(new I18nString("formInformation"))
				.withAddedCredentialParam(new CredentialRegistrationParam(EngineInitialization.DEFAULT_CREDENTIAL))
				.withAddedAgreement(new AgreementRegistrationParam(new I18nString("a"), false))
				.withAddedIdentityParam()
				.withIdentityType(X500Identity.ID)
				.withOptional(true)
				.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.withAddedIdentityParam()
				.withIdentityType(UsernameIdentity.ID)
				.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.withAddedAttributeParam()
				.withAttributeType(InitializerCommon.EMAIL_ATTR).withGroup("/")
				.withOptional(true)
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.withShowGroups(true).endAttributeParam()
				.withAddedGroupParam()
				.withGroupPath("/B")
				.withRetrievalSettings(ParameterRetrievalSettings.automatic)
				.endGroupParam();
	}
	
	private void checkUpdateOrAdd(EnquiryForm form, String msg, Class<?> exception) throws EngineException
	{
		try
		{
			enquiryManagement.addEnquiry(form);
			fail("Added the form with illegal " + msg);
		} catch (Exception e) 
		{
			assertTrue(e.toString(), e.getClass().isAssignableFrom(exception));
		}
		try
		{
			enquiryManagement.updateEnquiry(form, true);
			fail("Updated the form with illegal " + msg);
		} catch (Exception e) 
		{
			assertTrue(e.toString(), e.getClass().isAssignableFrom(exception));
		}
	}
	
	private EnquiryForm initAndCreateEnquiry(String autoAcceptCondition) throws EngineException
	{
		EnquiryForm form = getFormBuilder(autoAcceptCondition).build();
		enquiryManagement.addEnquiry(form);
		return form;
	}
}
