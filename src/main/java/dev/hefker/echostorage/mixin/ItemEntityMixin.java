package dev.hefker.echostorage.mixin;

import java.util.UUID;

import dev.hefker.echostorage.config.EchoConfig;
import dev.hefker.echostorage.item.Vacuum;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Auto-vacuum's hook: a pickup offers the stack to the player's Echo Bundles before vanilla puts
 * what is left in the inventory. On NeoForge this is an {@code ItemEntityPickupEvent.Pre} handler.
 */
@Mixin(ItemEntity.class)
abstract class ItemEntityMixin {
	@Shadow
	private int pickupDelay;

	@Shadow
	@Nullable
	private UUID target;

	@Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
	private void echostorage$vacuum(Player player, CallbackInfo callback) {
		ItemEntity self = (ItemEntity) (Object) this;
		// The same conditions vanilla checks before it will let the player take the item at all.
		if (self.level().isClientSide() || pickupDelay != 0 || (target != null && !target.equals(player.getUUID()))
				|| !EchoConfig.get().bundleVacuum()) {
			return;
		}
		ItemStack stack = self.getItem();
		ItemStack before = stack.copy();
		Vacuum.run(player.getInventory(), stack);
		int taken = before.getCount() - stack.getCount();
		if (taken == 0) {
			return;
		}

		// What vanilla does for a pickup, for the part the bundles took; it handles the rest.
		// No inventory slot changed but the bundle's, so the trigger behind "Diamonds!" and
		// recipe unlocks is told about the item itself.
		player.take(self, taken);
		player.awardStat(Stats.ITEM_PICKED_UP.get(before.getItem()), taken);
		if (player instanceof ServerPlayer serverPlayer) {
			CriteriaTriggers.INVENTORY_CHANGED.trigger(serverPlayer, player.getInventory(), before.copyWithCount(taken));
		}
		if (stack.isEmpty()) {
			self.discard();
			stack.setCount(taken);
			callback.cancel();
		}
		player.onItemPickup(self);
	}
}
