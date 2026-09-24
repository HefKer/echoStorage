package dev.hefker.echostorage.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.category.Category;
import dev.hefker.echostorage.link.LinkedChests.Row;
import dev.hefker.echostorage.link.LinkedChests.State;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/** The rows an Echo Interface lists, kept across resolutions so no labelled chest silently vanishes. */
class LinkedChestsTest {
	private final LinkedChests rows = new LinkedChests();
	private final Map<UUID, LinkedChests.Label> labels = new HashMap<>();
	private final Set<BlockPos> unloaded = new HashSet<>();

	@Test
	void aChestTheLinkReachesIsListedWithItsNameAndCategory() {
		LinkedChest ores = chest(1, "Ores", Optional.of(Categories.ORES));

		update(true, ores);

		assertEquals(List.of(new Row(ores.id(), ores.pos(), "Ores", Optional.of(Categories.ORES), State.LINKED)), rows.rows());
	}

	@Test
	void rowsKeepTheirPlaceAndNewChestsJoinAtTheEnd() {
		LinkedChest far = chest(5, "Far", Optional.empty());
		LinkedChest near = chest(1, "Near", Optional.empty());
		update(true, far);

		update(true, near, far);

		assertEquals(List.of(far.id(), near.id()), ids());
	}

	@Test
	void aRowFollowsItsChestsNameAndCategory() {
		LinkedChest chest = chest(1, "Ores", Optional.of(Categories.ORES));
		update(true, chest);
		labels.put(chest.id(), new LinkedChests.Label("", Optional.empty()));

		update(true, chest);

		assertEquals(new Row(chest.id(), chest.pos(), "", Optional.empty(), State.LINKED), rows.rows().get(0));
	}

	@Test
	void aListedChestTheLinkNoLongerReachesStaysGreyedWithItsLastName() {
		LinkedChest chest = chest(1, "Ores", Optional.of(Categories.ORES));
		update(true, chest);

		update(true);

		assertEquals(List.of(new Row(chest.id(), chest.pos(), "Ores", Optional.of(Categories.ORES), State.LOST)), rows.rows());
	}

	@Test
	void aListedChestInAnUnloadedChunkIsGreyedAsUnloadedNotLost() {
		LinkedChest chest = chest(1, "Ores", Optional.empty());
		update(true, chest);
		unloaded.add(chest.pos());

		update(false);

		assertEquals(List.of(State.UNLOADED), states());
	}

	@Test
	void whenAnUnloadedChunkCutTheLinkShortAChestNotReachedMayStillBeLinked() {
		LinkedChest chest = chest(1, "Ores", Optional.empty());
		update(true, chest);

		update(false);

		assertEquals(List.of(State.UNLOADED), states());
	}

	@Test
	void aLostChestReachedAgainIsLinkedAgain() {
		LinkedChest chest = chest(1, "Ores", Optional.empty());
		update(true, chest);
		update(true);

		update(true, chest);

		assertEquals(List.of(State.LINKED), states());
	}

	@Test
	void aChestBrokenAndReplacedLeavesItsOldRowLostAndGetsARowOfItsOwn() {
		LinkedChest before = chest(1, "Ores", Optional.empty());
		update(true, before);
		LinkedChest after = chest(1, "", Optional.empty());

		update(true, after);

		assertEquals(List.of(before.id(), after.id()), ids());
		assertEquals(List.of(State.LOST, State.LINKED), states());
	}

	@Test
	void theListStopsAt27RowsAndAGreyedRowKeepsItsPlaceOverANewChest() {
		LinkedChest[] first = new LinkedChest[LinkedChests.MAX_ROWS];
		for (int i = 0; i < first.length; i++) {
			first[i] = chest(i, "", Optional.empty());
		}
		update(true, first);
		LinkedChest[] later = Arrays.copyOf(first, first.length);
		later[0] = chest(100, "New", Optional.empty());

		update(true, later);

		assertEquals(LinkedChests.MAX_ROWS, rows.rows().size());
		assertEquals(first[0].id(), ids().get(0), "the lost chest keeps its row until dismissed");
		assertFalse(ids().contains(later[0].id()), "no room for the new chest yet");
	}

	@Test
	void dismissingAGreyedRowTakesItOffTheList() {
		LinkedChest lost = chest(1, "Lost", Optional.empty());
		update(true, lost);
		update(true);

		assertTrue(rows.dismiss(lost.id()));

		assertEquals(List.of(), rows.rows());
	}

	@Test
	void aLinkedRowCannotBeDismissed() {
		LinkedChest chest = chest(1, "Ores", Optional.empty());
		update(true, chest);

		assertFalse(rows.dismiss(chest.id()));

		assertEquals(List.of(chest.id()), ids());
	}

	// --- helpers --------------------------------------------------------------------------

	private LinkedChest chest(int x, String name, Optional<Category> category) {
		LinkedChest chest = new LinkedChest(UUID.randomUUID(), new BlockPos(x, 0, 0));
		labels.put(chest.id(), new LinkedChests.Label(name, category));
		return chest;
	}

	private List<UUID> ids() {
		return rows.rows().stream().map(Row::id).toList();
	}

	private List<State> states() {
		return rows.rows().stream().map(Row::state).toList();
	}

	private void update(boolean complete, LinkedChest... found) {
		rows.update(new Resolution(List.of(found), List.of(), complete),
				chest -> labels.get(chest.id()),
				pos -> !unloaded.contains(pos));
	}
}
