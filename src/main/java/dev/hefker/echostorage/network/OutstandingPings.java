package dev.hefker.echostorage.network;

import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pings sent but not yet answered, so a pong can be checked against a ping the server really
 * sent and timed against the server's own clock rather than the client's.
 *
 * <p>Pure bookkeeping — no game types, so it can be tested without a running server.
 */
final class OutstandingPings {
	private final Map<UUID, PendingPing> byPlayer = new ConcurrentHashMap<>();

	/** Records a ping, replacing any earlier one for that player. */
	void record(UUID player, long nonce, long sentAtNanos) {
		byPlayer.put(player, new PendingPing(nonce, sentAtNanos));
	}

	/**
	 * Consumes the outstanding ping for {@code player} and returns how long it took, or empty
	 * if no ping is outstanding or the nonce does not match — a ping answers only once.
	 */
	OptionalLong elapsedNanosFor(UUID player, long nonce, long nowNanos) {
		PendingPing pending = byPlayer.remove(player);

		if (pending == null || pending.nonce() != nonce) {
			return OptionalLong.empty();
		}

		return OptionalLong.of(nowNanos - pending.sentAtNanos());
	}

	private record PendingPing(long nonce, long sentAtNanos) {
	}
}
