/*
e * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.uiproviders;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;

import com.vaadin.data.ValueProvider;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar2;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridContextMenuSupport;
import pl.edu.icm.unity.webui.common.GridSelectionSupport;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler2;
import pl.edu.icm.unity.webui.common.SmallGrid;
import pl.edu.icm.unity.webui.common.Toolbar2;

/**
 * Allows for viewing and removing tokens 
 * @author P.Piernik
 *
 */

public class AdminTokensComponent extends VerticalLayout
{	
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			AdminTokensComponent.class);
	private AttributeSupport attrProcessor;
	protected SecuredTokensManagement tokenMan;
	private UnityMessageSource msg;
	
	protected VerticalLayout main;
	protected VerticalLayout tokensTablePanel;
	protected ComponentWithToolbar2 tableWithToolbar;
	protected Grid<TableTokensBean> tokensTable;
	private OAuthTokenViewer viewer;
	private boolean showViewer;
	
	public AdminTokensComponent(SecuredTokensManagement tokenMan, UnityMessageSource msg,
			AttributeSupport attrProcessor, boolean showViewer)
	{
		
		this.tokenMan = tokenMan;
		this.msg = msg;
		this.attrProcessor = attrProcessor;
		this.showViewer = showViewer;
		initUI();
	}
	
	private void initUI()
	{
		setCaption(msg.getMessage("OAuthTokenAdminUI.tokensLabel"));
		setMargin(false);
		setSpacing(false);
		tokensTable = new SmallGrid<>();
		tokensTable.setSizeFull();
		tokensTable.setSelectionMode(SelectionMode.MULTI);
		
		addColumn("type", TableTokensBean::getType, false);
		addColumn("value", TableTokensBean::getValue, false);
		addColumn("owner", TableTokensBean::getOwner, false);
		addColumn("clientName", TableTokensBean::getClientName, false);
		addColumn("createTime", TableTokensBean::getCreateTime, false);
		addColumn("expires", TableTokensBean::getExpires, false);
		addColumn("scopes", TableTokensBean::getScopes, false);
		addColumn("serverId", TableTokensBean::getServerId, true);
		addColumn("refreshToken", TableTokensBean::getRefreshToken, false);
		addColumn("hasIdToken", t -> String.valueOf(t.getHasIdToken()), true);
		
		tokensTable.sort("type", SortDirection.ASCENDING);

		viewer = new OAuthTokenViewer(msg);
		viewer.setVisible(false);
		tokensTable.addSelectionListener(event ->
		{
			Collection<TableTokensBean> items = event.getAllSelectedItems();

			if (items.size() > 1 || items.isEmpty())

			{
				viewer.setInput(null, null);
				viewer.setVisible(false);
				return;

			}
			TableTokensBean item = items.iterator().next();
			viewer.setVisible(true);
			viewer.setInput(item.getOAuthToken(), item.getToken());
		});
		
		SingleActionHandler2<TableTokensBean> refreshAction = getRefreshAction();
		SingleActionHandler2<TableTokensBean> deleteAction = getDeleteAction();
		
		GridContextMenuSupport<TableTokensBean> contextMenu = 
				new GridContextMenuSupport<>(tokensTable);
		contextMenu.addActionHandler(refreshAction);
		contextMenu.addActionHandler(deleteAction);
		GridSelectionSupport.installClickListener(tokensTable);
		
		tokensTablePanel = new VerticalLayout();
		tokensTablePanel.setSpacing(false);
		tokensTablePanel.setMargin(false);
		tokensTablePanel.addComponent(tokensTable);
			
		Toolbar2<TableTokensBean> toolbar = new Toolbar2<>(Orientation.HORIZONTAL);
		tokensTable.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addActionHandlers(contextMenu.getActionHandlers());
			
		tableWithToolbar = new ComponentWithToolbar2(tokensTablePanel, toolbar);
		tableWithToolbar.setWidth(100, Unit.PERCENTAGE);
		
		
		main = new VerticalLayout();
		if (showViewer)
		{

			CompositeSplitPanel hl = new CompositeSplitPanel(false, true,
					tableWithToolbar, viewer, 60);
			hl.setSizeFull();
			main.addComponent(hl);

		} else
		{
			main.addComponent(tableWithToolbar);
		}
		main.setSpacing(false);
		main.setMargin(false);
		main.setSizeFull();
		refresh();
		
	}
	
	private void addColumn(String key, ValueProvider<TableTokensBean, String> valueProvider, 
			boolean hidden)
	{
		tokensTable.addColumn(valueProvider, t -> t)
			.setCaption(msg.getMessage("OAuthToken." + key))
			.setHidable(true).setId(key).setHidden(hidden);
	}
	
	private SingleActionHandler2<TableTokensBean> getRefreshAction()
	{
		return SingleActionHandler2.builder4Refresh(msg, TableTokensBean.class)
				.withHandler(selection -> refresh())
				.build();
	}
	
	private SingleActionHandler2<TableTokensBean> getDeleteAction()
	{
		return SingleActionHandler2.builder4Delete(msg, TableTokensBean.class)
				.withHandler(this::deleteHandler)
				.build();
	}

	private void deleteHandler(Collection<TableTokensBean> items)
	{
		new ConfirmDialog(msg, msg.getMessage("OAuthTokenAdminUI.confirmDelete"),
				() -> 
			{
				for (TableTokensBean item : items)
					removeToken(item.getRealType(), item.getValue());
			}
		).show();

	}
	
	protected boolean removeToken(String type, String value)
	{
		try
		{
			tokenMan.removeToken(type, value);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(
					msg,
					msg.getMessage("OAuthTokenAdminUI.errorRemove"),
					e);
			return false;
		}
	}

	
	protected List<Token> getTokens() throws EngineException
	{
		List<Token> tokens = new ArrayList<>();	
		tokens.addAll(tokenMan.getAllTokens(OAuthProcessor.INTERNAL_ACCESS_TOKEN));
		tokens.addAll(tokenMan.getAllTokens(OAuthProcessor.INTERNAL_REFRESH_TOKEN));
		return tokens;
	}
	
	protected void refresh()
	{
		try
		{
			List<Token> tokens = getTokens();
			System.out.println(tokens);
			tokensTable.setItems(tokens.stream()
				.map(t -> new TableTokensBean(t, msg, establishOwner(attrProcessor, t))));
			tokensTable.deselectAll();
			viewer.setInput(null, null);
			viewer.setVisible(false);
			removeAllComponents();
			addComponent(main);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("OAuthTokenAdminUI.errorGetTokens"), e);
		}
		
	}
	
	
	private static String establishOwner(AttributeSupport attrProcessor, Token token)
	{
		long ownerId = token.getOwner();
		String idLabel = "[" + ownerId + "]";
		String attrNameValue = getAttrNameValue(attrProcessor, ownerId);
		return attrNameValue != null ? idLabel + " " + attrNameValue : idLabel;
	}
	
	private static String getAttrNameValue(AttributeSupport attrProcessor, long owner)
	{
		try
		{
			AttributeExt attribute = attrProcessor.getAttributeByMetadata(
					new EntityParam(owner), "/", 
					EntityNameMetadataProvider.NAME);
			if (attribute != null && !attribute.getValues().isEmpty())
				return attribute.getValues().get(0);
		} catch (Exception e)
		{
			log.debug("Can't get user's displayed name attribute for " + owner, e);
		}
		return null;
	}
	
	public static class TableTokensBean
	{
		private Token token;
		private OAuthToken oauthToken;
		private UnityMessageSource msg;
		private String owner;
		
		public TableTokensBean(Token token, UnityMessageSource msg, String owner)
		{
			this.token = token;
			this.msg = msg;
			this.oauthToken = OAuthToken.getInstanceFromJson(token.getContents());
			this.owner = owner;
		}
	
		public String getType()
		{
			try
			{
				return msg.getMessage("OAuthTokenType." + token.getType());
			} catch (Exception e)
			{
				return token.getType();
			}
		}
		
		String getRealType()
		{
			return token.getType();
		}
		
		public String getCreateTime()
		{
			return new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT)
					.format(token.getCreated());
		}
		
		public String getExpires()
		{
			return new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT)
			.format(token.getExpires());
		}
		
		public String getValue()
		{
			return token.getValue();
		}
		
		public String getServerId()
		{
			return oauthToken.getIssuerUri();
		}
		
		public String getRefreshToken()
		{
			//show refresh token only for access token
			boolean isRefreshToken = token.getType().equals(OAuthProcessor.INTERNAL_REFRESH_TOKEN);
			return oauthToken.getRefreshToken() != null &&	!isRefreshToken ? 
					oauthToken.getRefreshToken() : "";
		}
		
		public boolean getHasIdToken()
		{
			return oauthToken.getOpenidInfo() != null;
		}
		
		public String getClientName()
		{
			return oauthToken.getClientName() != null
					&& !oauthToken.getClientName().isEmpty()
							? oauthToken.getClientName()
							: oauthToken.getClientUsername();
		}

		public String getScopes()
		{
			return Stream.of(oauthToken.getEffectiveScope()).collect(Collectors.joining(", "));
		}
		
		public String getOwner()
		{
			return owner;
		}		
		
		Token getToken()
		{
			return token;
		}
		
		OAuthToken getOAuthToken()
		{
			return oauthToken;
		}
	}
}
