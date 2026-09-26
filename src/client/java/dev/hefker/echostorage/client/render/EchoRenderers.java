package dev.hefker.echostorage.client.render;

import dev.hefker.echostorage.block.EchoBlocks;
import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.item.EchoItems;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;

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
		// The sculk sensor's tendrils the relay's model borrows are cut out.
		BlockRenderLayerMap.INSTANCE.putBlock(EchoBlocks.ECHO_RELAY, RenderType.cutout());

		// The item is drawn by the block entity renderer, over a chest that is in no level.
		EchoChestBlockEntity itemChest = new EchoChestBlockEntity(BlockPos.ZERO, EchoBlocks.ECHO_CHEST.defaultBlockState());
		BuiltinItemRendererRegistry.INSTANCE.register(EchoItems.ECHO_CHEST, (stack, mode, pose, buffers, light, overlay) ->
				Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(itemChest, pose, buffers, light, overlay));
	}
}
