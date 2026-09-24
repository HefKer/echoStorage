package dev.hefker.echostorage.category;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The preset Categories. v1 ships only those its tag layer can do well (ADR-0001); wood,
 * stone, nature, lighting and redstone wait for a predicate layer rather than ship wrong.
 */
public final class Categories {
	public static final Category FOOD = Category.of("food");
	public static final Category ORES = Category.of("ores");
	public static final Category MATERIALS = Category.of("materials");
	public static final Category STORAGE_BLOCKS = Category.of("storage_blocks");
	public static final Category CROPS = Category.of("crops");
	public static final Category DYES = Category.of("dyes");
	public static final Category WOOL = Category.of("wool");
	public static final Category TOOLS = Category.of("tools");
	public static final Category ARMOR = Category.of("armor");

	/** Every preset, in the order a player is offered them. */
	public static final List<Category> ALL = List.of(FOOD, ORES, MATERIALS, STORAGE_BLOCKS, CROPS, DYES, WOOL, TOOLS, ARMOR);

	/** A Category saved as its name. A name no preset has any more fails to load. */
	public static final Codec<Category> CODEC = Codec.STRING.comapFlatMap(
			name -> byName(name).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown Category: " + name)),
			Category::name);

	/** A Category sent as its name. Both sides ship the same presets, so every name sent is known. */
	public static final StreamCodec<ByteBuf, Category> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
			name -> byName(name).orElseThrow(() -> new DecoderException("Unknown Category: " + name)),
			Category::name);

	private Categories() {
	}

	/** The preset called {@code name}, if one ships. */
	public static Optional<Category> byName(String name) {
		return ALL.stream().filter(category -> category.name().equals(name)).findFirst();
	}
}
