/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Mar 17, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.store.rdbms;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.log4j.Logger;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.atomikos.jdbc.AtomikosDataSourceBean;

import eu.unicore.util.db.DBPropertiesHelper;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.InternalException;


/**
 * Provides initial MyBatis initialization and support for obtaining MyBatis SqlSessions.
 * 
 * @author K. Benedyczak
 */
@Component
public class DBSessionManager implements SessionManager
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, DBSessionManager.class);

	public static final String DEF_MAPCONFIG_LOCATION = "pl/edu/icm/unity/store/rdbms/mapper/mapconfig.xml";

	public static final int SESSION_KEEP_WARN_TIME = 3000;
	
	private SqlSessionFactory sqlMapFactory;
	private Map<SqlSession, Holder> used = new HashMap<SqlSession, Holder>();

	@Autowired
	public DBSessionManager(DBConfiguration config, AtomikosDataSourceBean dsBean) 
			throws InternalException, IOException
	{
		sqlMapFactory = loadMybatis(config, dsBean);
	}
	
	private SqlSessionFactory loadMybatis(DBConfiguration config, AtomikosDataSourceBean dsBean) throws IOException
	{
		String mapFile = config.getFileValueAsString(DBConfiguration.DBCONFIG_FILE, false);
		SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
		Reader reader;
		if (mapFile != null)
			reader = new FileReader(mapFile);
		else
			reader = Resources.getResourceAsReader(DEF_MAPCONFIG_LOCATION);
		Properties properties = new Properties();
		properties.setProperty(DBPropertiesHelper.DIALECT,
				config.getValue(DBPropertiesHelper.DIALECT));
		
		SqlSessionFactory fromXML = builder.build(reader, properties);
		
		TransactionFactory txFactory = new SpringManagedTransactionFactory();
		Environment env = new Environment("def", txFactory, dsBean);
		fromXML.getConfiguration().setEnvironment(env);
		return fromXML;
	}

	private static class Holder
	{
		private long timestamp;
		private long reportDelay = 1000;
		private long lastReport = -1000;
		private StackTraceElement[] stackTrace;
		private String threadName;
		
		public Holder(long timestamp, StackTraceElement[] stackTrace, 
				String threadName)
		{
			this.timestamp = timestamp;
			this.stackTrace = stackTrace;
			this.threadName = threadName;
		}
	}
	
	@Override
	public Configuration getMyBatisConfiguration()
	{
		return sqlMapFactory.getConfiguration();
	}
	
	@Override
	public SqlSession getSqlSession(boolean transactional)
	{
		return getSqlSession(ExecutorType.SIMPLE, transactional);
	}

	@Override
	public SqlSession getSqlSession(ExecutorType executor, boolean transactional)
	{
		runSessionWatchdog();
		SqlSession newSession = sqlMapFactory.openSession(executor, !transactional);
		synchronized(this)
		{
			Holder h = new Holder(System.currentTimeMillis(), 
				Thread.currentThread().getStackTrace(), 
				Thread.currentThread().getName());
			Holder p = used.put(newSession, h);
			if (p != null)
				log.warn("Ups! MyBatis returned a SqlSession which is already used!");
		}
		return newSession;
	}
	
	private synchronized void runSessionWatchdog()
	{
		long now = System.currentTimeMillis();
		Iterator<Entry<SqlSession, Holder>> it = used.entrySet().iterator();
		
		while(it.hasNext())
		{
			Holder e = it.next().getValue();
			long wait = now - e.timestamp;
			long sinceLastLog = now - e.lastReport;
			if (wait > SESSION_KEEP_WARN_TIME && sinceLastLog > e.reportDelay)
			{
				e.lastReport = now;
				e.reportDelay *= 2;
				log.warn("SqlSession is kept for more than " +
						SESSION_KEEP_WARN_TIME/1000 +
						"s: " + wait/1000.0 + "s by " +
						e.threadName + ". Next report in at least " + 
						e.reportDelay + "ms. Stacktrace is:\n" +
						produceStackTrace(e.stackTrace));
			}
		}
	}
	
	private String produceStackTrace(StackTraceElement[] stackTrace)
	{
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement se: stackTrace)
			sb.append("  ").append(se.toString()).append("\n");
		return sb.toString();
	}
	
	@Override
	public void releaseSqlSession(SqlSession session)
	{
		synchronized(this)
		{
			Holder p = used.remove(session);
			if (p == null)
			{
				log.warn("Thread trying to release not known session:\n"
					+ produceStackTrace(
						Thread.currentThread().getStackTrace()));
			}
		}
		session.close();
	}
}
