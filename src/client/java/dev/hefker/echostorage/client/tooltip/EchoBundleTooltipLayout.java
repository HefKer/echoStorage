package dev.hefker.echostorage.client.tooltip;

/**
 * The grid an Echo Bundle tooltip draws: vanilla's layout up to vanilla's largest bundle, then
 * capped there. Past the cap the last cell stands for the entries not drawn.
 *
 * <p>Vanilla always draws one cell more than it has entries (the empty or blocked slot), so
 * {@code cells = columns * rows} is at least {@code shownEntries + 1} either way.
 *
 * @param hiddenEntries entries the grid has no room for; zero unless capped
 */
public record EchoBundleTooltipLayout(int columns, int rows, int shownEntries, int hiddenEntries) {
	/** A vanilla bundle holds at most 64 entries, and that tooltip is known to fit on screen. */
	static final int MAX_CELLS = 64;

	public static EchoBundleTooltipLayout forEntries(int entries) {
		if (entries <= MAX_CELLS) {
			int cells = entries + 1;
			int columns = Math.max(2, (int) Math.ceil(Math.sqrt(cells)));
			return new EchoBundleTooltipLayout(columns, Math.ceilDiv(cells, columns), entries, 0);
		}

		int columns = (int) Math.ceil(Math.sqrt(MAX_CELLS));
		int shown = MAX_CELLS - 1;
		return new EchoBundleTooltipLayout(columns, Math.ceilDiv(MAX_CELLS, columns), shown, entries - shown);
	}

	public boolean capped() {
		return hiddenEntries > 0;
	}
}
