# Categories are resolved in layers, not from item tags alone

The design assumed Minecraft's item tags could supply preset storage categories
(`wood`, `stone`, `nature`, `soil`, ores, food). Checked against the 1.21.1 server jar
(147 vanilla item tags) and `fabric-convention-tags-v2` 2.12.0 (272 `c:` item tags),
that assumption is mostly false: there is no `wood`, `nature` or `soil` tag in either
namespace; `c:stones` is six base-ingredient items, not stone-like building blocks; and
roughly 29% of vanilla items fall into no useful tag at all — concentrated in decorative
stone, the copper family, corals, redstone components and light sources, which is exactly
what players most want binned. Only `c:foods` and `c:ores` work as named.

So a Category is resolved in layers: an explicit tag where a real one exists, then
predicates over item classes and data components (Quark's approach — `DataComponents.FOOD`
beats any food tag, since it catches modded foods with no cooperation from their authors),
then curated unions for things like wood, then mod id as the final fallback. Every Category
is also backed by a datapack-overridable `echostorage:category/<name>` tag, because a large
modpack will categorize badly no matter what ships, and the override hook is the difference
between a mod pack authors fix and one they uninstall.

## Consequences

v1 ships only the Categories that can be done well from tags. Wood, stone, nature,
lighting and redstone wait for their predicate layer rather than shipping wrong.
