package dev.hefker.echostorage.platform;

import java.nio.file.Path;

/**
 * The loader-specific questions the rest of the mod is allowed to ask.
 *
 * <p>ADR-0003 rule 2: loader-specific questions are answered behind this interface, looked up
 * with {@link Services}. Porting means writing one more implementation and one more
 * {@code META-INF/services} file — nothing above this package changes.
 *
 * <p>Keep it small. A method belongs here only when vanilla genuinely cannot answer it, and
 * only once something calls it. Loader APIs that register rather than answer — networking,
 * menus, commands — are confined to their own package instead, which
 * {@code PortabilityRulesTest} enforces.
 */
public interface PlatformHelper {
	/** Human-readable loader name, e.g. {@code "Fabric"}. Used in logs and crash reports. */
	String platformName();

	/** The directory the loader keeps mod config files in. */
	Path configDir();
}
