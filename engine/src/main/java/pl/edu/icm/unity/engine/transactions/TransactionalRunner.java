/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.transactions;

import pl.edu.icm.unity.exceptions.EngineException;


public interface TransactionalRunner
{
	public void runInTransaciton(TxRunnable code) throws EngineException;
	
	public <T> T runInTransacitonRet(TxRunnableRet<T> code) throws EngineException;
	
	public interface TxRunnable
	{
		void run() throws EngineException;
	}

	public interface TxRunnableRet<T>
	{
		T run() throws EngineException;
	}
}
