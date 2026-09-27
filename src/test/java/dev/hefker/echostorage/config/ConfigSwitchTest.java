package dev.hefker.echostorage.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ConfigSwitchTest {
	@Test
	void theDefaultsReadOutAndBuildBackUnchanged() {
		assertEquals(EchoConfig.DEFAULTS, ConfigSwitch.build(option -> option.valueIn(EchoConfig.DEFAULTS)));
	}

	@Test
	void eachValueLandsOnItsOwnSwitch() {
		for (ConfigSwitch on : ConfigSwitch.ALL) {
			EchoConfig config = ConfigSwitch.build(option -> option == on);

			for (ConfigSwitch option : ConfigSwitch.ALL) {
				assertEquals(option == on, option.valueIn(config), option.path() + " with only " + on.path() + " on");
			}
		}
	}

	@Test
	void valuesAreAskedForInListOrder() {
		List<ConfigSwitch> asked = new ArrayList<>();
		ConfigSwitch.build(option -> {
			asked.add(option);
			return false;
		});

		assertEquals(ConfigSwitch.ALL, asked);
	}
}
