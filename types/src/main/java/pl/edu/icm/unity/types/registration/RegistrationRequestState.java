/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.List;

/**
 * Records an information about registration request state. The objects of this class are fully managed by the 
 * engine, users can only retrieve it.  
 * 
 * @author K. Benedyczak
 */
public class RegistrationRequestState
{
	private String requestId;
	private RegistrationRequest request;
	private List<AdminComment> adminComments;
	private List<AdminComment> publicAdminComments;
	private RegistrationRequestStatus status;

	public String getRequestId()
	{
		return requestId;
	}
	public void setRequestId(String requestId)
	{
		this.requestId = requestId;
	}
	public RegistrationRequest getRequest()
	{
		return request;
	}
	public void setRequest(RegistrationRequest request)
	{
		this.request = request;
	}
	public List<AdminComment> getAdminComments()
	{
		return adminComments;
	}
	public void setAdminComments(List<AdminComment> adminComments)
	{
		this.adminComments = adminComments;
	}
	public List<AdminComment> getPublicAdminComments()
	{
		return publicAdminComments;
	}
	public void setPublicAdminComments(List<AdminComment> publicAdminComments)
	{
		this.publicAdminComments = publicAdminComments;
	}
	public RegistrationRequestStatus getStatus()
	{
		return status;
	}
	public void setStatus(RegistrationRequestStatus status)
	{
		this.status = status;
	}
}