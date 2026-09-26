package dev.hefker.echostorage.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import dev.hefker.echostorage.EchoStorage;

/**
 * Reads {@link EchoConfig} from a TOML file, the format NeoForge's {@code ModConfigSpec} uses,
 * so a port could hand the same file to that system (ADR-0003 rule 6).
 */
public final class EchoConfigFile {
	private record Switch(String path, String comment, Predicate<EchoConfig> read) {
		boolean fallback() {
			return read.test(EchoConfig.DEFAULTS);
		}
	}

	private static final Switch SEARCH_IN_OPEN_CONTAINER = new Switch("search.in_open_container",
			" Offer a search box on an open container's screen.",
			EchoConfig::searchInOpenContainer);
	private static final Switch SEARCH_BY_CHEST_NAME = new Switch("search.by_chest_name",
			" Let an Echo Interface search its chests by name.",
			EchoConfig::searchByChestName);
	private static final Switch BUNDLE_VACUUM = new Switch("bundle.vacuum",
			" Let an Echo Bundle vacuum up items on pickup. Each bundle still starts with it off.",
			EchoConfig::bundleVacuum);
	private static final Switch BUNDLE_REFILL = new Switch("bundle.refill",
			" Placing the last block in hand pulls the next one from an Echo Bundle.",
			EchoConfig::bundleRefill);
	private static final Switch BUNDLE_PLACE = new Switch("bundle.place",
			" Using an Echo Bundle on a block places a block out of it.",
			EchoConfig::bundlePlace);

	private static final List<Switch> SWITCHES = List.of(SEARCH_IN_OPEN_CONTAINER, SEARCH_BY_CHEST_NAME,
			BUNDLE_VACUUM, BUNDLE_REFILL, BUNDLE_PLACE);

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
		return new EchoConfig(
				value(toml, SEARCH_IN_OPEN_CONTAINER),
				value(toml, SEARCH_BY_CHEST_NAME),
				value(toml, BUNDLE_VACUUM),
				value(toml, BUNDLE_REFILL),
				value(toml, BUNDLE_PLACE));
	}

	/** Writes each switch the file lacks at its default, with its comment. True if any were. */
	private static boolean addMissing(CommentedConfig toml) {
		boolean added = false;
		for (Switch option : SWITCHES) {
			if (!toml.contains(option.path())) {
				toml.set(option.path(), option.fallback());
				toml.setComment(option.path(), option.comment());
				added = true;
			}
		}
		return added;
	}

	private static boolean value(CommentedConfig toml, Switch option) {
		Object value = toml.get(option.path());
		if (value instanceof Boolean on) {
			return on;
		}
		EchoStorage.LOGGER.warn("{} should be true or false, not {}; using {}", option.path(), value, option.fallback());
		return option.fallback();
	}
}
