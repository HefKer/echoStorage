package dev.hefker.echostorage.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.hefker.echostorage.VanillaBootstrap;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EchoBundleContentsTest {
	@BeforeAll
	static void bootstrap() {
		VanillaBootstrap.run();
	}

	@Test
	void holdsFourStacksOfAnItemThatStacksToSixtyFour() {
		EchoBundleContents.Mutable bundle = new EchoBundleContents.Mutable(EchoBundleContents.EMPTY);
		ItemStack cobblestone = new ItemStack(Items.COBBLESTONE, 64);

		for (int i = 0; i < 4; i++) {
			assertEquals(64, bundle.tryInsert(cobblestone.copy()));
		}
		assertEquals(0, bundle.tryInsert(new ItemStack(Items.COBBLESTONE)));
	}

	@Test
	void holdsFourStacksOfAnItemThatStacksToSixteen() {
		assertEquals(64, capacityFor(new ItemStack(Items.ENDER_PEARL)));
	}

	@Test
	void holdsFourUnstackableItems() {
		assertEquals(4, capacityFor(new ItemStack(Items.DIAMOND_SWORD)));
	}

	@Test
	void holdsFourHivesWithBeesInThem() {
		ItemStack hive = new ItemStack(Items.BEEHIVE);
		hive.set(DataComponents.BEES, List.of(BeehiveBlockEntity.Occupant.create(0)));

		assertEquals(4, capacityFor(hive));
	}

	@Test
	void mixedItemsShareOneBudget() {
		EchoBundleContents.Mutable bundle = new EchoBundleContents.Mutable(EchoBundleContents.EMPTY);
		bundle.tryInsert(new ItemStack(Items.COBBLESTONE, 64));
		bundle.tryInsert(new ItemStack(Items.ENDER_PEARL, 16));

		assertEquals(32, bundle.tryInsert(new ItemStack(Items.ENDER_PEARL, 99)), "half the budget is left");
	}

	/** How many of a stack fit in an empty bundle, inserting one at a time. */
	private static int capacityFor(ItemStack one) {
		EchoBundleContents.Mutable bundle = new EchoBundleContents.Mutable(EchoBundleContents.EMPTY);
		int inserted = 0;
		while (bundle.tryInsert(one.copy()) == 1) {
			inserted++;
		}
		return inserted;
	}

	@Test
	void itemsAddedOneAtATimeMergeIntoFullStacksNotOneOversizedEntry() {
		EchoBundleContents full = fill(new ItemStack(Items.COBBLESTONE), 256);

		assertEquals(4, full.size());
		full.items().forEach(entry -> assertEquals(64, entry.getCount()));
	}

	@Test
	void aFullBundleOfOneItemSurvivesSavingAndLoading() {
		EchoBundleContents full = fill(new ItemStack(Items.COBBLESTONE), 256);

		Tag saved = EchoBundleContents.CODEC.encodeStart(NbtOps.INSTANCE, full).getOrThrow();
		EchoBundleContents loaded = EchoBundleContents.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();

		assertEquals(full, loaded);
		assertEquals(Fraction.ONE, loaded.weight());
	}

	@Test
	void removingOneGivesBackTheLastEntryPutInAndFreesItsRoom() {
		EchoBundleContents.Mutable bundle = new EchoBundleContents.Mutable(fill(new ItemStack(Items.COBBLESTONE, 64), 4));
		assertEquals(0, bundle.tryInsert(new ItemStack(Items.COBBLESTONE)), "precondition: full");

		ItemStack removed = bundle.removeOne();

		assertTrue(ItemStack.matches(new ItemStack(Items.COBBLESTONE, 64), removed));
		assertEquals(64, bundle.tryInsert(new ItemStack(Items.COBBLESTONE, 64)));
	}

	@Test
	void removingFromAnEmptyBundleGivesNothing() {
		assertTrue(new EchoBundleContents.Mutable(EchoBundleContents.EMPTY).removeOne().isEmpty());
	}

	@Test
	void refusesAVanillaBundleEvenAnEmptyOne() {
		assertRefuses(new ItemStack(Items.BUNDLE));
	}

	@Test
	void refusesAnotherEchoBundle() {
		ItemStack echoBundle = new ItemStack(Items.STICK);
		echoBundle.set(EchoComponents.ECHO_BUNDLE_CONTENTS, EchoBundleContents.EMPTY);

		assertRefuses(echoBundle);
	}

	@Test
	void refusesWhatVanillaKeepsOutOfContainerItems() {
		assertRefuses(new ItemStack(Items.SHULKER_BOX));
	}

	private static void assertRefuses(ItemStack stack) {
		EchoBundleContents.Mutable bundle = new EchoBundleContents.Mutable(EchoBundleContents.EMPTY);

		assertEquals(0, bundle.tryInsert(stack));
		assertEquals(1, stack.getCount(), "a refused stack is left untouched");
		assertTrue(bundle.toImmutable().isEmpty());
	}

	private static EchoBundleContents fill(ItemStack stack, int times) {
		EchoBundleContents.Mutable bundle = new EchoBundleContents.Mutable(EchoBundleContents.EMPTY);
		for (int i = 0; i < times; i++) {
			bundle.tryInsert(stack.copy());
		}
		return bundle.toImmutable();
	}
}
