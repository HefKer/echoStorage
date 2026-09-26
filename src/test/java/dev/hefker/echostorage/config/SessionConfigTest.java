package dev.hefker.echostorage.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionConfigTest {
	private static final EchoConfig LOCAL = new EchoConfig(false, true, false, true, false, true);
	private static final EchoConfig SERVER_CONFIG = new EchoConfig(true, false, true, false, true, false);

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
	void theServerConfigAppliesForTheSession() {
		SessionConfig.applyRemote(SERVER_CONFIG);

		assertEquals(SERVER_CONFIG, EchoConfig.get());
	}

	@Test
	void leavingTheServerRestoresTheLocalConfig() {
		SessionConfig.applyRemote(SERVER_CONFIG);
		SessionConfig.restoreLocal();

		assertEquals(LOCAL, EchoConfig.get());
	}

	@Test
	void restoringWithNothingAppliedKeepsTheLocalConfig() {
		SessionConfig.restoreLocal();

		assertEquals(LOCAL, EchoConfig.get());
	}

	@Test
	void aSecondServerConfigStillRestoresTheLocalOne() {
		SessionConfig.applyRemote(SERVER_CONFIG);
		SessionConfig.applyRemote(EchoConfig.DEFAULTS);
		SessionConfig.restoreLocal();

		assertEquals(LOCAL, EchoConfig.get());
	}
}
