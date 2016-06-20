/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.List;
import java.util.stream.Collectors;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Helper class allowing to create integer attributes easily.
 * @author K. Benedyczak
 */
public class IntegerAttribute extends Attribute
{
	public IntegerAttribute(String name, String groupPath,
			List<String> values, String remoteIdp, String translationProfile)
	{
		super(name, IntegerAttributeSyntax.ID, groupPath, values, remoteIdp, translationProfile);
	}

	public IntegerAttribute(String name, String groupPath, List<Long> values)
	{
		super(name, IntegerAttributeSyntax.ID, groupPath, convert(values));
	}
	
	private static List<String> convert(List<Long> values)
	{
		IntegerAttributeSyntax syntax = new IntegerAttributeSyntax();
		return values.stream().
				map(v -> syntax.convertToString(v)).
				collect(Collectors.toList());
	}
}
