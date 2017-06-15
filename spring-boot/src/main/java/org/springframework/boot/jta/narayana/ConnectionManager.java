/*
 * Copyright 2012-2017 the original author or authors.
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

import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO.
 *
 * @author Gytis Trikleris
 * @since 1.5.x TODO
 */
public class ConnectionManager {

	private static final Log logger = LogFactory.getLog(DataSourceXAResourceRecoveryHelper.class);

	private final XADataSource dataSource;

	private final String user;

	private final String password;

	private XAConnection connection;

	private XAResource resource;

	public ConnectionManager(XADataSource dataSource, String user, String password) {
		this.dataSource = dataSource;
		this.user = user;
		this.password = password;
	}

	public void connectAndAccept(XAResourceConsumer consumer) throws XAException {
		if (isConnected()) {
			consumer.accept(this.resource);
			return;
		}

		connect();
		try {
			consumer.accept(this.resource);
		}
		finally {
			disconnect();
		}
	}

	public <T> T connectAndApply(XAResourceFunction<T> function) throws XAException {
		if (isConnected()) {
			return function.apply(this.resource);
		}

		connect();
		try {
			return function.apply(this.resource);
		}
		finally {
			disconnect();
		}
	}

	public void connect() throws XAException {
		if (isConnected()) {
			return;
		}

		try {
			this.connection = getXaConnection();
			this.resource = this.connection.getXAResource();
		}
		catch (SQLException e) {
			if (this.connection != null) {
				try {
					this.connection.close();
				}
				catch (SQLException ignore) {
				}
			}
			logger.warn("Failed to create connection", e);
			throw new XAException(XAException.XAER_RMFAIL);
		}
	}

	public void disconnect() {
		if (!isConnected()) {
			return;
		}

		try {
			this.connection.close();
		}
		catch (SQLException e) {
			logger.warn("Failed to close connection", e);
		}
		finally {
			this.connection = null;
			this.resource = null;
		}
	}

	public boolean isConnected() {
		return this.connection != null && this.resource != null;
	}

	private XAConnection getXaConnection() throws SQLException {
		if (this.user == null && this.password == null) {
			return this.dataSource.getXAConnection();
		}
		return this.dataSource.getXAConnection(this.user, this.password);
	}

}
