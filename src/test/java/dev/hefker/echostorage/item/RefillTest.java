package dev.hefker.echostorage.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import dev.hefker.echostorage.VanillaBootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Refilling the hand from the player's bundles after the last block in it was placed. */
class RefillTest {
	@BeforeAll
	static void bootstrap() {
		VanillaBootstrap.run();
	}

	private final SimpleContainer player = new SimpleContainer(41);

	@Test
	void pullsAFullStackOfTheSameBlockOutOfABundle() {
		player.setItem(5, TestBundles.of(EchoBundleSettings.DEFAULT, new ItemStack(Items.STONE, 64),
				new ItemStack(Items.STONE, 64), new ItemStack(Items.DIRT, 10)));

		ItemStack pulled = Refill.take(player, new ItemStack(Items.STONE));

		assertTrue(ItemStack.matches(new ItemStack(Items.STONE, 64), pulled), "pulled " + pulled);
		assertEquals(64, TestBundles.count(player.getItem(5), Items.STONE));
		assertEquals(10, TestBundles.count(player.getItem(5), Items.DIRT));
	}

	@Test
	void gathersAStackFromSeveralEntriesAndBundles() {
		player.setItem(2, TestBundles.of(EchoBundleSettings.DEFAULT, new ItemStack(Items.STONE, 20)));
		player.setItem(5, TestBundles.of(EchoBundleSettings.DEFAULT, new ItemStack(Items.STONE, 64)));

		ItemStack pulled = Refill.take(player, new ItemStack(Items.STONE));

		assertEquals(64, pulled.getCount());
		assertEquals(0, TestBundles.count(player.getItem(2), Items.STONE));
		assertEquals(20, TestBundles.count(player.getItem(5), Items.STONE));
	}

	@Test
	void pullsFromABundleWhetherOrNotItVacuums() {
		player.setItem(5, TestBundles.of(new EchoBundleSettings(Optional.empty(), true), new ItemStack(Items.STONE, 3)));

		assertEquals(3, Refill.take(player, new ItemStack(Items.STONE)).getCount());
	}

	@Test
	void onlyAnExactMatchIsPulled() {
		ItemStack named = new ItemStack(Items.STONE, 5);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Keystone"));
		player.setItem(5, TestBundles.of(EchoBundleSettings.DEFAULT, named));

		assertTrue(Refill.take(player, new ItemStack(Items.STONE)).isEmpty());
		assertEquals(5, TestBundles.count(player.getItem(5), Items.STONE));
	}

	@Test
	void nothingToPullGivesNothing() {
		player.setItem(0, new ItemStack(Items.STONE, 64));

		assertTrue(Refill.take(player, new ItemStack(Items.STONE)).isEmpty(), "loose stacks are not a bundle's to hand over");
		assertEquals(64, player.getItem(0).getCount());
	}

	@Test
	void aStackOfSeveralBundlesIsNeverTakenFrom() {
		ItemStack pair = TestBundles.of(EchoBundleSettings.DEFAULT, new ItemStack(Items.STONE, 10));
		pair.setCount(2);
		player.setItem(5, pair);

		assertTrue(Refill.take(player, new ItemStack(Items.STONE)).isEmpty());
		assertEquals(10, TestBundles.count(player.getItem(5), Items.STONE));
	}
}
