package dev.hefker.echostorage.config;

/**
 * The client's side of config sync: while connected to a server, {@link EchoConfig#get()}
 * answers with the server's switches, and on leaving it answers with the client's own file
 * again.
 *
 * <p>The local config is captured when the first server's config arrives, not at load, so
 * this needs no hook into startup. Singleplayer goes through the same path: the integrated
 * server sends the values both sides already hold, and nothing changes.
 *
 * <p>It lives in the common source set, not the client one, only so it can be unit-tested
 * without a client classpath; nothing on the server calls it.
 */
public final class SessionConfig {
	/** The client's own config while a server's is applied; {@code null} when none is. */
	private static EchoConfig local;

	private SessionConfig() {
	}

	/** Makes the server's config the one {@link EchoConfig#get()} returns until {@link #restoreLocal()}. */
	public static synchronized void applyRemote(EchoConfig serverConfig) {
		if (local == null) {
			local = EchoConfig.get();
		}
		EchoConfig.set(serverConfig);
	}

	/** Puts the client's own config back. Does nothing if no server's config was applied. */
	public static synchronized void restoreLocal() {
		if (local != null) {
			EchoConfig.set(local);
			local = null;
		}
	}
}
