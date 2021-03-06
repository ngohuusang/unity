/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import pl.edu.icm.unity.rest.TestRESTBase;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttribute;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttribute;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.JpegImageAttribute;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.VerifiableEmail;


public class TestQuery extends TestRESTBase
{
	private ObjectMapper m = new ObjectMapper();
	
	{
		m.enable(SerializationFeature.INDENT_OUTPUT);
	}
	
	@Before
	public void init() throws Exception
	{
		setupPasswordAuthn();
		createUsernameUserWithRole("System Manager");
		super.deployEndpoint(RESTAdminEndpoint.NAME, 
				"restAdmin", "/restadm");
	}
	
	@Test
	public void resolveOfEmailWithTagsReturnsEntity() throws Exception
	{
		idsMan.addEntity(new IdentityParam(EmailIdentity.ID, "a+zzz@ex.com"), "cr-pass", 
				EntityState.valid, false);
		
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(client, host);

		HttpGet resolve = new HttpGet("/restadm/v1/resolve/email/a+foo@ex.com");
		HttpResponse response = client.execute(host, resolve, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("User's info:\n" + formatJson(contents));
	}	
	
	@Test
	public void testQuery() throws Exception
	{
		long e = createTestContents();
		
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(client, host);

		HttpGet resolve = new HttpGet("/restadm/v1/resolve/userName/admin");
		HttpResponse response = client.execute(host, resolve, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("User's info:\n" + formatJson(contents));
		
		HttpGet getGroups = new HttpGet("/restadm/v1/entity/"+e+"/groups");
		response = client.execute(host, getGroups, localcontext);
		contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("User's groups:\n" + contents);
		
		HttpGet getGroupContents = new HttpGet("/restadm/v1/group/%2Fexample%2Fsub");
		response = client.execute(host, getGroupContents, localcontext);
		contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("Group's /example/sub contents:\n" + formatJson(contents));

		HttpGet getAttributes = new HttpGet("/restadm/v1/entity/" + e + "/attributes?group=%2Fexample");
		response = client.execute(host, getAttributes, localcontext);
		contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("Attributes in /example:\n" + formatJson(contents));
	}
	
	@Test
	public void fullEntityIsReturned() throws Exception
	{
		long e = createTestContents();
		
		HttpGet getEntity = new HttpGet("/restadm/v1/entity/"+e);
		HttpResponse response = executeQuery(getEntity);
		
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("User's info:\n" + formatJson(contents));
		Entity parsed = m.readValue(contents, Entity.class);
		assertThat(parsed.getId(), is(e));
	}
	
	@Test
	public void queryByPersistentIdWorks() throws Exception
	{
		long entityId = createTestContents();
		
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(client, host);

		Entity entity = idsMan.getEntity(new EntityParam(entityId));
		Identity persistent = entity.getIdentities().stream().
			filter(i -> i.getTypeId().equals(PersistentIdentity.ID)).
			findFirst().
			get();
				
		
		HttpGet getGroups = new HttpGet("/restadm/v1/entity/"+persistent.getValue()+"/groups");
		HttpResponse response = client.execute(host, getGroups, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("User's groups:\n" + contents);
	}

	@Test
	public void queryWithExplicitIdTypeWorks() throws Exception
	{
		createTestContents();
		
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(client, host);

		HttpGet getGroups = new HttpGet("/restadm/v1/entity/tested/groups?identityType=" 
				+ UsernameIdentity.ID);
		HttpResponse response = client.execute(host, getGroups, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("User's groups:\n" + contents);
	}
	
	
	private HttpResponse executeQuery(HttpRequest request) throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(client, host);
		return client.execute(host, request, localcontext);
	}
	
	
	protected long createTestContents() throws Exception
	{
		groupsMan.addGroup(new Group("/example"));
		groupsMan.addGroup(new Group("/example/sub"));
		Identity id = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tested"), "cr-pass", 
				EntityState.valid, false);
		EntityParam e = new EntityParam(id);
		groupsMan.addMemberFromParent("/example", e);
		groupsMan.addMemberFromParent("/example/sub", e);
		
		aTypeMan.addAttributeType(new AttributeType("stringA", StringAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("intA", IntegerAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("floatA", FloatingPointAttributeSyntax.ID));
		EnumAttributeSyntax enumSyntax = new EnumAttributeSyntax("V1", "V2");
		AttributeType enumAT = new AttributeType("enumA", EnumAttributeSyntax.ID);
		enumAT.setValueSyntaxConfiguration(enumSyntax.getSerializedConfiguration());
		aTypeMan.addAttributeType(enumAT);
		aTypeMan.addAttributeType(new AttributeType("jpegA", JpegImageAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("emailA", VerifiableEmailAttributeSyntax.ID));
		
		attrsMan.setAttribute(e, StringAttribute.of("stringA", "/example", 
				"value"), false);
		attrsMan.setAttribute(e, IntegerAttribute.of("intA", "/example", 
				12), false);
		attrsMan.setAttribute(e, FloatingPointAttribute.of("floatA", "/example", 
				12.9), false);
		attrsMan.setAttribute(e, JpegImageAttribute.of("jpegA", "/example", 
				new BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB)), false);
		attrsMan.setAttribute(e, EnumAttribute.of("enumA", "/example", 
				"V1"), false);
		attrsMan.setAttribute(e, VerifiableEmailAttribute.of("emailA", "/example", 
				new VerifiableEmail("some@example.com")), false);
		return id.getEntityId();
	}
	
	public String formatJson(String contents) throws JsonProcessingException, IOException
	{
		JsonNode n = m.readTree(contents);
		return m.writeValueAsString(n);
	}
}
