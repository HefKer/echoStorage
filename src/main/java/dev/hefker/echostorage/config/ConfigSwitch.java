package dev.hefker.echostorage.config;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * One {@link EchoConfig} switch: where it sits in the TOML file, the comment written above it,
 * and how to read it off a config. {@link #ALL} lists them in the record's component order, and
 * is the one place that order is written down: {@link EchoConfigFile} fills a config from it and
 * the config payload puts one boolean per switch on the wire in it.
 *
 * @param path    the TOML key, with dots between tables
 * @param comment written above the key when the file lacks it
 * @param read    the record accessor this switch answers to
 */
public record ConfigSwitch(String path, String comment, Predicate<EchoConfig> read) {
	/** Every switch, in {@link EchoConfig}'s component order. */
	public static final List<ConfigSwitch> ALL = List.of(
			new ConfigSwitch("search.in_open_container",
					" Offer a search box on an open container's screen.",
					EchoConfig::searchInOpenContainer),
			new ConfigSwitch("search.by_chest_name",
					" Let an Echo Interface search its chests by name.",
					EchoConfig::searchByChestName),
			new ConfigSwitch("bundle.vacuum",
					" Let an Echo Bundle vacuum up items on pickup. Each bundle still starts with it off.",
					EchoConfig::bundleVacuum),
			new ConfigSwitch("bundle.refill",
					" Placing the last block in hand pulls the next one from an Echo Bundle.",
					EchoConfig::bundleRefill),
			new ConfigSwitch("bundle.place",
					" Using an Echo Bundle on a block places a block out of it.",
					EchoConfig::bundlePlace),
			new ConfigSwitch("quick_stack.chest",
					" Offer a quick-stack button on an Echo Chest's screen.",
					EchoConfig::chestQuickStack),
			new ConfigSwitch("quick_stack.interface",
					" Offer a quick-stack button on an Echo Interface's screen, into every linked chest at once.",
					EchoConfig::interfaceQuickStack));

	/** The record's canonical constructor, taking one boolean per switch. */
	private static final MethodHandle CONSTRUCTOR = canonicalConstructor();

	public boolean in(EchoConfig config) {
		return read.test(config);
	}

	public boolean fallback() {
		return in(EchoConfig.DEFAULTS);
	}

	/** Each switch's value in {@code config}, in {@link #ALL} order. */
	public static List<Boolean> values(EchoConfig config) {
		List<Boolean> values = new ArrayList<>(ALL.size());
		for (ConfigSwitch option : ALL) {
			values.add(option.in(config));
		}
		return values;
	}

	/**
	 * The config whose switches take {@code values}, in {@link #ALL} order. Refuses a list of the
	 * wrong length rather than shifting values onto their neighbours.
	 */
	public static EchoConfig build(List<Boolean> values) {
		if (values.size() != ALL.size()) {
			throw new IllegalArgumentException("Expected " + ALL.size() + " switch values, got " + values.size());
		}
		try {
			return (EchoConfig) CONSTRUCTOR.invokeWithArguments(values);
		} catch (Throwable e) {
			throw new IllegalStateException("Could not build an EchoConfig from " + values, e);
		}
	}

	private static MethodHandle canonicalConstructor() {
		RecordComponent[] components = EchoConfig.class.getRecordComponents();
		if (components.length != ALL.size()) {
			throw new IllegalStateException("EchoConfig has " + components.length + " components but "
					+ ALL.size() + " switches are listed");
		}
		Class<?>[] types = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
		if (Arrays.stream(types).anyMatch(type -> type != boolean.class)) {
			throw new IllegalStateException("EchoConfig's components must all be boolean switches");
		}
		try {
			return MethodHandles.lookup().findConstructor(EchoConfig.class, MethodType.methodType(void.class, types));
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("EchoConfig has no canonical constructor to build from", e);
		}
	}
}
