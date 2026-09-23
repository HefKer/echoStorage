package dev.hefker.echostorage.block;

import net.minecraft.util.StringUtil;

/**
 * What a player-typed Echo Chest name becomes once the server accepts it.
 *
 * <p>The rules are the anvil's — same allowed characters, same length — because an accepted
 * name is stored as a vanilla custom name, and an anvil can rename the dropped item too.
 */
public final class EchoChestName {
	/** The anvil's limit, {@code AnvilMenu.MAX_NAME_LENGTH}. */
	public static final int MAX_LENGTH = 50;

	private EchoChestName() {
	}

	/** The name {@code typed} is accepted as; empty means the chest has no name. */
	public static String sanitize(String typed) {
		String filtered = StringUtil.filterText(typed).strip();
		return filtered.length() <= MAX_LENGTH ? filtered : filtered.substring(0, MAX_LENGTH).strip();
	}
}
