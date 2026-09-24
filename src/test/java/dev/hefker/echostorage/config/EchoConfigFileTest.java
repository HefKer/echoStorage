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
		assertTrue(written.contains("wireless = false"), written);
		assertTrue(written.contains("vacuum = true"), written);
		assertTrue(written.contains("#"), "the file should explain its switches:\n" + written);
	}

	@Test
	void aSwitchTurnedOffInTheFileIsOff() throws IOException {
		Path file = write("""
				[links]
				wireless = true
				[bundle]
				vacuum = false
				""");

		EchoConfig config = EchoConfigFile.load(file);

		assertEquals(new EchoConfig(true, true, true, false, true, true), config);
	}

	@Test
	void switchesMissingFromTheFileAreAddedWithoutLosingThePlayersOwn() throws IOException {
		Path file = write("""
				[bundle]
				# vacuum pulls in junk on our server
				vacuum = false
				""");

		EchoConfigFile.load(file);

		String rewritten = Files.readString(file);
		assertTrue(rewritten.contains("vacuum pulls in junk on our server"), rewritten);
		assertTrue(rewritten.contains("vacuum = false"), rewritten);
		assertTrue(rewritten.contains("refill = true"), rewritten);
		assertTrue(rewritten.contains("wireless = false"), rewritten);
		assertEquals(new EchoConfig(false, true, true, false, true, true), EchoConfigFile.load(file));
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
				[links]
				wireless = "yes"
				[bundle]
				place = false
				""");

		assertEquals(new EchoConfig(false, true, true, true, true, false), EchoConfigFile.load(file));
	}

	private Path write(String toml) throws IOException {
		return Files.writeString(dir.resolve("echostorage.toml"), toml);
	}
}
