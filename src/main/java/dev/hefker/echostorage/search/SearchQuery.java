package dev.hefker.echostorage.search;

import java.util.Locale;

/**
 * What a player typed into a search box, matched against names: a name matches if it contains
 * the query, ignoring case and the query's surrounding whitespace.
 *
 * <p>ADR-0002 allows search over two things only: what is already on screen, and names the
 * player typed themselves. This matches a name; the caller decides which names it is shown,
 * and must never hand it the contents of a chest that isn't open.
 */
public final class SearchQuery {
	private final String text;

	private SearchQuery(String text) {
		this.text = text;
	}

	public static SearchQuery of(String typed) {
		return new SearchQuery(fold(typed.strip()));
	}

	/** True when nothing was typed but whitespace, which every name matches. */
	public boolean isBlank() {
		return text.isEmpty();
	}

	public boolean matches(String name) {
		return fold(name).contains(text);
	}

	private static String fold(String s) {
		return s.toLowerCase(Locale.ROOT);
	}
}
