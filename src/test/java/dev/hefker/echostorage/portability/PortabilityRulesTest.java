package dev.hefker.echostorage.portability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * ADR-0003 buys NeoForge portability as rules rather than intentions. The rules that say
 * "loader API X appears in exactly one place" are checkable, so they are checked here: a
 * stray import is caught by the build instead of by a port two years from now.
 */
class PortabilityRulesTest {
	private static final List<Path> SOURCE_ROOTS =
			List.of(Path.of("src/main/java"), Path.of("src/client/java"));

	/** Loader package prefix -> the one package prefix allowed to import it. */
	private static final Map<String, String> CONFINED_IMPORTS = Map.of(
			"net.fabricmc.fabric.api.networking", "dev.hefker.echostorage.network",
			"net.fabricmc.fabric.api.client.networking", "dev.hefker.echostorage.network",
			"net.fabricmc.fabric.api.screenhandler", "dev.hefker.echostorage.menu",
			"net.fabricmc.loader.api", "dev.hefker.echostorage.platform.fabric");

	/** Fabric sugar that rule 5 says to skip entirely in favour of the vanilla equivalent. */
	private static final List<String> FORBIDDEN_IMPORTS = List.of(
			"net.fabricmc.fabric.api.item.v1.FabricItemSettings",
			"net.fabricmc.fabric.api.transfer.v1.storage.Storage");

	static Stream<Path> sourceFiles() {
		return SOURCE_ROOTS.stream().flatMap(root -> {
			try {
				return Files.walk(root).filter(p -> p.toString().endsWith(".java")).toList().stream();
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
	void loaderApisStayInTheirOnePackage(Path file) throws IOException {
		String source = Files.readString(file);
		String pkg = packageOf(source);

		CONFINED_IMPORTS.forEach((loaderPackage, allowedPackage) -> {
			if (importsFrom(source, loaderPackage) && !pkg.startsWith(allowedPackage)) {
				throw new AssertionError(file + " (package " + pkg + ") imports " + loaderPackage
						+ ", which belongs only in " + allowedPackage);
			}
		});
	}

	@ParameterizedTest
	@MethodSource("sourceFiles")
	void fabricSugarIsNotUsedWhereVanillaWouldDo(Path file) throws IOException {
		String source = Files.readString(file);

		for (String forbidden : FORBIDDEN_IMPORTS) {
			assertTrue(!source.contains("import " + forbidden + ";"),
					file + " imports " + forbidden + "; ADR-0003 rule 5 wants the vanilla equivalent");
		}
	}

	private static boolean importsFrom(String source, String packagePrefix) {
		return source.contains("import " + packagePrefix + ".");
	}

	private static String packageOf(String source) {
		return source.lines()
				.filter(line -> line.startsWith("package "))
				.map(line -> line.substring("package ".length()).replace(";", "").trim())
				.findFirst()
				.orElseThrow(() -> new AssertionError("source file has no package declaration"));
	}
}
