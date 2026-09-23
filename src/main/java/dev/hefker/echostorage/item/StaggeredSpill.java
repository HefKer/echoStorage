package dev.hefker.echostorage.item;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

/**
 * Spawns spilled contents a fixed number per tick instead of all at once. A vanilla bundle
 * can burst at most 64 item entities; an Echo Bundle could burst 256.
 *
 * <p>Each pending spill is the action that spawns one entry, so this knows nothing about
 * levels or entities. Server thread only.
 */
public final class StaggeredSpill {
	private final int perTick;
	private final Deque<Runnable> pending = new ArrayDeque<>();

	public StaggeredSpill(int perTick) {
		this.perTick = perTick;
	}

	public void add(Collection<? extends Runnable> spills) {
		pending.addAll(spills);
	}

	public void tick() {
		for (int i = 0; i < perTick && !pending.isEmpty(); i++) {
			pending.poll().run();
		}
	}

	/** Spills everything still pending, for when there will be no next tick. */
	public void flush() {
		while (!pending.isEmpty()) {
			pending.poll().run();
		}
	}
}
