package dev.hefker.echostorage.platform;

/**
 * The loader-specific questions the rest of the mod is allowed to ask.
 *
 * <p>ADR-0003 rule 2: every mod loader API that has no vanilla equivalent goes behind this
 * interface, looked up with {@link Services}. Porting means writing one more implementation
 * and one more {@code META-INF/services} file — nothing above this package changes.
 *
 * <p>Keep it small. A method belongs here only when vanilla genuinely cannot answer it.
 */
public interface PlatformHelper {
	/** Human-readable loader name, e.g. {@code "Fabric"}. Used in logs and crash reports. */
	String platformName();

	/** Whether a mod with the given id is loaded. */
	boolean isModLoaded(String modId);

	/** Whether the game is running from a development environment rather than a shipped jar. */
	boolean isDevelopmentEnvironment();
}
