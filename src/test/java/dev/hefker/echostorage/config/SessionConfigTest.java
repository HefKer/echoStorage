package dev.hefker.echostorage.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionConfigTest {
	private static final EchoConfig LOCAL = new EchoConfig(false, true, false, true, false, true);
	private static final EchoConfig SERVERS = new EchoConfig(true, false, true, false, true, false);

	@BeforeEach
	void loadLocal() {
		EchoConfig.set(LOCAL);
	}

	@AfterEach
	void reset() {
		SessionConfig.restoreLocal();
		EchoConfig.set(EchoConfig.DEFAULTS);
	}

	@Test
	void theServersConfigAppliesForTheSession() {
		SessionConfig.applyRemote(SERVERS);

		assertEquals(SERVERS, EchoConfig.get());
	}

	@Test
	void leavingTheServerRestoresTheLocalConfig() {
		SessionConfig.applyRemote(SERVERS);
		SessionConfig.restoreLocal();

		assertEquals(LOCAL, EchoConfig.get());
	}

	@Test
	void restoringWithNothingAppliedKeepsTheLocalConfig() {
		SessionConfig.restoreLocal();

		assertEquals(LOCAL, EchoConfig.get());
	}

	@Test
	void aSecondServersConfigStillRestoresTheLocalOne() {
		SessionConfig.applyRemote(SERVERS);
		SessionConfig.applyRemote(EchoConfig.DEFAULTS);
		SessionConfig.restoreLocal();

		assertEquals(LOCAL, EchoConfig.get());
	}
}
