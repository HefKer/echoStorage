package dev.hefker.echostorage.client.render;

import java.util.Optional;

import dev.hefker.echostorage.block.EchoBlocks;
import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.block.EchoShulkerBoxBlockEntity;
import dev.hefker.echostorage.item.EchoItems;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Block entity and item renderers, and block render layers. The block entity half is vanilla
 * {@code BlockEntityRenderers}; the item half is a loader API, because vanilla's item renderer
 * only knows its own chests, and becomes {@code IClientItemExtensions} on NeoForge. The render
 * layers are a loader API too, since vanilla's map of them is closed; on NeoForge the model
 * JSON names its {@code render_type} instead.
 */
public final class EchoRenderers {
	private EchoRenderers() {
	}

	public static void register() {
		BlockEntityRenderers.register(EchoBlocks.ECHO_CHEST_ENTITY, EchoChestRenderer::new);
		BlockEntityRenderers.register(EchoBlocks.ECHO_SHULKER_BOX_ENTITY, EchoShulkerBoxRenderer::new);
		// The sculk sensor's tendrils the relay's model borrows are cut out.
		BlockRenderLayerMap.INSTANCE.putBlock(EchoBlocks.ECHO_RELAY, RenderType.cutout());

		registerChestItem(EchoItems.ECHO_CHEST, EchoBlocks.ECHO_CHEST);
		registerChestItem(EchoItems.DEEP_ECHO_CHEST, EchoBlocks.DEEP_ECHO_CHEST);
		registerShulkerBoxItem();
	}

	/** The item is drawn by the block entity renderer, over a chest of its kind that is in no level. */
	private static void registerChestItem(Item item, Block block) {
		EchoChestBlockEntity itemChest = new EchoChestBlockEntity(BlockPos.ZERO, block.defaultBlockState());
		BuiltinItemRendererRegistry.INSTANCE.register(item, (stack, mode, pose, buffers, light, overlay) ->
				Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(itemChest, pose, buffers, light, overlay));
	}

	/**
	 * The box's item is drawn in the colour on its stack, so it asks the block entity renderer to
	 * draw a shut box rather than handing it a block entity, which has only one colour.
	 */
	private static void registerShulkerBoxItem() {
		EchoShulkerBoxBlockEntity itemBox = new EchoShulkerBoxBlockEntity(BlockPos.ZERO, EchoBlocks.ECHO_SHULKER_BOX.defaultBlockState());
		BuiltinItemRendererRegistry.INSTANCE.register(EchoItems.ECHO_SHULKER_BOX, (stack, mode, pose, buffers, light, overlay) -> {
			if (Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(itemBox) instanceof EchoShulkerBoxRenderer renderer) {
				renderer.draw(Direction.UP, 0.0F, Optional.ofNullable(stack.get(DataComponents.BASE_COLOR)), pose, buffers, light, overlay);
			}
		});
	}
}
