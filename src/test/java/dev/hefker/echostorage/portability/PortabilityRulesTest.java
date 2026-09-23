package dev.hefker.echostorage.portability;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * ADR-0003 buys NeoForge portability as rules rather than intentions. The rules that say
 * "loader API X appears in exactly one place" are checkable, so they are checked here: a
 * stray import is caught by the build instead of by a port two years from now.
 *
 * <p>The check is an allow-list, not a deny-list. Every {@code net.fabricmc} import in the
 * tree must be named below, so a Fabric API nobody has thought about yet fails the build on
 * first use rather than quietly spreading.
 */
class PortabilityRulesTest {
	private static final List<Path> SOURCE_ROOTS =
			List.of(Path.of("src/main/java"), Path.of("src/client/java"));

	private static final Pattern FABRIC_IMPORT =
			Pattern.compile("^import (?:static )?(net\\.fabricmc\\.[\\w.]+);", Pattern.MULTILINE);

	/**
	 * Loader API prefix -> the packages allowed to import it. The longest matching prefix
	 * decides, so one API can have a different owner from the rest of its namespace. Package
	 * matching is exact: a sub-package does not inherit its parent's permission.
	 */
	private static final Map<String, List<String>> CONFINED_IMPORTS = Map.of(
			"net.fabricmc.api", List.of("dev.hefker.echostorage", "dev.hefker.echostorage.client"),
			"net.fabricmc.fabric.api.networking", List.of("dev.hefker.echostorage.network"),
			"net.fabricmc.fabric.api.client.networking", List.of("dev.hefker.echostorage.network"),
			"net.fabricmc.fabric.api.screenhandler", List.of("dev.hefker.echostorage.menu"),
			"net.fabricmc.fabric.api.command", List.of("dev.hefker.echostorage.command"),
			"net.fabricmc.fabric.api.itemgroup", List.of("dev.hefker.echostorage.item"),
			"net.fabricmc.fabric.api.event.lifecycle", List.of("dev.hefker.echostorage.item"),
			"net.fabricmc.fabric.api.client.rendering", List.of("dev.hefker.echostorage.client.tooltip"),
			"net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry",
					List.of("dev.hefker.echostorage.client.render"),
			"net.fabricmc.loader.api", List.of("dev.hefker.echostorage.platform.fabric"));

	private static final Path RESOURCE_ROOT = Path.of("src/main/resources");

	private static final Pattern FABRIC_RESOURCE_KEY = Pattern.compile("\"fabric:[\\w/]+\"");

	/**
	 * Data files allowed to use a Fabric-only JSON key (a custom ingredient, a load condition).
	 * These do not show up as imports, so they are listed here instead; each one needs a
	 * NeoForge equivalent on a port.
	 */
	private static final List<String> LOADER_RESOURCES = List.of(
			// fabric:components ingredient; NeoForge has neoforge:components.
			"data/echostorage/recipe/echo_bundle.json");

	/** Fabric sugar that rule 5 says to skip entirely in favour of the vanilla equivalent. */
	private static final List<String> FORBIDDEN_IMPORTS = List.of(
			"net.fabricmc.fabric.api.item.v1.FabricItemSettings",
			"net.fabricmc.fabric.api.transfer");

	static Stream<Path> sourceFiles() {
		return SOURCE_ROOTS.stream().flatMap(root -> {
			try (Stream<Path> tree = Files.walk(root)) {
				return tree.filter(path -> path.toString().endsWith(".java")).toList().stream();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	@Test
	void theSourceTreeIsWhereWeThinkItIs() {
		assertTrue(sourceFiles().count() > 0, "found no java sources to check");
	}

	@ParameterizedTest
	@MethodSource("sourceFiles")
	void everyLoaderImportIsOneWeHaveConfined(Path file) throws IOException {
		String source = Files.readString(file);
		String pkg = packageOf(source);
		Matcher imports = FABRIC_IMPORT.matcher(source);

		while (imports.find()) {
			String imported = imports.group(1);
			String rule = CONFINED_IMPORTS.keySet().stream()
					.filter(prefix -> imported.equals(prefix) || imported.startsWith(prefix + "."))
					.max(Comparator.comparingInt(String::length))
					.orElseThrow(() -> new AssertionError(file + " imports " + imported
							+ ", a loader API no portability rule covers. Confine it to one package"
							+ " and add it to CONFINED_IMPORTS, or use the vanilla equivalent."));

			if (!CONFINED_IMPORTS.get(rule).contains(pkg)) {
				throw new AssertionError(file + " (package " + pkg + ") imports " + imported
						+ ", which belongs only in " + CONFINED_IMPORTS.get(rule));
			}
		}
	}

	@ParameterizedTest
	@MethodSource("sourceFiles")
	void fabricSugarIsNotUsedWhereVanillaWouldDo(Path file) throws IOException {
		String source = Files.readString(file);

		for (String forbidden : FORBIDDEN_IMPORTS) {
			assertTrue(!source.contains("import " + forbidden),
					file + " imports " + forbidden + "; ADR-0003 rule 5 wants the vanilla equivalent");
		}
	}

	static Stream<Path> resourceFiles() {
		try (Stream<Path> tree = Files.walk(RESOURCE_ROOT)) {
			return tree.filter(path -> path.toString().endsWith(".json")).toList().stream();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@ParameterizedTest
	@MethodSource("resourceFiles")
	void everyLoaderKeyInDataIsOneWeHaveListed(Path file) throws IOException {
		String relative = RESOURCE_ROOT.relativize(file).toString().replace('\\', '/');
		if (relative.equals("fabric.mod.json") || LOADER_RESOURCES.contains(relative)) {
			return;
		}
		Matcher key = FABRIC_RESOURCE_KEY.matcher(Files.readString(file));
		if (key.find()) {
			throw new AssertionError(file + " uses " + key.group() + ", a Fabric-only data key."
					+ " Add the file to LOADER_RESOURCES with its NeoForge equivalent.");
		}
	}

	private static String packageOf(String source) {
		return source.lines()
				.filter(line -> line.startsWith("package "))
				.map(line -> line.substring("package ".length()).replace(";", "").trim())
				.findFirst()
				.orElseThrow(() -> new AssertionError("source file has no package declaration"));
	}
}
