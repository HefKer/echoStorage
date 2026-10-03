package dev.hefker.echostorage.gametest;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Another mod's shulker box whose block extends vanilla's, unlike the heart of the sea that
 * stands in for one elsewhere. Registered under the game test mod's namespace and tagged into
 * {@code c:shulker_boxes} by the game test data pack. Such a box may have a size of its own, so
 * Echo Storage treats it as one of unknown size (ADR-0007). It is only ever an item in these
 * tests, so it has no block entity type.
 */
public class ShulkerBoxExtendingVanillas implements ModInitializer {
	public static final Block BLOCK = new ShulkerBoxBlock(null, BlockBehaviour.Properties.ofFullCopy(Blocks.SHULKER_BOX));

	public static final Item ITEM = new BlockItem(BLOCK, new Item.Properties().stacksTo(1));

	@Override
	public void onInitialize() {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath("echostorage-gametest", "shulker_box");
		Registry.register(BuiltInRegistries.BLOCK, id, BLOCK);
		Registry.register(BuiltInRegistries.ITEM, id, ITEM);
	}
}
