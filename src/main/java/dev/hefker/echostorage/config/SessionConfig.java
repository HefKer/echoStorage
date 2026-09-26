package dev.hefker.echostorage.config;

/**
 * The client's side of config sync: while connected to a server, {@link EchoConfig#get()}
 * answers with the server's switches, and on leaving it answers with the client's own file
 * again.
 *
 * <p>The local config is captured when the first server's config arrives, not at load, so
 * this needs no hook into startup. Singleplayer goes through the same path: the integrated
 * server sends the values both sides already hold, and nothing changes.
 */
public final class SessionConfig {
	/** The client's own config while a server's is applied; {@code null} when none is. */
	private static EchoConfig local;

	private SessionConfig() {
	}

	/** Makes the server's config the one {@link EchoConfig#get()} returns until {@link #restoreLocal()}. */
	public static synchronized void applyRemote(EchoConfig servers) {
		if (local == null) {
			local = EchoConfig.get();
		}
		EchoConfig.set(servers);
	}

	/** Puts the client's own config back. Does nothing if no server's config was applied. */
	public static synchronized void restoreLocal() {
		if (local != null) {
			EchoConfig.set(local);
			local = null;
		}
	}
}
