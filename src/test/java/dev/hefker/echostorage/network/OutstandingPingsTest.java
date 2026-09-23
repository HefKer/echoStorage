package dev.hefker.echostorage.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalLong;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** Everything in a pong comes from the client, so the server matches it against its own record. */
class OutstandingPingsTest {
	private final OutstandingPings pings = new OutstandingPings();
	private final UUID player = UUID.randomUUID();

	@Test
	void anAnsweredPingReportsElapsedTimeFromTheServersOwnClock() {
		pings.record(player, 7L, 1_000L);
		assertEquals(OptionalLong.of(4_000L), pings.elapsedNanosFor(player, 7L, 5_000L));
	}

	@Test
	void aPongNobodyAskedForIsIgnored() {
		assertTrue(pings.elapsedNanosFor(player, 7L, 5_000L).isEmpty());
	}

	@Test
	void aPongWithTheWrongNonceIsIgnored() {
		pings.record(player, 7L, 1_000L);
		assertTrue(pings.elapsedNanosFor(player, 8L, 5_000L).isEmpty());
	}

	@Test
	void aPingCanOnlyBeAnsweredOnce() {
		pings.record(player, 7L, 1_000L);
		pings.elapsedNanosFor(player, 7L, 5_000L);
		assertTrue(pings.elapsedNanosFor(player, 7L, 9_000L).isEmpty());
	}

	@Test
	void onePlayersPongCannotAnswerAnothersPing() {
		pings.record(player, 7L, 1_000L);
		assertTrue(pings.elapsedNanosFor(UUID.randomUUID(), 7L, 5_000L).isEmpty());
	}
}
