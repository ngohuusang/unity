/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.html.HtmlEscapers;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Component with a list of small buttons. Buttons are bound to actions via 
 * {@link SingleActionHandler}.
 * 
 * Additionally toolbar's buttons have their state enabled or disabled depending 
 * whether the toolbar's target is set or not.
 *  
 * @author K. Benedyczak
 */
public class Toolbar2<T> extends CustomComponent
{
	private Orientation orientation;
	private Set<T> target;
	private List<Button> buttons;
	private AbstractOrderedLayout main;
	
	public Toolbar2(Orientation orientation)
	{
		this.orientation = orientation;
		this.main = orientation == Orientation.HORIZONTAL ? 
				new HorizontalLayout() : new VerticalLayout();
		target = Collections.emptySet();
		buttons = new ArrayList<>();
		main.setSpacing(true);
		main.setMargin(false);
		main.addStyleName(Styles.tinySpacing.toString());
		setCompositionRoot(main);
		setSizeUndefined();
	}

	public Orientation getOrientation()
	{
		return orientation;
	}

	/**
	 * @return a listener that can be registered on a selectable component as {@link Tree} or {@link Table}
	 * to update the toolbar's target.
	 */
	public SelectionListener<T> getSelectionListener()
	{
		return event ->
		{
			target = event.getAllSelectedItems();
			for (Button button: buttons)
				updateButtonState(button);
		};
	}
	
	private void updateButtonState(Button button)
	{
		Object buttonData = button.getData();
		if (buttonData == null || !(buttonData instanceof SingleActionHandler2))
			return;
		@SuppressWarnings("unchecked")
		SingleActionHandler2<T> handler = (SingleActionHandler2<T>) button.getData();
		if (handler.isVisible(target))
		{
			button.setVisible(true);
			button.setEnabled(handler.isEnabled(target));
		} else
		{
			button.setVisible(false);
		}
	}
	
	public void addActionHandlers(Collection<SingleActionHandler2<T>> handlers)
	{
		for (SingleActionHandler2<T> handler: handlers)
			addActionHandler(handler);
	}
	
	public void refresh()
	{
		target = new HashSet<>();
		for (Button button: buttons)
			updateButtonState(button);
	}
	
	public void addSeparator()
	{
		Label sep = new Label();
		String style = orientation == Orientation.HORIZONTAL ? Styles.verticalBar.toString() :
			Styles.horizontalBar.toString();
		sep.addStyleName(style);
		main.addComponent(sep);
		main.setComponentAlignment(sep, Alignment.MIDDLE_CENTER);
	}
	
	public void addActionHandler(SingleActionHandler2<T> handler)
	{
		final Button button = new Button();
		button.setData(handler);
		if (handler.getIcon() != null)
			button.setIcon(handler.getIcon());
		else if (handler.getCaption() != null)
			button.setCaption(handler.getCaption());
		
		if (handler.getCaption() != null)
			button.setDescription(HtmlEscapers.htmlEscaper().escape(handler.getCaption()));
		button.addStyleName(Styles.vButtonLink.toString());
		button.addStyleName(Styles.toolbarButton.toString());
		button.addClickListener(event ->
		{
			if (!handler.isEnabled(target))
				return;
			handler.handle(target);
		});
		buttons.add(button);
		main.addComponent(button);
		updateButtonState(button);
	}
}
