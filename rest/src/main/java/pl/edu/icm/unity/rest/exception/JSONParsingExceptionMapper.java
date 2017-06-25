/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.types.JsonError;

/**
 * Maps JSON parse exceptions to HTTP error responses
 * @author K. Benedyczak
 */
@Provider
public class JSONParsingExceptionMapper implements ExceptionMapper<JSONParsingException>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, JSONParsingExceptionMapper.class);
	
	public Response toResponse(JSONParsingException ex)
	{
		log.error("JSON parse error during RESTful API invocation", ex);
		return Response.status(Status.BAD_REQUEST).entity(new JsonError(ex).toString()).
				type(MediaType.APPLICATION_JSON).build();
	}
}
