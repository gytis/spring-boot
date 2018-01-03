/*
 * Copyright 2012-2018 the original author or authors.
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

import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.managed.DataSourceXAConnectionFactory;
import org.apache.commons.dbcp2.managed.ManagedDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;

/**
 * A class to wrap an {@link XADataSource} with a pooled {@link ManagedDataSource} from DBCP2.
 *
 * @author Gytis Trikleris
 */
public class DbcpXaDataSourceWrapper {

	private final TransactionManager transactionManager;

	private final NarayanaProperties narayanaProperties;

	public DbcpXaDataSourceWrapper(TransactionManager transactionManager, NarayanaProperties narayanaProperties) {
		this.transactionManager = transactionManager;
		this.narayanaProperties = narayanaProperties;
	}

	public DataSource wrapDataSource(XADataSource xaDataSource) {
		DataSourceXAConnectionFactory xaConnectionFactory =
				new DataSourceXAConnectionFactory(this.transactionManager, xaDataSource);
		ObjectPool<PoolableConnection> objectPool = createObjectPool(xaConnectionFactory);
		return new ManagedDataSource<PoolableConnection>(objectPool, xaConnectionFactory.getTransactionRegistry());
	}

	private ObjectPool<PoolableConnection> createObjectPool(DataSourceXAConnectionFactory xaConnectionFactory) {
		PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(xaConnectionFactory, null);
		GenericObjectPool<PoolableConnection> objectPool =
				new GenericObjectPool<PoolableConnection>(poolableConnectionFactory, getObjectPoolConfig());
		poolableConnectionFactory.setPool(objectPool);
		return objectPool;
	}

	private GenericObjectPoolConfig getObjectPoolConfig() {
		GenericObjectPoolConfig objectPoolConfig = new GenericObjectPoolConfig();
		MutablePropertyValues properties = new MutablePropertyValues(this.narayanaProperties.getPool());
		new RelaxedDataBinder(objectPoolConfig).bind(properties);
		return objectPoolConfig;
	}

}
