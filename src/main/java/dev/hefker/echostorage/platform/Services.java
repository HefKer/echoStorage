package dev.hefker.echostorage.platform;

import java.util.Objects;
import java.util.ServiceLoader;

import dev.hefker.echostorage.EchoStorage;

/**
 * The single {@link ServiceLoader} lookup for loader-specific services (ADR-0003 rule 2,
 * the {@code jaredlll08/MultiLoader-Template} pattern).
 *
 * <p>Nothing else in the mod may call {@code ServiceLoader} directly.
 */
public final class Services {
	public static final PlatformHelper PLATFORM = load(PlatformHelper.class);

	private Services() {
	}

	/**
	 * Resolves the single implementation of {@code clazz} on the classpath.
	 *
	 * @throws NullPointerException if no implementation is registered — a missing
	 *         {@code META-INF/services} entry is a packaging bug, not a runtime condition,
	 *         so it fails immediately rather than handing back null.
	 */
	public static <T> T load(Class<T> clazz) {
		T loaded = ServiceLoader.load(clazz)
				.findFirst()
				.orElse(null);
		Objects.requireNonNull(loaded, () -> "No implementation of " + clazz.getName() + " on the classpath");
		EchoStorage.LOGGER.debug("Loaded {} for service {}", loaded.getClass().getName(), clazz.getName());
		return loaded;
	}
}
