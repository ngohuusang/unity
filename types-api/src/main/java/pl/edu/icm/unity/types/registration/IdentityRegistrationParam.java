/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Identity registration option.
 * @author K. Benedyczak
 */
public class IdentityRegistrationParam extends OptionalRegistrationParam
{
	private String identityType;

	public String getIdentityType()
	{
		return identityType;
	}

	public void setIdentityType(String identityType)
	{
		this.identityType = identityType;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((identityType == null) ? 0 : identityType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdentityRegistrationParam other = (IdentityRegistrationParam) obj;
		if (identityType == null)
		{
			if (other.identityType != null)
				return false;
		} else if (!identityType.equals(other.identityType))
			return false;
		return true;
	}
}
