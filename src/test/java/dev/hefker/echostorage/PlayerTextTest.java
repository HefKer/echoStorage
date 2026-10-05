package dev.hefker.echostorage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.hefker.echostorage.config.ConfigSwitch;
import org.junit.jupiter.api.Test;

/** Nothing in the game is named Link, so player-facing text never says Link or Linked. */
class PlayerTextTest {
	/** Link at a word's start in any casing, so "blink" is not caught. */
	private static final Pattern LINK = Pattern.compile("\\blink", Pattern.CASE_INSENSITIVE);

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
		return LINK.matcher(text).find();
	}
}
