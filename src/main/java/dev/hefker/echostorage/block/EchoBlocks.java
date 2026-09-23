package dev.hefker.echostorage.block;

import dev.hefker.echostorage.EchoStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Block and block entity registration. Vanilla registries only, so this ports unchanged. */
public final class EchoBlocks {
	public static final Block ECHO_CHEST = new EchoChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST));

	public static final BlockEntityType<EchoChestBlockEntity> ECHO_CHEST_ENTITY =
			BlockEntityType.Builder.of(EchoChestBlockEntity::new, ECHO_CHEST).build(null);

	private EchoBlocks() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.BLOCK, EchoStorage.id("echo_chest"), ECHO_CHEST);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, EchoStorage.id("echo_chest"), ECHO_CHEST_ENTITY);
	}
}
