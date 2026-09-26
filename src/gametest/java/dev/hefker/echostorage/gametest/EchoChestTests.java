package dev.hefker.echostorage.gametest;

import java.util.List;

import dev.hefker.echostorage.block.EchoBlocks;
import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.item.EchoBundleContents;
import dev.hefker.echostorage.item.EchoComponents;
import dev.hefker.echostorage.item.EchoItems;
import dev.hefker.echostorage.menu.EchoChestMenu;
import dev.hefker.echostorage.menu.EchoChestMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * Helpers and constants the Echo Chest, Echo Bundle and Echo Interface game tests share.
 *
 * <p>GameTestHelper.assertValueEqual takes (actual, expected, what).
 */
final class EchoChestTests {
	static final BlockPos CHEST = new BlockPos(1, 1, 1);
	static final BlockPos NEIGHBOUR = new BlockPos(2, 1, 1);
	/** The first slot of the player's main inventory, above the hotbar. */
	static final int FIRST_MAIN_INVENTORY_SLOT = 9;
	/** The same slot in the Echo Chest menu, which lists the chest's 27 first. */
	static final int FIRST_PLAYER_MENU_SLOT = EchoChestBlockEntity.SLOTS;

	private EchoChestTests() {
	}

	static EchoChestBlockEntity placeChest(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, EchoBlocks.ECHO_CHEST);
		return chestAt(helper, pos);
	}

	static EchoChestBlockEntity chestAt(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockEntity(pos);
	}

	/** Places {@code stack} at {@code pos} the way a player would, through the block item. */
	static void placeFromItem(GameTestHelper helper, ItemStack stack, BlockPos pos) {
		Player player = holding(helper, stack);
		helper.placeAt(player, player.getMainHandItem(), pos.below(), Direction.UP);
		helper.assertBlockPresent(EchoBlocks.ECHO_CHEST, pos);
	}

	/** Placement reads the stack from the player's hand, not from the stack it is handed. */
	static Player holding(GameTestHelper helper, ItemStack stack) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return player;
	}

	/** Breaks the block as a player's tool would: loot table drops and all. */
	static void breakChest(GameTestHelper helper, BlockPos pos) {
		helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
	}

	static ItemStack droppedChest(GameTestHelper helper) {
		List<ItemStack> chests = helper.getEntities(EntityType.ITEM).stream()
				.map(ItemEntity::getItem)
				.filter(stack -> stack.is(EchoItems.ECHO_CHEST))
				.toList();
		helper.assertValueEqual(chests.size(), 1, "dropped Echo Chests");
		return chests.getFirst();
	}

	/** Opens {@code chest}'s menu for a player standing above {@link #CHEST}, wherever the chest is. */
	static ServerPlayer openedBy(GameTestHelper helper, EchoChestBlockEntity chest) {
		@SuppressWarnings("removal")
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.moveTo(helper.absoluteVec(CHEST.getCenter()).add(0, 1, 0));
		player.openMenu(new EchoChestMenuProvider(chest));
		helper.assertTrue(player.containerMenu instanceof EchoChestMenu, "the Echo Chest menu did not open");
		return player;
	}

	static EchoChestMenu menu(ServerPlayer player) {
		return (EchoChestMenu) player.containerMenu;
	}

	static ItemStack bundleOf(ItemStack... contents) {
		EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(EchoBundleContents.EMPTY);
		for (ItemStack stack : contents) {
			mutable.tryInsert(stack.copy());
		}
		ItemStack bundle = new ItemStack(EchoItems.ECHO_BUNDLE);
		bundle.set(EchoComponents.ECHO_BUNDLE_CONTENTS, mutable.toImmutable());
		return bundle;
	}

	static void assertStack(GameTestHelper helper, ItemStack expected, ItemStack actual, String what) {
		helper.assertTrue(ItemStack.matches(expected, actual), what + ": expected " + expected + ", got " + actual);
	}
}
