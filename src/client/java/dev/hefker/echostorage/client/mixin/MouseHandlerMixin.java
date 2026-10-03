package dev.hefker.echostorage.client.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import dev.hefker.echostorage.client.SelectedItemScrolling;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sneak + scroll's hook: a scroll that steps an Echo Bundle's Selected item does not also move
 * the hotbar. Wraps the one call that scrolls the hotbar, so it only ever sees a scroll in the
 * world with no screen open and the player not spectating. Fabric API has no in-world scroll
 * event on 1.21.1; on NeoForge this is an {@code InputEvent.MouseScrollingEvent} handler.
 */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
	@WrapWithCondition(method = "onScroll", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Inventory;swapPaint(D)V"))
	private boolean echostorage$stepSelectedItem(Inventory inventory, double direction) {
		return !SelectedItemScrolling.onScroll(inventory.player, direction);
	}
}
