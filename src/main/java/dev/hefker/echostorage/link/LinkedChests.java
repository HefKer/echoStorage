package dev.hefker.echostorage.link;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.hefker.echostorage.EchoStorage;
import dev.hefker.echostorage.block.EchoChestName;
import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.category.Category;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;

/** The Echo Chests an Echo Interface lists, one row each, kept between resolutions. */
public final class LinkedChests {
	/** One screen's worth (ADR-0004). Greyed rows count, so a new chest waits for one to be dismissed. */
	public static final int MAX_ROWS = 27;
	/**
	 * The item a row names when nothing better is known: a row saved before rows named one, when
	 * every chest was an Echo Chest, or a chest no longer there to ask.
	 */
	public static final ResourceLocation ECHO_CHEST_ITEM = EchoStorage.id("echo_chest");

	private final List<Row> rows = new ArrayList<>();

	public List<Row> rows() {
		return List.copyOf(rows);
	}

	/** Puts back rows saved earlier, as many as fit. */
	public void replace(List<Row> saved) {
		rows.clear();
		rows.addAll(saved.subList(0, Math.min(saved.size(), MAX_ROWS)));
	}

	/**
	 * Brings the rows up to date with {@code resolution}.
	 *
	 * @param labels   what each chest found is called now
	 * @param isLoaded whether the chunk holding a position is loaded
	 */
	public void update(Resolution resolution, Function<LinkedChest, Label> labels, Predicate<BlockPos> isLoaded) {
		Map<UUID, LinkedChest> found = new LinkedHashMap<>();
		resolution.chests().forEach(chest -> found.put(chest.id(), chest));
		rows.replaceAll(row -> {
			LinkedChest chest = found.remove(row.id());
			return chest == null ? row.withState(missing(row, resolution, isLoaded)) : linked(chest, labels);
		});
		for (LinkedChest chest : found.values()) {
			if (rows.size() >= MAX_ROWS) {
				break;
			}
			rows.add(linked(chest, labels));
		}
	}

	/**
	 * Takes a greyed row off the list, and says whether it did. A linked row stays: it would only
	 * come straight back.
	 */
	public boolean dismiss(UUID id) {
		return rows.removeIf(row -> row.id().equals(id) && row.state() != State.LINKED);
	}

	/**
	 * Why a listed chest was not reached. Only a chest whose chunk is loaded, behind a Link that
	 * met no unloaded chunk, is known to be gone; any other may turn up once the chunks load.
	 * Once lost, a row stays lost until its chest is reached again.
	 */
	private static State missing(Row row, Resolution resolution, Predicate<BlockPos> isLoaded) {
		if (!isLoaded.test(row.pos())) {
			return State.UNLOADED;
		}
		return resolution.complete() || row.state() == State.LOST ? State.LOST : State.UNLOADED;
	}

	private static Row linked(LinkedChest chest, Function<LinkedChest, Label> labels) {
		Label label = labels.apply(chest);
		return new Row(chest.id(), chest.pos(), label.item(), label.color(), label.name(), label.category(), State.LINKED);
	}

	/**
	 * What a chest is, as the chest holds it: its item and colour, the typed name, empty if none,
	 * and the Category. The label shown for an unnamed chest is worked out from these where it is drawn,
	 * never stored, so clearing a Category leaves no name behind.
	 *
	 * @param item the registry id of the chest's item, which the row draws as its icon and which
	 *             names an unnamed chest with no Category
	 * @param color what the icon is dyed, for a chest that can be; empty for one that is not
	 */
	public record Label(ResourceLocation item, Optional<DyeColor> color, String name, Optional<Category> category) {
	}

	/**
	 * One listed chest, as it was when last reached.
	 *
	 * @param item  a registry id rather than a flag for one kind, so every kind of chest can be listed
	 * @param color the colour the chest's icon is drawn in, as an Echo Shulker Box is dyed
	 */
	public record Row(UUID id, BlockPos pos, ResourceLocation item, Optional<DyeColor> color, String name, Optional<Category> category,
			State state) {
		/**
		 * Saved with the interface. A Category that no longer ships, or one of the wrong type, loads
		 * as none, a name of the wrong type as no name, a missing or malformed item as an Echo
		 * Chest's, and a missing or unknown colour as undyed, so the row keeps its place rather than
		 * dropping off the list.
		 */
		public static final Codec<Row> CODEC = RecordCodecBuilder.create(row -> row.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(Row::id),
				BlockPos.CODEC.fieldOf("pos").forGetter(Row::pos),
				ResourceLocation.CODEC.lenientOptionalFieldOf("item", ECHO_CHEST_ITEM).forGetter(Row::item),
				DyeColor.CODEC.lenientOptionalFieldOf("color").forGetter(Row::color),
				Codec.STRING.lenientOptionalFieldOf("name", "").forGetter(Row::name),
				Categories.OPTIONAL_FIELD.forGetter(Row::category),
				State.CODEC.fieldOf("state").forGetter(Row::state)
		).apply(row, Row::new));

		/**
		 * Sent to an open interface screen, which works out each row's label from it. Seven fields
		 * are one more than {@code StreamCodec.composite} takes, so the kind of chest travels as a pair.
		 */
		public static final StreamCodec<ByteBuf, Row> STREAM_CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, Row::id,
				BlockPos.STREAM_CODEC, Row::pos,
				Kind.STREAM_CODEC, Row::kind,
				ByteBufCodecs.stringUtf8(EchoChestName.MAX_LENGTH), Row::name,
				ByteBufCodecs.optional(Categories.STREAM_CODEC), Row::category,
				State.STREAM_CODEC, Row::state,
				(id, pos, kind, name, category, state) -> new Row(id, pos, kind.item(), kind.color(), name, category, state));

		Row withState(State state) {
			return new Row(id, pos, item, color, name, category, state);
		}

		private Kind kind() {
			return new Kind(item, color);
		}

		/** The part of a row that says what the icon is: which chest, in which colour. */
		private record Kind(ResourceLocation item, Optional<DyeColor> color) {
			static final StreamCodec<ByteBuf, Kind> STREAM_CODEC = StreamCodec.composite(
					ResourceLocation.STREAM_CODEC, Kind::item,
					ByteBufCodecs.optional(DyeColor.STREAM_CODEC), Kind::color,
					Kind::new);
		}
	}

	public enum State implements StringRepresentable {
		/** Reached by the last resolution, so it can be opened. */
		LINKED,
		/** Not reached because a chunk it or its Link stands in is unloaded, and never force-loaded. */
		UNLOADED,
		/** Not reached, though every chunk the Link runs through was loaded: broken, replaced, or cut off. */
		LOST;

		static final Codec<State> CODEC = StringRepresentable.fromEnum(State::values);

		/** Throws on a state it does not know rather than read it as one, least of all as openable. */
		static final StreamCodec<ByteBuf, State> STREAM_CODEC = ByteBufCodecs.idMapper(ordinal -> {
			if (ordinal < 0 || ordinal >= values().length) {
				throw new DecoderException("Unknown row state: " + ordinal);
			}
			return values()[ordinal];
		}, State::ordinal);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
}
