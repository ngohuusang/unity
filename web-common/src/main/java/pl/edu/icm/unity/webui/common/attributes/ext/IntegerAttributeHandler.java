/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.TextOnlyAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;
import pl.edu.icm.unity.webui.common.boundededitors.LongBoundEditor;


/**
 * Integer attribute handler for the web
 * @author K. Benedyczak
 */
public class IntegerAttributeHandler extends TextOnlyAttributeHandler
{
	private UnityMessageSource msg;

	public IntegerAttributeHandler(UnityMessageSource msg, AttributeValueSyntax<?> syntax)
	{
		super(syntax);
		this.msg = msg;
	}


	@Override
	protected List<String> getHints()
	{
		List<String> sb = new ArrayList<String>(2);
		IntegerAttributeSyntax syntax = (IntegerAttributeSyntax) this.syntax;
		
		if (syntax.getMin() != Long.MIN_VALUE)
			sb.add(msg.getMessage("NumericAttributeHandler.min", syntax.getMin()));
		else
			sb.add(msg.getMessage("NumericAttributeHandler.minUndef"));
		if (syntax.getMax() != Long.MAX_VALUE)
			sb.add(msg.getMessage("NumericAttributeHandler.max", syntax.getMax()));
		else
			sb.add(msg.getMessage("NumericAttributeHandler.maxUndef"));
		
		return sb;
	}
	
	private static class IntegerSyntaxEditor implements AttributeSyntaxEditor<Long>
	{
		private IntegerAttributeSyntax initial;
		private LongBoundEditor max, min;
		private UnityMessageSource msg;
		
		
		public IntegerSyntaxEditor(IntegerAttributeSyntax initial, UnityMessageSource msg)
		{
			this.initial = initial;
			this.msg = msg;
		}

		@Override
		public Component getEditor()
		{
			FormLayout fl = new CompactFormLayout();
			min = new LongBoundEditor(msg, msg.getMessage("NumericAttributeHandler.minUndef"), 
					msg.getMessage("NumericAttributeHandler.minE"), Long.MIN_VALUE);
			max = new LongBoundEditor(msg, msg.getMessage("NumericAttributeHandler.maxUndef"), 
					msg.getMessage("NumericAttributeHandler.maxE"), Long.MAX_VALUE);
			if (initial != null)
			{
				max.setValue(initial.getMax());
				min.setValue(initial.getMin());
			} else
			{
				max.setValue(Long.MAX_VALUE);
				min.setValue(0l);
			}
			fl.addComponents(min, max);
			return fl;
		}

		@Override
		public AttributeValueSyntax<Long> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			try
			{
				IntegerAttributeSyntax ret = new IntegerAttributeSyntax();
				ret.setMax(max.getValue());
				ret.setMin(min.getValue());
				return ret;
			} catch (Exception e)
			{
				throw new IllegalAttributeTypeException(e.getMessage(), e);
			}
		}
	}

	
	@org.springframework.stereotype.Component
	public static class IntegerAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private UnityMessageSource msg;

		@Autowired
		public IntegerAttributeHandlerFactory(UnityMessageSource msg)
		{
			this.msg = msg;
		}
		

		@Override
		public String getSupportedSyntaxId()
		{
			return IntegerAttributeSyntax.ID;
		}
		
		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new IntegerAttributeHandler(msg, syntax);
		}
		
		@Override
		public AttributeSyntaxEditor<Long> getSyntaxEditorComponent(
				AttributeValueSyntax<?> initialValue)
		{
			return new IntegerSyntaxEditor((IntegerAttributeSyntax) initialValue, msg);
		}
	}
}
