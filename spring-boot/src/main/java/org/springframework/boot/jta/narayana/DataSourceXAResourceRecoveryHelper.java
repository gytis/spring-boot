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

import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * XAResourceRecoveryHelper implementation which gets XIDs, which needs to be recovered,
 * from the database.
 *
 * @author Gytis Trikleris
 * @since 1.4.0
 */
public class DataSourceXAResourceRecoveryHelper
		implements XAResourceRecoveryHelper, XAResource {

	private static final XAResource[] NO_XA_RESOURCES = {};

	private static final Log logger = LogFactory
			.getLog(DataSourceXAResourceRecoveryHelper.class);

	private final ConnectionManager connectionManager;

	/**
	 * Create a new {@link DataSourceXAResourceRecoveryHelper} instance.
	 * @param xaDataSource the XA data source
	 */
	public DataSourceXAResourceRecoveryHelper(XADataSource xaDataSource) {
		this(xaDataSource, null, null);
	}

	/**
	 * Create a new {@link DataSourceXAResourceRecoveryHelper} instance.
	 * @param xaDataSource the XA data source
	 * @param user the database user or {@code null}
	 * @param password the database password or {@code null}
	 */
	public DataSourceXAResourceRecoveryHelper(XADataSource xaDataSource, String user,
			String password) {
		Assert.notNull(xaDataSource, "XADataSource must not be null");
		this.connectionManager = new ConnectionManager(xaDataSource, user, password);
	}

	@Override
	public boolean initialise(String properties) {
		return true;
	}

	@Override
	public XAResource[] getXAResources() {
		if (!this.connectionManager.isConnected()) {
			try {
				this.connectionManager.connect();
			}
			catch (XAException ignored) {
				return NO_XA_RESOURCES;
			}
		}

		return new XAResource[] { this };
	}

	@Override
	public Xid[] recover(final int flag) throws XAException {
		try {
			return this.connectionManager.connectAndApply(new XAResourceFunction<Xid[]>() {
				@Override
				public Xid[] apply(XAResource delegate) throws XAException {
					return delegate.recover(flag);
				}
			});
		}
		finally {
			if (flag == XAResource.TMENDRSCAN) {
				this.connectionManager.disconnect();
			}
		}
	}

	@Override
	public void start(final Xid xid, final int flags) throws XAException {
		this.connectionManager.connectAndAccept(new XAResourceConsumer() {
			@Override
			public void accept(XAResource delegate) throws XAException {
				delegate.start(xid, flags);
			}
		});
	}

	@Override
	public void end(final Xid xid, final int flags) throws XAException {
		this.connectionManager.connectAndAccept(new XAResourceConsumer() {
			@Override
			public void accept(XAResource delegate) throws XAException {
				delegate.end(xid, flags);
			}
		});
	}

	@Override
	public int prepare(final Xid xid) throws XAException {
		return this.connectionManager.connectAndApply(new XAResourceFunction<Integer>() {
			@Override
			public Integer apply(XAResource delegate) throws XAException {
				return delegate.prepare(xid);
			}
		});
	}

	@Override
	public void commit(final Xid xid, final boolean onePhase) throws XAException {
		this.connectionManager.connectAndAccept(new XAResourceConsumer() {
			@Override
			public void accept(XAResource delegate) throws XAException {
				delegate.commit(xid, onePhase);
			}
		});
	}

	@Override
	public void rollback(final Xid xid) throws XAException {
		this.connectionManager.connectAndAccept(new XAResourceConsumer() {
			@Override
			public void accept(XAResource delegate) throws XAException {
				delegate.rollback(xid);
			}
		});
	}

	@Override
	public boolean isSameRM(final XAResource xaResource) throws XAException {
		return this.connectionManager.connectAndApply(new XAResourceFunction<Boolean>() {
			@Override
			public Boolean apply(XAResource delegate) throws XAException {
				return delegate.isSameRM(xaResource);
			}
		});
	}

	@Override
	public void forget(final Xid xid) throws XAException {
		this.connectionManager.connectAndAccept(new XAResourceConsumer() {
			@Override
			public void accept(XAResource delegate) throws XAException {
				delegate.forget(xid);
			}
		});
	}

	@Override
	public int getTransactionTimeout() throws XAException {
		return this.connectionManager.connectAndApply(new XAResourceFunction<Integer>() {
			@Override
			public Integer apply(XAResource delegate) throws XAException {
				return delegate.getTransactionTimeout();
			}
		});
	}

	@Override
	public boolean setTransactionTimeout(final int seconds) throws XAException {
		return this.connectionManager.connectAndApply(new XAResourceFunction<Boolean>() {
			@Override
			public Boolean apply(XAResource delegate) throws XAException {
				return delegate.setTransactionTimeout(seconds);
			}
		});
	}

}
