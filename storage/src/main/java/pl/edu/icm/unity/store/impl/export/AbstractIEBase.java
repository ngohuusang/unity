/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.export;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Generic base code for importers and exporters.
 * @author K. Benedyczak
 */
abstract class AbstractIEBase<T>
{
	/**
	 * @return all objects to be exported
	 */
	protected abstract List<T> getAllToExport();
	/**
	 * Converts in-db object to JSON form
	 * @param exportedObj
	 * @return
	 */
	protected abstract ObjectNode toJsonSingle(T exportedObj);

	/**
	 * Converts imported object from JSON to the in-db form
	 * @param src
	 * @return
	 */
	protected abstract T fromJsonSingle(ObjectNode src);

	/**
	 * Stores in imported object in db
	 * @param toCreate
	 */
	protected abstract void createSingle(T toCreate);
	
	
	public void serialize(JsonGenerator jg) throws IOException
	{
		List<T> all = getAllToExport();
		jg.writeStartArray();
		for (T obj: all)
		{
			jg.writeStartObject();
			serializeToJson(jg, obj);
			jg.writeEndObject();
		}
		jg.writeEndArray();
	}
	
	protected void serializeToJson(JsonGenerator jg, T obj) throws IOException
	{
		ObjectNode asJson = toJsonSingle(obj);
		jg.writeTree(asJson);
	}
	
	public void deserialize(JsonParser input) throws IOException
	{
		JsonUtils.expect(input, JsonToken.START_ARRAY);
		while(input.nextToken() == JsonToken.START_OBJECT)
		{
			T obj = deserializeFromJson(input);
			JsonUtils.nextExpect(input, JsonToken.END_OBJECT);
			createSingle(obj);
		}
		JsonUtils.expect(input, JsonToken.END_ARRAY);
	}

	private T deserializeFromJson(JsonParser input) throws IOException
	{
		ObjectNode read = input.readValueAsTree();
		return fromJsonSingle(read);
	}
}


