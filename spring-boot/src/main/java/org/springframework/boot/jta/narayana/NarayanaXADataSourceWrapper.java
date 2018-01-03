/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.jta.narayana;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.managed.DataSourceXAConnectionFactory;
import org.apache.commons.dbcp2.managed.ManagedDataSource;
import org.apache.commons.pool2.impl.GenericObjectPool;

import org.springframework.boot.jta.XADataSourceWrapper;
import org.springframework.util.Assert;

/**
 * {@link XADataSourceWrapper} that uses {@link ManagedDataSource} to wrap an
 * {@link XADataSource}.
 *
 * @author Gytis Trikleris
 * @since 1.4.0
 */
public class NarayanaXADataSourceWrapper implements XADataSourceWrapper {

	private final TransactionManager transactionManager;

	private final NarayanaRecoveryManagerBean recoveryManager;

	private final NarayanaProperties properties;

	/**
	 * Create a new {@link NarayanaXADataSourceWrapper} instance.
	 *
	 * @param transactionManager the transaction manager
	 * @param recoveryManager    the underlying recovery manager
	 * @param properties         the Narayana properties
	 */
	public NarayanaXADataSourceWrapper(TransactionManager transactionManager,
			NarayanaRecoveryManagerBean recoveryManager, NarayanaProperties properties) {
		Assert.notNull(transactionManager, "TransactionManager must not be null");
		Assert.notNull(recoveryManager, "RecoveryManager must not be null");
		Assert.notNull(properties, "Properties must not be null");
		this.transactionManager = transactionManager;
		this.recoveryManager = recoveryManager;
		this.properties = properties;
	}

	@Override
	public DataSource wrapDataSource(XADataSource dataSource) {
		XAResourceRecoveryHelper recoveryHelper = getRecoveryHelper(dataSource);
		this.recoveryManager.registerXAResourceRecoveryHelper(recoveryHelper);

		DataSourceXAConnectionFactory dataSourceXAConnectionFactory =
				new DataSourceXAConnectionFactory(this.transactionManager, dataSource);
		PoolableConnectionFactory poolableConnectionFactory =
				new PoolableConnectionFactory(dataSourceXAConnectionFactory, null);
		GenericObjectPool<PoolableConnection> connectionPool =
				new GenericObjectPool<PoolableConnection>(poolableConnectionFactory);
		return new ManagedDataSource<PoolableConnection>(connectionPool,
				dataSourceXAConnectionFactory.getTransactionRegistry());
	}

	private XAResourceRecoveryHelper getRecoveryHelper(XADataSource dataSource) {
		if (this.properties.getRecoveryDbUser() == null
				&& this.properties.getRecoveryDbPass() == null) {
			return new DataSourceXAResourceRecoveryHelper(dataSource);
		}
		return new DataSourceXAResourceRecoveryHelper(dataSource,
				this.properties.getRecoveryDbUser(), this.properties.getRecoveryDbPass());
	}

}
