package dev.hefker.echostorage.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.hefker.echostorage.config.EchoConfig;
import dev.hefker.echostorage.item.Refill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Auto-refill's hook: once a placement uses up the last block in the player's hand, the next
 * stack of it comes out of an Echo Bundle. Server-side only; the client learns of the new stack
 * from the inventory sync. On NeoForge this is a {@code BlockEvent.EntityPlaceEvent} handler.
 */
@Mixin(BlockItem.class)
abstract class BlockItemMixin {
	@WrapOperation(method = "place", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"))
	private void echostorage$refill(ItemStack stack, int amount, LivingEntity entity, Operation<Void> consume,
			@Local(argsOnly = true) BlockPlaceContext context) {
		ItemStack like = stack.copyWithCount(1);
		consume.call(stack, amount, entity);
		if (!stack.isEmpty() || !(entity instanceof ServerPlayer player) || !EchoConfig.get().bundleRefill()) {
			return;
		}
		// Only the hand's own stack: placing from a bundle places a copy, and is no reason to refill.
		InteractionHand hand = context.getHand();
		if (player.getItemInHand(hand) != stack) {
			return;
		}
		ItemStack next = Refill.take(player.getInventory(), like);
		if (!next.isEmpty()) {
			player.setItemInHand(hand, next);
		}
	}
}
