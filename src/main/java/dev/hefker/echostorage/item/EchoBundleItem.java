package dev.hefker.echostorage.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.hefker.echostorage.config.EchoConfig;
import dev.hefker.echostorage.menu.EchoBundleMenuProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.lang3.math.Fraction;

/**
 * A bundle holding four bundles' worth. Behaves like vanilla's {@code BundleItem}, which it
 * cannot extend because every method there names vanilla's contents component; the
 * differences are only where four times the contents would otherwise overflow something.
 */
public class EchoBundleItem extends Item {
	private static final int BAR_COLOR = Mth.color(0.4F, 0.4F, 1.0F);
	private static final int TOOLTIP_MAX_WEIGHT = 64 * EchoBundleContents.CAPACITY_IN_STACKS;

	/** Vanilla's worst case for one bundle; a single use empties at most this many entries. */
	public static final int MAX_ENTRIES_DROPPED_AT_ONCE = 64;

	public EchoBundleItem(Item.Properties properties) {
		super(properties);
	}

	public static float getFullnessDisplay(ItemStack stack) {
		return contentsOf(stack).weight().floatValue();
	}

	/** What the player has set on this bundle from its screen; the defaults if nothing. */
	public static EchoBundleSettings settingsOf(ItemStack stack) {
		return stack.getOrDefault(EchoComponents.ECHO_BUNDLE_SETTINGS, EchoBundleSettings.DEFAULT);
	}

	/** Writes {@code settings}, leaving no component behind once they are back to the defaults. */
	public static void setSettings(ItemStack stack, EchoBundleSettings settings) {
		if (settings.equals(EchoBundleSettings.DEFAULT)) {
			stack.remove(EchoComponents.ECHO_BUNDLE_SETTINGS);
		} else {
			stack.set(EchoComponents.ECHO_BUNDLE_SETTINGS, settings);
		}
	}

	private static EchoBundleContents contentsOf(ItemStack stack) {
		return stack.getOrDefault(EchoComponents.ECHO_BUNDLE_CONTENTS, EchoBundleContents.EMPTY);
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack bundle, Slot slot, ClickAction action, Player player) {
		if (action != ClickAction.SECONDARY) {
			return false;
		}
		EchoBundleContents contents = bundle.get(EchoComponents.ECHO_BUNDLE_CONTENTS);
		if (contents == null) {
			return false;
		}

		ItemStack inSlot = slot.getItem();
		EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(contents);
		if (inSlot.isEmpty()) {
			playSound(player, SoundEvents.BUNDLE_REMOVE_ONE);
			ItemStack removed = mutable.removeOne();
			if (!removed.isEmpty()) {
				mutable.tryInsert(slot.safeInsert(removed));
			}
		} else if (mutable.tryTransfer(slot, player) > 0) {
			playSound(player, SoundEvents.BUNDLE_INSERT);
		}

		bundle.set(EchoComponents.ECHO_BUNDLE_CONTENTS, mutable.toImmutable());
		return true;
	}

	@Override
	public boolean overrideOtherStackedOnMe(
			ItemStack bundle, ItemStack carried, Slot slot, ClickAction action, Player player, SlotAccess carriedAccess) {
		if (action != ClickAction.SECONDARY || !slot.allowModification(player)) {
			return false;
		}
		EchoBundleContents contents = bundle.get(EchoComponents.ECHO_BUNDLE_CONTENTS);
		if (contents == null) {
			return false;
		}

		EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(contents);
		if (carried.isEmpty()) {
			ItemStack removed = mutable.removeOne();
			if (!removed.isEmpty()) {
				playSound(player, SoundEvents.BUNDLE_REMOVE_ONE);
				carriedAccess.set(removed);
			}
		} else if (mutable.tryInsert(carried) > 0) {
			playSound(player, SoundEvents.BUNDLE_INSERT);
		}

		bundle.set(EchoComponents.ECHO_BUNDLE_CONTENTS, mutable.toImmutable());
		return true;
	}

	/** Sneak-use opens the bundle's own screen; any other use empties it, as a vanilla bundle does. */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack bundle = player.getItemInHand(hand);
		if (player.isSecondaryUseActive()) {
			if (!level.isClientSide()) {
				int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : Inventory.SLOT_OFFHAND;
				player.openMenu(new EchoBundleMenuProvider(slot, bundle.getHoverName()));
			}
			return InteractionResultHolder.sidedSuccess(bundle, level.isClientSide());
		}
		if (dropContents(bundle, player)) {
			playSound(player, SoundEvents.BUNDLE_DROP_CONTENTS);
			player.awardStat(Stats.ITEM_USED.get(this));
			return InteractionResultHolder.sidedSuccess(bundle, level.isClientSide());
		}
		return InteractionResultHolder.fail(bundle);
	}

	/**
	 * Place from the bundle: using it on a block places the block most recently put in, as if it
	 * were in the hand. A creative player places without using any up, as with a block in hand.
	 * With nothing placeable inside, or with the config switch off, the use falls through to
	 * {@link #use}.
	 */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack bundle = context.getItemInHand();
		EchoBundleContents contents = bundle.get(EchoComponents.ECHO_BUNDLE_CONTENTS);
		if (!EchoConfig.get().bundlePlace() || contents == null || bundle.getCount() != 1) {
			return InteractionResult.PASS;
		}
		Optional<ItemStack> block = contents.mostRecent(entry -> entry.getItem() instanceof BlockItem);
		if (block.isEmpty()) {
			return InteractionResult.PASS;
		}

		// A copy of one, so a failed placement changes nothing and the real hand is never emptied.
		ItemStack placing = block.get().copyWithCount(1);
		BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
				context.getClickedPos(), context.isInside());
		InteractionResult result = ((BlockItem) placing.getItem())
				.place(new BlockPlaceContext(context.getLevel(), context.getPlayer(), context.getHand(), placing, hit));
		if (placing.isEmpty()) {
			EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(contents);
			mutable.take(block.get(), 1);
			bundle.set(EchoComponents.ECHO_BUNDLE_CONTENTS, mutable.toImmutable());
		}
		return result;
	}

	/**
	 * Empties up to {@link #MAX_ENTRIES_DROPPED_AT_ONCE} entries, most recent first; using the
	 * bundle again drops the next lot. Deterministic, so the client's prediction matches.
	 */
	private static boolean dropContents(ItemStack bundle, Player player) {
		EchoBundleContents contents = bundle.get(EchoComponents.ECHO_BUNDLE_CONTENTS);
		if (contents == null || contents.isEmpty()) {
			return false;
		}

		EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(contents);
		List<ItemStack> dropped = new ArrayList<>();
		while (dropped.size() < MAX_ENTRIES_DROPPED_AT_ONCE) {
			ItemStack removed = mutable.removeOne();
			if (removed.isEmpty()) {
				break;
			}
			dropped.add(removed);
		}
		bundle.set(EchoComponents.ECHO_BUNDLE_CONTENTS, mutable.toImmutable());

		if (player instanceof ServerPlayer) {
			dropped.forEach(stack -> player.drop(stack, true));
		}
		return true;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return contentsOf(stack).weight().compareTo(Fraction.ZERO) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.min(1 + Mth.mulAndTruncate(contentsOf(stack).weight(), 12), 13);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return BAR_COLOR;
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		if (stack.has(DataComponents.HIDE_TOOLTIP)
				|| stack.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP)) {
			return Optional.empty();
		}
		return Optional.ofNullable(stack.get(EchoComponents.ECHO_BUNDLE_CONTENTS)).map(EchoBundleTooltip::new);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
		EchoBundleContents contents = stack.get(EchoComponents.ECHO_BUNDLE_CONTENTS);
		if (contents != null) {
			int fullness = Mth.mulAndTruncate(contents.weight(), TOOLTIP_MAX_WEIGHT);
			lines.add(Component.translatable("item.minecraft.bundle.fullness", fullness, TOOLTIP_MAX_WEIGHT)
					.withStyle(ChatFormatting.GRAY));
		}
		EchoBundleSettings settings = settingsOf(stack);
		settings.category().ifPresent(category -> lines.add(
				Component.translatable("item.echostorage.echo_bundle.category", category.displayName()).withStyle(ChatFormatting.GRAY)));
		if (settings.vacuum()) {
			lines.add(Component.translatable("item.echostorage.echo_bundle.vacuums").withStyle(ChatFormatting.GRAY));
		}
	}

	/** Staggered through {@link EchoItems#SPILL}: a destroyed bundle may hold 256 entries. */
	@Override
	public void onDestroyed(ItemEntity entity) {
		EchoBundleContents contents = entity.getItem().get(EchoComponents.ECHO_BUNDLE_CONTENTS);
		if (contents == null) {
			return;
		}
		entity.getItem().set(EchoComponents.ECHO_BUNDLE_CONTENTS, EchoBundleContents.EMPTY);

		Level level = entity.level();
		if (level.isClientSide) {
			return;
		}
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		List<Runnable> spills = new ArrayList<>();
		for (ItemStack stack : contents.items()) {
			ItemStack copy = stack.copy();
			spills.add(() -> level.addFreshEntity(new ItemEntity(level, x, y, z, copy)));
		}
		EchoItems.SPILL.add(spills);
	}

	private static void playSound(Entity entity, SoundEvent sound) {
		entity.playSound(sound, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
	}
}
