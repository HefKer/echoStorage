package dev.hefker.echostorage.link;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

/** An Echo Chest a Link reached, or an Echo Relay heard: which chest, and where it stands. */
public record LinkedChest(UUID id, BlockPos pos) {
	/** Saved with an Echo Relay, for each chest it has heard. */
	public static final Codec<LinkedChest> CODEC = RecordCodecBuilder.create(chest -> chest.group(
			UUIDUtil.CODEC.fieldOf("id").forGetter(LinkedChest::id),
			BlockPos.CODEC.fieldOf("pos").forGetter(LinkedChest::pos)
	).apply(chest, LinkedChest::new));
}
