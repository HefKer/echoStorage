package dev.hefker.echostorage.link;

import java.util.UUID;

import net.minecraft.core.BlockPos;

/** An Echo Chest a Link reached: which chest, and where it stands. */
public record LinkedChest(UUID id, BlockPos pos) {
}
