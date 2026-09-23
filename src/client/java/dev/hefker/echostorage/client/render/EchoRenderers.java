package dev.hefker.echostorage.client.render;

import dev.hefker.echostorage.block.EchoBlocks;
import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.item.EchoItems;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;

/**
 * Block entity and item renderers. The block half is vanilla {@code BlockEntityRenderers};
 * the item half is the one loader API here, because vanilla's item renderer only knows its
 * own chests. On NeoForge it becomes {@code IClientItemExtensions}.
 */
public final class EchoRenderers {
	private EchoRenderers() {
	}

	public static void register() {
		BlockEntityRenderers.register(EchoBlocks.ECHO_CHEST_ENTITY, EchoChestRenderer::new);

		// The item is drawn by the block entity renderer, over a chest that is in no level.
		EchoChestBlockEntity itemChest = new EchoChestBlockEntity(BlockPos.ZERO, EchoBlocks.ECHO_CHEST.defaultBlockState());
		BuiltinItemRendererRegistry.INSTANCE.register(EchoItems.ECHO_CHEST, (stack, mode, pose, buffers, light, overlay) ->
				Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(itemChest, pose, buffers, light, overlay));
	}
}
