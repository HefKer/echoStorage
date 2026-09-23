package dev.hefker.echostorage.network;

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
	private SeamProbe() {
	}

	/** Starts a round trip for {@code player}. Returns the nonce that identifies it. */
	public static long ping(ServerPlayer player) {
		long nonce = System.nanoTime();
		Net.sendTo(player, new SeamPingPayload(nonce));
		return nonce;
	}

	/** Handles the client's answer. Runs on the server thread. */
	public static void onPong(ServerPlayer player, SeamPongPayload pong) {
		long elapsedMicros = (System.nanoTime() - pong.nonce()) / 1000L;
		String message = "seam round trip: %s client -> %s server in %dµs"
				.formatted(pong.platformName(), Services.PLATFORM.platformName(), elapsedMicros);

		EchoStorage.LOGGER.info("{} ({})", message, player.getGameProfile().getName());
		player.sendSystemMessage(Component.literal(message));
	}
}
