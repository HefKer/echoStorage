package dev.hefker.echostorage.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/**
 * A destroyed Echo Bundle can hold four times the entries of a vanilla one, so its contents
 * come out over several ticks rather than as one burst of item entities.
 */
class StaggeredSpillTest {
	private final StaggeredSpill spill = new StaggeredSpill(64);
	private final List<Integer> spilled = new ArrayList<>();
	private int numbered;

	@Test
	void nothingSpillsUntilTheTick() {
		spill.add(entries(10));
		assertEquals(0, spilled.size());
	}

	@Test
	void aSmallSpillComesOutInOneTick() {
		spill.add(entries(10));
		spill.tick();
		assertEquals(10, spilled.size());
	}

	@Test
	void aLargeSpillComesOutAFixedNumberPerTickInOrder() {
		spill.add(entries(150));

		spill.tick();
		assertEquals(64, spilled.size());
		spill.tick();
		assertEquals(128, spilled.size());
		spill.tick();
		assertEquals(150, spilled.size());
		assertEquals(IntStream.range(0, 150).boxed().toList(), spilled);
	}

	@Test
	void separateBundlesShareOneTicksBudget() {
		spill.add(entries(40));
		spill.add(entries(40));

		spill.tick();
		assertEquals(64, spilled.size());
	}

	@Test
	void flushingSpillsEverythingLeftSoAStoppingServerLosesNothing() {
		spill.add(entries(150));
		spill.tick();

		spill.flush();
		assertEquals(150, spilled.size());
		spill.tick();
		assertEquals(150, spilled.size());
	}

	/** Spill actions that each record their own number, numbered on from the last call. */
	private List<Runnable> entries(int count) {
		int first = numbered;
		numbered += count;
		return IntStream.range(first, numbered).<Runnable>mapToObj(i -> () -> spilled.add(i)).toList();
	}
}
