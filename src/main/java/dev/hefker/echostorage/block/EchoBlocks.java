package dev.hefker.echostorage.block;

import dev.hefker.echostorage.EchoStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Block and block entity registration. Vanilla registries only, so this ports unchanged. */
public final class EchoBlocks {
	public static final Block ECHO_CHEST = new EchoChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST));

	public static final BlockEntityType<EchoChestBlockEntity> ECHO_CHEST_ENTITY =
			BlockEntityType.Builder.of(EchoChestBlockEntity::new, ECHO_CHEST).build(null);

	public static final Block ECHO_INTERFACE = new EchoInterfaceBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_CYAN)
			.strength(3.0F, 6.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.SCULK_CATALYST));

	public static final BlockEntityType<EchoInterfaceBlockEntity> ECHO_INTERFACE_ENTITY =
			BlockEntityType.Builder.of(EchoInterfaceBlockEntity::new, ECHO_INTERFACE).build(null);

	public static final Block ECHO_RELAY = new EchoRelayBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_CYAN)
			.strength(1.5F)
			.sound(SoundType.SCULK_SENSOR)
			.lightLevel(state -> 1));

	public static final BlockEntityType<EchoRelayBlockEntity> ECHO_RELAY_ENTITY =
			BlockEntityType.Builder.of(EchoRelayBlockEntity::new, ECHO_RELAY).build(null);

	private EchoBlocks() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.BLOCK, EchoStorage.id("echo_chest"), ECHO_CHEST);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, EchoStorage.id("echo_chest"), ECHO_CHEST_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK, EchoStorage.id("echo_interface"), ECHO_INTERFACE);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, EchoStorage.id("echo_interface"), ECHO_INTERFACE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK, EchoStorage.id("echo_relay"), ECHO_RELAY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, EchoStorage.id("echo_relay"), ECHO_RELAY_ENTITY);
	}
}
