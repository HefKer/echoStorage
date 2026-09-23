package dev.hefker.echostorage.client.tooltip;

/**
 * The grid an Echo Bundle tooltip draws: vanilla's layout up to vanilla's largest grid, then
 * capped there. Past the cap the last cell stands for the entries not drawn.
 *
 * <p>Vanilla always draws one cell more than it has entries (the empty or blocked slot), so
 * {@code cells = columns * rows} is at least {@code shownEntries + 1} either way.
 *
 * @param hiddenEntries entries the grid has no room for; zero unless capped
 */
public record EchoBundleTooltipLayout(int columns, int rows, int shownEntries, int hiddenEntries) {
	/**
	 * Vanilla's largest bundle grid: 64 entries plus its trailing cell lay out 9 by 8, which is
	 * known to fit on screen. Vanilla's own formula stays within it up to 71 entries.
	 */
	static final int MAX_COLUMNS = 9;
	static final int MAX_ROWS = 8;

	public static EchoBundleTooltipLayout forEntries(int entries) {
		int maxCells = MAX_COLUMNS * MAX_ROWS;
		if (entries < maxCells) {
			int cells = entries + 1;
			int columns = Math.max(2, (int) Math.ceil(Math.sqrt(cells)));
			return new EchoBundleTooltipLayout(columns, Math.ceilDiv(cells, columns), entries, 0);
		}

		int shown = maxCells - 1;
		return new EchoBundleTooltipLayout(MAX_COLUMNS, MAX_ROWS, shown, entries - shown);
	}

	public boolean capped() {
		return hiddenEntries > 0;
	}
}
