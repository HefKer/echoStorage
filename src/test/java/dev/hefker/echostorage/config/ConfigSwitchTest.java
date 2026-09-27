package dev.hefker.echostorage.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class ConfigSwitchTest {
	@Test
	void theDefaultsReadOutAndBuildBackUnchanged() {
		assertEquals(EchoConfig.DEFAULTS, ConfigSwitch.build(ConfigSwitch.values(EchoConfig.DEFAULTS)));
	}

	@Test
	void eachValueLandsOnItsOwnSwitch() {
		for (int on = 0; on < ConfigSwitch.ALL.size(); on++) {
			List<Boolean> values = new ArrayList<>(Collections.nCopies(ConfigSwitch.ALL.size(), false));
			values.set(on, true);

			EchoConfig config = ConfigSwitch.build(values);

			for (int i = 0; i < ConfigSwitch.ALL.size(); i++) {
				ConfigSwitch option = ConfigSwitch.ALL.get(i);
				assertEquals(i == on, option.in(config), option.path() + " with only " + ConfigSwitch.ALL.get(on).path() + " on");
			}
		}
	}

	@Test
	void tooFewOrTooManyValuesAreRefused() {
		List<Boolean> values = ConfigSwitch.values(EchoConfig.DEFAULTS);
		List<Boolean> tooMany = new ArrayList<>(values);
		tooMany.add(true);

		assertThrows(IllegalArgumentException.class, () -> ConfigSwitch.build(values.subList(1, values.size())));
		assertThrows(IllegalArgumentException.class, () -> ConfigSwitch.build(tooMany));
	}
}
