package dev.hefker.echostorage.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import dev.hefker.echostorage.EchoStorage;

/**
 * Reads {@link EchoConfig} from a TOML file, the format NeoForge's {@code ModConfigSpec} uses,
 * so a port could hand the same file to that system (ADR-0003 rule 6). Its keys and comments
 * come from {@link ConfigSwitch#ALL}.
 */
public final class EchoConfigFile {
	private EchoConfigFile() {
	}

	/**
	 * Reads the switches from {@code file}, first writing any it lacks back to it at their
	 * defaults. Never throws: a file that cannot be read or parsed is logged and left alone for
	 * the player to fix, and the defaults apply until it is.
	 */
	public static EchoConfig load(Path file) {
		CommentedConfig toml = TomlFormat.newConfig(LinkedHashMap::new);
		try {
			if (Files.exists(file)) {
				TomlFormat.instance().createParser().parse(Files.readString(file), toml, ParsingMode.REPLACE);
			}
		} catch (IOException | ParsingException e) {
			EchoStorage.LOGGER.error("Could not read {}; using the default settings until it is fixed", file, e);
			return EchoConfig.DEFAULTS;
		}
		if (addMissing(toml)) {
			try {
				Files.createDirectories(file.getParent());
				Files.writeString(file, TomlFormat.instance().createWriter().writeToString(toml));
			} catch (IOException e) {
				EchoStorage.LOGGER.warn("Could not write the missing settings to {}", file, e);
			}
		}
		List<Boolean> values = new ArrayList<>(ConfigSwitch.ALL.size());
		for (ConfigSwitch option : ConfigSwitch.ALL) {
			values.add(value(toml, option));
		}
		return ConfigSwitch.build(values);
	}

	/** Writes each switch the file lacks at its default, with its comment. True if any were. */
	private static boolean addMissing(CommentedConfig toml) {
		boolean added = false;
		for (ConfigSwitch option : ConfigSwitch.ALL) {
			if (!toml.contains(option.path())) {
				toml.set(option.path(), option.fallback());
				toml.setComment(option.path(), option.comment());
				added = true;
			}
		}
		return added;
	}

	private static boolean value(CommentedConfig toml, ConfigSwitch option) {
		Object value = toml.get(option.path());
		if (value instanceof Boolean on) {
			return on;
		}
		EchoStorage.LOGGER.warn("{} should be true or false, not {}; using {}", option.path(), value, option.fallback());
		return option.fallback();
	}
}
