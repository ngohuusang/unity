/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;


/**
 * Builder of {@link RegistrationForm}
 */
public class RegistrationFormBuilder extends BaseFormBuilder<RegistrationFormBuilder>
{
	private RegistrationForm instance;

	public RegistrationFormBuilder()
	{
		super(new RegistrationForm());
		instance = (RegistrationForm) getInstance();
	}

	public RegistrationForm build()
	{
		instance.validate();
		instance.validateRegistration();
		return instance;
	}

	public RegistrationFormBuilder withNotificationsConfiguration(RegistrationFormNotifications aValue)
	{
		instance.setNotificationsConfiguration(aValue);

		return this;
	}

	public RegistrationFormNotificationsBuilder withNotificationsConfiguration()
	{
		RegistrationFormNotifications obj = new RegistrationFormNotifications();

		withNotificationsConfiguration(obj);

		return new RegistrationFormNotificationsBuilder(obj, this);
	}

	public RegistrationFormBuilder withByInvitationOnly(boolean aValue)
	{
		instance.setByInvitationOnly(aValue);

		return this;
	}
	
	public RegistrationFormBuilder withRegistrationCode(String aValue)
	{
		instance.setRegistrationCode(aValue);

		return this;
	}

	public RegistrationFormBuilder withPubliclyAvailable(boolean aValue)
	{
		instance.setPubliclyAvailable(aValue);

		return this;
	}

	public RegistrationFormBuilder withDefaultCredentialRequirement(String aValue)
	{
		instance.setDefaultCredentialRequirement(aValue);

		return this;
	}

	public RegistrationFormBuilder withCaptchaLength(int aValue)
	{
		instance.setCaptchaLength(aValue);

		return this;
	}

	public static class RegistrationFormNotificationsBuilder
	{
		private RegistrationFormNotifications instance;
		private RegistrationFormBuilder parent;

		protected RegistrationFormNotificationsBuilder(
				RegistrationFormNotifications aInstance, RegistrationFormBuilder parent)
		{
			instance = aInstance;
			this.parent = parent;
		}

		protected RegistrationFormNotifications getInstance()
		{
			return instance;
		}

		public RegistrationFormNotificationsBuilder withSubmittedTemplate(String aValue)
		{
			instance.setSubmittedTemplate(aValue);

			return this;
		}

		public RegistrationFormNotificationsBuilder withUpdatedTemplate(String aValue)
		{
			instance.setUpdatedTemplate(aValue);

			return this;
		}

		public RegistrationFormNotificationsBuilder withRejectedTemplate(String aValue)
		{
			instance.setRejectedTemplate(aValue);

			return this;
		}

		public RegistrationFormNotificationsBuilder withAcceptedTemplate(String aValue)
		{
			instance.setAcceptedTemplate(aValue);

			return this;
		}

		public RegistrationFormNotificationsBuilder withInvitationTemplate(String aValue)
		{
			instance.setInvitationTemplate(aValue);

			return this;
		}

		public RegistrationFormNotificationsBuilder withChannel(String aValue)
		{
			instance.setChannel(aValue);

			return this;
		}

		public RegistrationFormNotificationsBuilder withAdminsNotificationGroup(String aValue)
		{
			instance.setAdminsNotificationGroup(aValue);

			return this;
		}

		public RegistrationFormBuilder endNotificationsConfiguration()
		{
			return parent;
		}
	}
}
