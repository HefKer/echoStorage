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
			new ConfigSwitch("vacuum.enabled",
					" Let a carried Echo Bundle or Echo Shulker Box vacuum up items on pickup. Each one still starts with it off.",
					EchoConfig::vacuumEnabled),
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

	static {
		checkOrder();
	}

	/** This switch's value in {@code config}. */
	public boolean valueIn(EchoConfig config) {
		return read.test(config);
	}

	public boolean fallback() {
		return valueIn(EchoConfig.DEFAULTS);
	}

	/**
	 * The config whose switches take the values {@code valueOf} gives, asked once per switch in
	 * {@link #ALL} order, so a reader of a stream can hand them back as it reads.
	 */
	public static EchoConfig build(Predicate<ConfigSwitch> valueOf) {
		List<Boolean> values = new ArrayList<>(ALL.size());
		for (ConfigSwitch option : ALL) {
			values.add(valueOf.test(option));
		}
		try {
			return (EchoConfig) CONSTRUCTOR.invokeWithArguments(values);
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException("Could not build an EchoConfig from " + values, e);
		}
	}

	/**
	 * Refuses a record whose components don't line up with {@link #ALL}: a count or a type that
	 * differs here would otherwise shift every later value onto its neighbour.
	 */
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

	/**
	 * Builds a config with each switch alone on and checks that exactly that switch reads it back,
	 * so {@link #ALL} listed out of the record's order fails at load rather than swapping switches.
	 */
	private static void checkOrder() {
		for (ConfigSwitch on : ALL) {
			EchoConfig config = build(option -> option == on);
			for (ConfigSwitch option : ALL) {
				if (option.valueIn(config) != (option == on)) {
					throw new IllegalStateException(on.path() + " is listed out of EchoConfig's component order");
				}
			}
		}
	}
}
