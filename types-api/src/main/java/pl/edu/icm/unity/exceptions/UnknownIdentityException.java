/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when identity value is invalid. Runtime
 * @author K. Benedyczak
 */
public class UnknownIdentityException extends IllegalArgumentException
{
	public UnknownIdentityException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public UnknownIdentityException(String msg)
	{
		super(msg);
	}
}
