package dev.hefker.echostorage.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ServicesTest {
	@Test
	void platformHelperResolvesToTheLoaderImplementationOnTheClasspath() {
		assertNotNull(Services.PLATFORM);
		assertEquals("Fabric", Services.PLATFORM.platformName());
	}

	@Test
	void loadingAnUnregisteredServiceFailsLoudlyRatherThanReturningNull() {
		assertThrows(NullPointerException.class, () -> Services.load(Runnable.class));
	}
}
