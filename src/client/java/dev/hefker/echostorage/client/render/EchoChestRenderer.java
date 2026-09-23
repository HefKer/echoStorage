package dev.hefker.echostorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.hefker.echostorage.block.EchoChestBlock;
import dev.hefker.echostorage.block.EchoChestBlockEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/**
 * Draws an Echo Chest as vanilla's single chest, lid animation and all. It bakes only the
 * single-chest layer: there is no double-chest model to fall into (ADR-0006).
 *
 * <p>The texture is the vanilla chest, cooled with a tint so the two can be told apart. That
 * is a stand-in until the chest has art of its own.
 */
public class EchoChestRenderer implements BlockEntityRenderer<EchoChestBlockEntity> {
	private static final int TINT = 0xFF9FC4C8;

	private final ModelPart lid;
	private final ModelPart lock;
	private final ModelPart bottom;

	public EchoChestRenderer(BlockEntityRendererProvider.Context context) {
		ModelPart chest = context.bakeLayer(ModelLayers.CHEST);
		this.lid = chest.getChild("lid");
		this.lock = chest.getChild("lock");
		this.bottom = chest.getChild("bottom");
	}

	@Override
	public void render(EchoChestBlockEntity chest, float partialTick, PoseStack pose, MultiBufferSource buffers,
			int light, int overlay) {
		// Rendered as an item there is no level, and the default state faces the viewer.
		Direction facing = chest.getLevel() != null
				? chest.getBlockState().getValue(EchoChestBlock.FACING)
				: Direction.SOUTH;

		pose.pushPose();
		pose.translate(0.5F, 0.5F, 0.5F);
		pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
		pose.translate(-0.5F, -0.5F, -0.5F);

		// Vanilla's easing: the lid snaps open and settles closed.
		float closed = 1.0F - chest.getOpenNess(partialTick);
		float open = 1.0F - closed * closed * closed;
		lid.xRot = -(open * ((float) Math.PI / 2F));
		lock.xRot = lid.xRot;

		VertexConsumer vertices = Sheets.CHEST_LOCATION.buffer(buffers, RenderType::entityCutout);
		lid.render(pose, vertices, light, overlay, TINT);
		lock.render(pose, vertices, light, overlay, TINT);
		bottom.render(pose, vertices, light, overlay, TINT);

		pose.popPose();
	}
}
