package dev.hefker.echostorage.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * The Selected item an Echo Bundle stores: one of the item, standing for every entry that would
 * stack with it. An item rather than an entry index, because every insert shifts the indices.
 *
 * <p>What is stored may name an item no longer inside, and an old bundle stores nothing; either
 * way {@link EchoBundleContents#selected} falls back to the first Stop. Read through
 * {@link EchoBundleItem#selectedItemOf}; the mod writes it only from {@link EchoBundleItem}.
 *
 * <p>Read leniently, as {@link EchoBundleSettings} is: an item that no longer exists, or a value of
 * the wrong type, loads as {@link #NONE}, because a failing component would lose the whole bundle.
 *
 * @param item one of the Selected item, or empty for none stored
 */
public record SelectedItem(ItemStack item) {
	public static final SelectedItem NONE = new SelectedItem(ItemStack.EMPTY);

	public static final Codec<SelectedItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ItemStack.SINGLE_ITEM_CODEC.lenientOptionalFieldOf("item", ItemStack.EMPTY).forGetter(SelectedItem::item)
	).apply(instance, SelectedItem::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, SelectedItem> STREAM_CODEC =
			ItemStack.OPTIONAL_STREAM_CODEC.map(SelectedItem::new, SelectedItem::item);

	public SelectedItem {
		item = item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1);
	}

	public boolean isEmpty() {
		return item.isEmpty();
	}

	/** Whether {@code stack} would stack with the Selected item, whatever its count. */
	public boolean matches(ItemStack stack) {
		return !item.isEmpty() && ItemStack.isSameItemSameComponents(item, stack);
	}

	/** By item and components: {@code ItemStack} has no equality of its own, and components are compared. */
	@Override
	public boolean equals(Object other) {
		return other instanceof SelectedItem selected && ItemStack.matches(item, selected.item);
	}

	@Override
	public int hashCode() {
		return ItemStack.hashItemAndComponents(item);
	}

	@Override
	public String toString() {
		return "SelectedItem[" + item + "]";
	}
}
