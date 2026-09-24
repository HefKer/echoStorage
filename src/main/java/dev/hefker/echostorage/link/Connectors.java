package dev.hefker.echostorage.link;

import dev.hefker.echostorage.EchoStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * The blocks that can carry a Link. Read from a tag, never hardcoded, so packs and later
 * versions add their own; the mod ships sculk vein and sculk block.
 */
public final class Connectors {
	public static final TagKey<Block> TAG = TagKey.create(Registries.BLOCK, EchoStorage.id("connectors"));

	private Connectors() {
	}
}
