package dev.hefker.echostorage.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EchoConfigFileTest {
	@TempDir
	Path dir;

	@Test
	void withNoFileTheDefaultsApplyAndACommentedFileIsWritten() throws IOException {
		Path file = dir.resolve("echostorage.toml");

		assertEquals(EchoConfig.DEFAULTS, EchoConfigFile.load(file));

		String written = Files.readString(file);
		assertTrue(written.contains("[vacuum]"), written);
		assertTrue(written.contains("enabled = true"), written);
		assertTrue(written.contains("#"), "the file should explain its switches:\n" + written);
	}

	@Test
	void aFreshFileHasChestQuickStackOnAndInterfaceQuickStackOff() throws IOException {
		Path file = dir.resolve("echostorage.toml");

		EchoConfigFile.load(file);

		String written = Files.readString(file);
		assertTrue(written.contains("[quick_stack]"), written);
		assertTrue(written.contains("chest = true"), written);
		assertTrue(written.contains("interface = false"), written);
	}

	@Test
	void theQuickStackSwitchesAreReadFromTheFile() throws IOException {
		Path file = write("""
				[quick_stack]
				chest = false
				interface = true
				""");

		EchoConfig config = EchoConfigFile.load(file);

		assertEquals(new EchoConfig(true, true, true, true, true, false, true), config);
	}

	@Test
	void aSwitchTurnedOffInTheFileIsOff() throws IOException {
		Path file = write("""
				[search]
				by_chest_name = false
				[vacuum]
				enabled = false
				""");

		EchoConfig config = EchoConfigFile.load(file);

		assertEquals(new EchoConfig(true, false, false, true, true, true, false), config);
	}

	@Test
	void switchesMissingFromTheFileAreAddedWithoutLosingThePlayersOwn() throws IOException {
		Path file = write("""
				[vacuum]
				# vacuum pulls in junk on our server
				enabled = false
				""");

		EchoConfigFile.load(file);

		String rewritten = Files.readString(file);
		assertTrue(rewritten.contains("vacuum pulls in junk on our server"), rewritten);
		assertTrue(rewritten.contains("enabled = false"), rewritten);
		assertTrue(rewritten.contains("refill = true"), rewritten);
		assertEquals(new EchoConfig(true, true, false, true, true, true, false), EchoConfigFile.load(file));
	}

	@Test
	void aFileThatIsNotTomlFallsBackToTheDefaultsAndIsLeftForThePlayerToFix() throws IOException {
		String broken = """
				[bundle
				vacuum = false
				""";
		Path file = write(broken);

		assertEquals(EchoConfig.DEFAULTS, EchoConfigFile.load(file));
		assertEquals(broken, Files.readString(file));
	}

	@Test
	void aSwitchThatIsNotTrueOrFalseKeepsItsDefault() throws IOException {
		Path file = write("""
				[search]
				in_open_container = "yes"
				[bundle]
				place = false
				""");

		assertEquals(new EchoConfig(true, true, true, true, false, true, false), EchoConfigFile.load(file));
	}

	@Test
	void aFileFromBeforeTheWirelessSwitchWasRemovedStillLoads() throws IOException {
		Path file = write("""
				[links]
				wireless = true
				[vacuum]
				enabled = false
				""");

		assertEquals(new EchoConfig(true, true, false, true, true, true, false), EchoConfigFile.load(file));
	}

	/** The switch was renamed before it was ever released, so the old value is not carried over. */
	@Test
	void aFileFromBeforeVacuumCoveredTheShulkerBoxLoadsWithVacuumAtItsDefault() throws IOException {
		Path file = write("""
				[bundle]
				vacuum = false
				""");

		EchoConfig config = EchoConfigFile.load(file);

		assertEquals(EchoConfig.DEFAULTS, config);
		assertTrue(Files.readString(file).contains("enabled = true"), Files.readString(file));
	}

	private Path write(String toml) throws IOException {
		return Files.writeString(dir.resolve("echostorage.toml"), toml);
	}
}
