/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.i18n;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.v7.ui.AbstractTextField;
import com.vaadin.v7.ui.CustomField;
import com.vaadin.v7.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Base class for implementations of custom fields allowing for editing an {@link I18nString}, 
 * i.e. a string in several languages.
 * By default the default locale is only shown, but a special button can be clicked to show text fields
 * to enter translations.
 * <p>
 * Extensions can define what component is used for editing values, {@link TextField} or {@link TextArea}.
 * @author K. Benedyczak
 */
@Deprecated
public abstract class Abstract18nField<T extends AbstractTextField> extends CustomField<I18nString>
{
	private UnityMessageSource msg;
	private String defaultLocaleCode;
	private String defaultLocaleName;
	private Map<String, Locale> enabledLocales;
	private T defaultTf;
	private Map<String, T> translationTFs = new HashMap<>();
	private Map<String, String> notShownTranslations = new HashMap<>();
	private String preservedDef;
	private boolean shown = false;
	private Component main;
	private HorizontalLayout hl;
	
	public Abstract18nField(UnityMessageSource msg)
	{
		this.enabledLocales = new HashMap<>(msg.getEnabledLocales());
		this.defaultLocaleCode = msg.getDefaultLocaleCode();
		this.msg = msg;
		for (Map.Entry<String, Locale> locE: enabledLocales.entrySet())
			if (defaultLocaleCode.equals(locE.getValue().toString()))
				defaultLocaleName = locE.getKey();
	}

	public Abstract18nField(UnityMessageSource msg, String caption)
	{
		this(msg);
		setCaption(caption);
	}
	
	protected abstract T makeFieldInstance();
	
	protected void initUI()
	{
		defaultTf = makeFieldInstance();
		defaultTf.setDescription(defaultLocaleName);
		String defStyle = Styles.getFlagBgStyleForLocale(defaultLocaleCode);
		if (defStyle != null)
			defaultTf.addStyleName(defStyle);
		final Button showAll = new Button(Images.zoomin.getResource());
		showAll.addStyleName(Styles.vButtonLink.toString());
		showAll.setDescription(msg.getMessage("I18TextField.showLanguages"));
		showAll.addClickListener((event) -> 
		{
				show();
				if (shown)
				{
					showAll.setIcon(Images.zoomout.getResource());
					showAll.setDescription(msg.getMessage("I18TextField.hideLanguages"));
				}
				else
				{
					showAll.setIcon(Images.zoomin.getResource());
					showAll.setDescription(msg.getMessage("I18TextField.showLanguages"));
				}
		});

		hl = new HorizontalLayout();
		hl.addComponents(defaultTf, showAll);
		hl.setComponentAlignment(showAll, Alignment.MIDDLE_CENTER);
		hl.setSpacing(true);
		hl.setMargin(false);
		hl.setExpandRatio(defaultTf, 1.0f);
		hl.setWidth(defaultTf.getWidth(), defaultTf.getWidthUnits());
		hl.addStyleName(Styles.smallSpacing.toString());
		
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(false);
		main.addStyleName(Styles.smallSpacing.toString());
		main.addComponent(hl);

		for (Map.Entry<String, Locale> locE: enabledLocales.entrySet())
		{
			String localeKey = locE.getValue().toString();
			if (defaultLocaleCode.equals(localeKey))
				continue;
			
			T tf = makeFieldInstance();
			tf.setDescription(locE.getKey());
			String style = Styles.getFlagBgStyleForLocale(localeKey);
			if (style != null)
				tf.addStyleName(style);
			translationTFs.put(locE.getValue().toString(), tf);
			main.addComponent(tf);
			tf.setVisible(false);
		}
		this.main = main;
	}

	
	
	@Override
	protected Component initContent()
	{
		return main;
	}

	private void show()
	{
		shown = !shown;
		for (T tf: translationTFs.values())
		{
			tf.setVisible(shown);
		}
	}
	
	@Override
	public void setDescription(String description)
	{
		defaultTf.setDescription(description + "<br>" + defaultLocaleName);
	}
	
	@Override
	protected void setInternalValue(I18nString value)
	{
		super.setInternalValue(value);
		if (value == null)
			return;
		for (Map.Entry<String, String> vE: value.getMap().entrySet())
		{
			if (vE.getKey().equals(defaultLocaleCode))
				defaultTf.setValue(vE.getValue());
			else
			{
				T tf = translationTFs.get(vE.getKey());
				if (tf != null)
					tf.setValue(vE.getValue());
				else
					notShownTranslations.put(vE.getKey(), vE.getValue());
			}
		}
		preservedDef = value.getDefaultValue();
	}
	
	@Override
	protected I18nString getInternalValue()
	{
		I18nString ret = new I18nString();
		if (defaultTf.getValue() != null && !defaultTf.getValue().equals(""))
			ret.addValue(defaultLocaleCode, defaultTf.getValue());
		for (Map.Entry<String, T> tfE: translationTFs.entrySet())
		{
			T tf = tfE.getValue();
			if (tf.getValue() != null && !tf.getValue().equals(""))
				ret.addValue(tfE.getKey(), tf.getValue());
		}
		for (Map.Entry<String, String> hiddenE: notShownTranslations.entrySet())
			ret.addValue(hiddenE.getKey(), hiddenE.getValue());
		ret.setDefaultValue(preservedDef);
		return ret;
	}
	
	@Override
	public Class<? extends I18nString> getType()
	{
		return I18nString.class;
	}
	
	public void addFocusListener(FocusListener listener)
	{
		for (T tf: translationTFs.values())
		{
			tf.addFocusListener(listener);
		}
		defaultTf.addFocusListener(listener);
	}
	
	@Override
	public void setWidth(float width, Unit unit)
	{
		super.setWidth(width, unit);
		if (translationTFs != null)
		{
			hl.setWidth(width, unit);
			defaultTf.setWidth(width, unit);
			for (T tf: translationTFs.values())
			{
				tf.setWidth(width, unit);
			}
		}
	}
}
