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

import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;
import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.Assert;

/**
 * Bean to set up Narayana recovery manager.
 *
 * @author Gytis Trikleris
 * @since 1.4.0
 */
public class NarayanaRecoveryManagerBean implements DisposableBean {

	private final RecoveryManagerService recoveryManagerService;

	public NarayanaRecoveryManagerBean(RecoveryManagerService recoveryManagerService) {
		Assert.notNull(recoveryManagerService, "RecoveryManagerService must not be null");
		this.recoveryManagerService = recoveryManagerService;
	}

	@EventListener
	public void create(ApplicationReadyEvent ignored) {
		this.recoveryManagerService.create();
		this.recoveryManagerService.start();
	}

	@Override
	public void destroy() throws Exception {
		this.recoveryManagerService.stop();
		this.recoveryManagerService.destroy();
	}

	void registerXAResourceRecoveryHelper(
			XAResourceRecoveryHelper xaResourceRecoveryHelper) {
		getXARecoveryModule().addXAResourceRecoveryHelper(xaResourceRecoveryHelper);
	}

	private XARecoveryModule getXARecoveryModule() {
		XARecoveryModule xaRecoveryModule = XARecoveryModule.getRegisteredXARecoveryModule();
		if (xaRecoveryModule == null) {
			throw new IllegalStateException(
					"XARecoveryModule is not registered with recovery manager");
		}
		return xaRecoveryModule;
	}

}
