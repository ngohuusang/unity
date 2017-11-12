/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.Grid;

/**
 * Table with styles creating a smaller representation.
 * 
 * @author K. Benedyczak
 * @param <T>
 */
public class SmallTable<T> extends Grid<T>
{
	
	public SmallTable(String caption)
	{
		super(caption);
		setup();
	}

	public SmallTable()
	{
		setup();
	}
	
	private void setup()
	{
		addStyleName(Styles.vTableNoHorizontalLines.toString());
		addStyleName(Styles.vSmall.toString());
	}
}
