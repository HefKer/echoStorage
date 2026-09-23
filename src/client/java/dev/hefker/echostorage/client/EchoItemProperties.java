package dev.hefker.echostorage.client;

import dev.hefker.echostorage.item.EchoBundleItem;
import dev.hefker.echostorage.item.EchoItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

/** Model predicates. Vanilla {@code ItemProperties}, so this ports unchanged. */
public final class EchoItemProperties {
	private EchoItemProperties() {
	}

	public static void register() {
		ItemProperties.register(EchoItems.ECHO_BUNDLE, ResourceLocation.withDefaultNamespace("filled"),
				(stack, level, entity, seed) -> EchoBundleItem.getFullnessDisplay(stack));
	}
}
