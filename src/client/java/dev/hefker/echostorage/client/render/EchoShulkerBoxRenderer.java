package dev.hefker.echostorage.client.render;

import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hefker.echostorage.block.EchoShulkerBoxBlock;
import dev.hefker.echostorage.block.EchoShulkerBoxBlockEntity;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws an Echo Shulker Box as vanilla's shulker box, lid animation and all. Every colour is one
 * texture, vanilla's white shulker, tinted by the dye, or the Echo Chest's teal when undyed, so
 * the box's own art only has to replace that one texture.
 */
public class EchoShulkerBoxRenderer implements BlockEntityRenderer<EchoShulkerBoxBlockEntity> {
	private static final Material TEXTURE = Sheets.SHULKER_TEXTURE_LOCATION.get(DyeColor.WHITE.getId());
	/** Vanilla draws a shulker box a hair smaller than a block, so it never z-fights its neighbours. */
	private static final float SCALE = 0.9995F;
	private static final float MAX_LID_ROTATION = 270.0F;

	private final ShulkerModel<Shulker> model;

	public EchoShulkerBoxRenderer(BlockEntityRendererProvider.Context context) {
		this.model = new ShulkerModel<>(context.bakeLayer(ModelLayers.SHULKER));
	}

	@Override
	public void render(EchoShulkerBoxBlockEntity box, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		// Rendered as an item there is no level, and the default state faces up.
		BlockState state = box.getBlockState();
		Direction facing = box.hasLevel() && state.hasProperty(EchoShulkerBoxBlock.FACING)
				? state.getValue(EchoShulkerBoxBlock.FACING)
				: Direction.UP;
		draw(facing, box.getOpenNess(partialTick), box.color(), pose, buffers, light, overlay);
	}

	/** Draws a box facing {@code facing}, its lid {@code openness} of the way open, in {@code color}. */
	void draw(Direction facing, float openness, Optional<DyeColor> color, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		pose.pushPose();
		pose.translate(0.5F, 0.5F, 0.5F);
		pose.scale(SCALE, SCALE, SCALE);
		pose.mulPose(facing.getRotation());
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.0F, -1.0F, 0.0F);

		ModelPart lid = model.getLid();
		lid.setPos(0.0F, 24.0F - openness * 0.5F * 16.0F, 0.0F);
		lid.yRot = MAX_LID_ROTATION * openness * ((float) Math.PI / 180F);

		int tint = color.map(DyeColor::getTextureDiffuseColor).orElse(EchoChestRenderer.TINT);
		VertexConsumer vertices = TEXTURE.buffer(buffers, RenderType::entityCutoutNoCull);
		model.renderToBuffer(pose, vertices, light, overlay, tint);
		pose.popPose();
	}
}
