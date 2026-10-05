package dev.hefker.echostorage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.hefker.echostorage.config.ConfigSwitch;
import org.junit.jupiter.api.Test;

/** Player-facing text says Link in plain words, since nothing in the game is named Link. */
class PlayerTextTest {
	@Test
	void noLanguageStringSaysLink() throws IOException {
		try (InputStream in = PlayerTextTest.class.getResourceAsStream("/assets/echostorage/lang/en_us.json")) {
			assertNotNull(in, "en_us.json is missing");
			for (Map.Entry<String, JsonElement> entry : JsonParser
					.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().entrySet()) {
				assertFalse(saysLink(entry.getValue().getAsString()), entry.getKey());
			}
		}
	}

	@Test
	void noConfigCommentSaysLink() {
		for (ConfigSwitch option : ConfigSwitch.ALL) {
			assertFalse(saysLink(option.comment()), option.path());
		}
	}

	private static boolean saysLink(String text) {
		return text.toLowerCase(Locale.ROOT).contains("link");
	}
}
