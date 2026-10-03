package dev.hefker.echostorage.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.hefker.echostorage.CodecRoundTrip;
import dev.hefker.echostorage.VanillaBootstrap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SelectedItemTest {
	@BeforeAll
	static void bootstrap() {
		VanillaBootstrap.run();
	}

	// --- Stops -----------------------------------------------------------------------------

	@Test
	void logsOverSeveralEntriesAreOneStopAndTwoDifferentBooksAreTwoNewestFirst() {
		ItemStack mending = book("mending", 1);
		ItemStack unbreaking = book("unbreaking", 3);
		// Entries, newest first: 56 logs, Unbreaking, Mending, 64 logs.
		EchoBundleContents contents = contentsOf(new ItemStack(Items.OAK_LOG, 100), mending, unbreaking,
				new ItemStack(Items.OAK_LOG, 20));
		assertEquals(4, contents.size(), "precondition: the logs span two entries either side of the books");

		List<ItemStack> stops = contents.stops();

		assertStops(List.of(new ItemStack(Items.OAK_LOG), unbreaking, mending), stops);
		stops.forEach(stop -> assertEquals(1, stop.getCount(), "a Stop is a one-count template"));
		assertEquals(120, contents.countOf(new ItemStack(Items.OAK_LOG)));
	}

	@Test
	void twoIdenticalNonStackablesAreOneStopOfTwo() {
		ItemStack mending = book("mending", 1);
		ItemStack unbreaking = book("unbreaking", 3);
		EchoBundleContents contents = contentsOf(unbreaking, mending, unbreaking.copy());
		assertEquals(3, contents.size(), "precondition: one entry per book");

		assertStops(List.of(unbreaking, mending), contents.stops());
		assertEquals(2, contents.countOf(unbreaking));
	}

	@Test
	void anEmptyBundleHasNoStops() {
		assertTrue(EchoBundleContents.EMPTY.stops().isEmpty());
	}

	// --- set and kept ----------------------------------------------------------------------

	@Test
	void theFirstItemIntoAnEmptyBundleBecomesTheSelectedItem() {
		ItemStack bundle = emptyBundle();

		put(bundle, new ItemStack(Items.STONE, 5));

		assertSelected(Items.STONE, bundle);
	}

	@Test
	void aLaterInsertOfADifferentItemLeavesTheSelectedItem() {
		ItemStack bundle = emptyBundle();
		put(bundle, new ItemStack(Items.STONE, 5));

		put(bundle, new ItemStack(Items.DIRT, 5));
		put(bundle, new ItemStack(Items.BREAD, 5));

		assertSelected(Items.STONE, bundle);
	}

	@Test
	void aBundleSavedBeforeTheSelectedItemExistedResolvesToTheFirstStop() {
		ItemStack bundle = savedBeforeTheSelectedItem(new ItemStack(Items.STONE, 5), new ItemStack(Items.DIRT, 5));

		assertSelected(Items.DIRT, bundle);
	}

	@Test
	void aBundleSavedBeforeTheSelectedItemExistedKeepsItsFirstStopSelectedWhenSomethingNewGoesIn() {
		ItemStack bundle = savedBeforeTheSelectedItem(new ItemStack(Items.STONE, 5), new ItemStack(Items.DIRT, 5));

		put(bundle, new ItemStack(Items.BREAD, 5));

		assertSelected(Items.DIRT, bundle);
	}

	@Test
	void aStoredSelectedItemNoLongerInsideCountsAsNone() {
		ItemStack bundle = bundleOf(new ItemStack(Items.STONE, 5), new ItemStack(Items.DIRT, 5));
		bundle.set(EchoComponents.ECHO_BUNDLE_SELECTED_ITEM, new SelectedItem(new ItemStack(Items.DIAMOND)));

		assertSelected(Items.DIRT, bundle);
	}

	@Test
	void anEmptyBundleHasNoSelectedItem() {
		ItemStack bundle = emptyBundle();
		put(bundle, new ItemStack(Items.STONE, 5));

		EchoBundleItem.setContents(bundle, EchoBundleContents.EMPTY);

		assertTrue(EchoBundleItem.selectedItemOf(bundle).isEmpty());
		assertFalse(bundle.has(EchoComponents.ECHO_BUNDLE_SELECTED_ITEM), "an empty bundle kept its Selected item");
	}

	@Test
	void selectingAnItemInsideMakesItTheSelectedItemAndAnythingElseChangesNothing() {
		ItemStack bundle = bundleOf(new ItemStack(Items.STONE, 5), new ItemStack(Items.DIRT, 5));

		assertTrue(EchoBundleItem.select(bundle, new ItemStack(Items.STONE, 3)));
		assertSelected(Items.STONE, bundle);
		assertFalse(EchoBundleItem.select(bundle, new ItemStack(Items.DIAMOND)));
		assertSelected(Items.STONE, bundle);
	}

	// --- stepping --------------------------------------------------------------------------

	@Test
	void steppingForwardAndBackWrapsAtBothEnds() {
		// Stops, newest first: bread, dirt, stone.
		EchoBundleContents contents = contentsOf(new ItemStack(Items.STONE), new ItemStack(Items.DIRT), new ItemStack(Items.BREAD));
		SelectedItem bread = new SelectedItem(new ItemStack(Items.BREAD));
		SelectedItem stone = new SelectedItem(new ItemStack(Items.STONE));

		assertEquals(new SelectedItem(new ItemStack(Items.DIRT)), contents.stepped(bread, 1));
		assertEquals(bread, contents.stepped(stone, 1), "forward from the last Stop wraps to the first");
		assertEquals(stone, contents.stepped(bread, -1), "back from the first Stop wraps to the last");
		assertEquals(stone, contents.stepped(SelectedItem.NONE, -1), "no stored Selected item steps from the first Stop");
	}

	@Test
	void steppingASingleStopStaysOnIt() {
		EchoBundleContents contents = contentsOf(new ItemStack(Items.STONE, 100));

		assertEquals(new SelectedItem(new ItemStack(Items.STONE)), contents.stepped(SelectedItem.NONE, 1));
	}

	// --- running out -----------------------------------------------------------------------

	@Test
	void whenTheSelectedItemRunsOutItMovesToTheStopAfterIt() {
		// Stops, newest first: bread, dirt, stone.
		ItemStack bundle = bundleOf(new ItemStack(Items.STONE), new ItemStack(Items.DIRT), new ItemStack(Items.BREAD));
		EchoBundleItem.select(bundle, new ItemStack(Items.DIRT));

		take(bundle, new ItemStack(Items.DIRT));

		assertSelected(Items.STONE, bundle);
	}

	@Test
	void whenTheLastStopRunsOutTheSelectedItemWrapsToTheFirst() {
		ItemStack bundle = bundleOf(new ItemStack(Items.STONE), new ItemStack(Items.DIRT), new ItemStack(Items.BREAD));
		EchoBundleItem.select(bundle, new ItemStack(Items.STONE));

		take(bundle, new ItemStack(Items.STONE));

		assertSelected(Items.BREAD, bundle);
	}

	@Test
	void takingSomeButNotAllOfTheSelectedItemKeepsIt() {
		ItemStack bundle = bundleOf(new ItemStack(Items.STONE, 5), new ItemStack(Items.DIRT));
		EchoBundleItem.select(bundle, new ItemStack(Items.STONE));

		EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(contentsIn(bundle));
		mutable.take(new ItemStack(Items.STONE), 4);
		EchoBundleItem.setContents(bundle, mutable.toImmutable());

		assertSelected(Items.STONE, bundle);
	}

	@Test
	void emptyingTheBundleClearsTheSelectedItem() {
		ItemStack bundle = bundleOf(new ItemStack(Items.STONE));

		take(bundle, new ItemStack(Items.STONE));

		assertTrue(EchoBundleItem.selectedItemOf(bundle).isEmpty());
		assertFalse(bundle.has(EchoComponents.ECHO_BUNDLE_SELECTED_ITEM));
	}

	@Test
	void takingOutOneRemovesAnEntryOfTheSelectedItemNotTheNewest() {
		EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(
				contentsOf(new ItemStack(Items.STONE, 64), new ItemStack(Items.STONE, 10), new ItemStack(Items.DIRT, 3)));

		ItemStack removed = mutable.removeOne(new ItemStack(Items.STONE));

		assertTrue(ItemStack.matches(new ItemStack(Items.STONE, 10), removed), "the newest stone entry: " + removed);
		assertEquals(64, mutable.toImmutable().countOf(new ItemStack(Items.STONE)));
		assertEquals(3, mutable.toImmutable().countOf(new ItemStack(Items.DIRT)));
	}

	// --- codecs ----------------------------------------------------------------------------

	@Test
	void theSelectedItemSurvivesASaveAndTheNetwork() {
		SelectedItem stone = new SelectedItem(new ItemStack(Items.STONE));
		// The static registries, which an item on the wire is named against.
		RegistryAccess builtIn = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

		assertEquals(stone, load(save(stone)));
		assertEquals(stone, CodecRoundTrip.of(SelectedItem.STREAM_CODEC, stone, builtIn));
		assertEquals(SelectedItem.NONE, CodecRoundTrip.of(SelectedItem.STREAM_CODEC, SelectedItem.NONE, builtIn));
	}

	@Test
	void aSelectedItemIsAlwaysOneOfItsItem() {
		assertEquals(new SelectedItem(new ItemStack(Items.STONE)), new SelectedItem(new ItemStack(Items.STONE, 40)));
	}

	@Test
	void aGarbageOrUnknownSelectedItemLoadsAsNoneRatherThanFailing() {
		assertEquals(SelectedItem.NONE, load("{}"));
		assertEquals(SelectedItem.NONE, load("{\"item\": 5}"));
		assertEquals(SelectedItem.NONE, load("{\"item\": {\"id\": \"othermod:gone\"}}"));
	}

	// --- helpers ---------------------------------------------------------------------------


	/** One holder per name, so two books of the same enchantment stack together. */
	private static final Map<String, Holder<Enchantment>> HOLDERS = new HashMap<>();

	/** An enchanted book holding one enchantment, which no registry is needed to tell apart. */
	private static ItemStack book(String enchantment, int level) {
		ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace(enchantment));
		Holder<Enchantment> holder = HOLDERS.computeIfAbsent(enchantment,
				name -> Holder.Reference.createStandAlone(new HolderOwner<>() {
				}, key));
		ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		enchantments.set(holder, level);
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		book.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
		return book;
	}

	private static EchoBundleContents contentsOf(ItemStack... stacks) {
		EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(EchoBundleContents.EMPTY);
		for (ItemStack stack : stacks) {
			mutable.tryInsert(stack.copy());
		}
		return mutable.toImmutable();
	}

	private static EchoBundleContents contentsIn(ItemStack bundle) {
		return bundle.getOrDefault(EchoComponents.ECHO_BUNDLE_CONTENTS, EchoBundleContents.EMPTY);
	}

	private static ItemStack emptyBundle() {
		ItemStack bundle = new ItemStack(Items.STICK);
		EchoBundleItem.setContents(bundle, EchoBundleContents.EMPTY);
		return bundle;
	}

	/**
	 * A bundle as one saved before the Selected item existed (#31) loads: contents and no stored
	 * Selected item. The one place tests write the contents without {@link EchoBundleItem#setContents}.
	 */
	private static ItemStack savedBeforeTheSelectedItem(ItemStack... stacks) {
		ItemStack bundle = new ItemStack(Items.STICK);
		bundle.set(EchoComponents.ECHO_BUNDLE_CONTENTS, contentsOf(stacks));
		return bundle;
	}

	/** A bundle filled one stack at a time, each through the write path a real insert takes. */
	private static ItemStack bundleOf(ItemStack... stacks) {
		ItemStack bundle = emptyBundle();
		for (ItemStack stack : stacks) {
			put(bundle, stack);
		}
		return bundle;
	}

	private static void put(ItemStack bundle, ItemStack stack) {
		EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(contentsIn(bundle));
		mutable.tryInsert(stack.copy());
		EchoBundleItem.setContents(bundle, mutable.toImmutable());
	}

	/** Takes every one of {@code like} out of the bundle. */
	private static void take(ItemStack bundle, ItemStack like) {
		EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(contentsIn(bundle));
		mutable.take(like, Integer.MAX_VALUE);
		EchoBundleItem.setContents(bundle, mutable.toImmutable());
	}

	private static void assertSelected(Item expected, ItemStack bundle) {
		ItemStack selected = EchoBundleItem.selectedItemOf(bundle);
		assertTrue(selected.is(expected), "selected " + selected + ", expected " + expected);
	}

	private static void assertStops(List<ItemStack> expected, List<ItemStack> actual) {
		assertEquals(expected.size(), actual.size(), "Stops: " + actual);
		for (int i = 0; i < expected.size(); i++) {
			assertTrue(ItemStack.isSameItemSameComponents(expected.get(i), actual.get(i)),
					"Stop " + i + " is " + actual.get(i) + ", expected " + expected.get(i));
		}
	}

	private static String save(SelectedItem selected) {
		return SelectedItem.CODEC.encodeStart(JsonOps.INSTANCE, selected).getOrThrow().toString();
	}

	private static SelectedItem load(String json) {
		return SelectedItem.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
	}
}
