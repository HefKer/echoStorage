package dev.hefker.echostorage.network;

import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import dev.hefker.echostorage.EchoStorage;
import dev.hefker.echostorage.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * The round trip that proves the networking seam works end to end: the server pings a player,
 * the client answers, and the server reports how long it took.
 *
 * <p>It exists to keep the seam exercised while there are no real payloads yet, and to be the
 * worked example the first real payload is copied from. Note that it handles payloads without
 * importing a single networking API — sending is {@link Net}'s job.
 */
public final class SeamProbe {
	/** How many characters of a client-supplied loader name we are willing to repeat. */
	private static final int MAX_PLATFORM_NAME = 32;

	private static final OutstandingPings PINGS = new OutstandingPings();

	private SeamProbe() {
	}

	/** Starts a round trip for {@code player}, replacing any answer still outstanding. */
	public static void ping(ServerPlayer player) {
		long nonce = ThreadLocalRandom.current().nextLong();
		PINGS.record(player.getUUID(), nonce, System.nanoTime());
		Net.sendTo(player, new SeamPingPayload(nonce));
	}

	/**
	 * Handles the client's answer. Runs on the server thread.
	 *
	 * <p>Everything in the payload came from the client, so the nonce is matched against a
	 * ping we actually sent and the loader name is trimmed before it goes anywhere near chat.
	 */
	public static void onPong(ServerPlayer player, SeamPongPayload pong) {
		UUID id = player.getUUID();
		OptionalLong elapsedNanos = PINGS.elapsedNanosFor(id, pong.nonce(), System.nanoTime());

		if (elapsedNanos.isEmpty()) {
			EchoStorage.LOGGER.debug("ignoring an unsolicited seam pong from {}", id);
			return;
		}

		String message = "seam round trip: %s client -> %s server in %dµs"
				.formatted(trimmed(pong.platformName()), Services.PLATFORM.platformName(),
						elapsedNanos.getAsLong() / 1000L);

		EchoStorage.LOGGER.info("{} ({})", message, player.getGameProfile().getName());
		player.sendSystemMessage(Component.literal(message));
	}

	private static String trimmed(String platformName) {
		return platformName.length() <= MAX_PLATFORM_NAME
				? platformName
				: platformName.substring(0, MAX_PLATFORM_NAME);
	}
}
