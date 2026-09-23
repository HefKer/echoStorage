# Reach is defined by sculk links, not by a radius

An Echo Interface reaches exactly the Echo Chests connected to it by a path of sculk the player
built. It is not a wireless terminal with a range setting.

The alternative was a radius: any echo chest within N blocks. We rejected it because N is an
arbitrary number with no in-world existence — it can only ever be defended in a config file. A
sculk path is visible, costs materials, can be broken by a creeper, and is understood by looking
at it. It also gives the deep-dark theming a mechanical job instead of a cosmetic one, and it
keeps the mod's spatial model to a single concept: things are linked, or they aren't.

Mechanically: a flood-fill from the interface through sculk vein and sculk block, cached in the
interface's block entity and recomputed on open and on nearby sculk change. Traversal is capped
at 128 blocks of sculk and 27 chests per interface, so the list stays one screen. Chests in
unloaded chunks appear greyed and unopenable — the interface never force-loads chunks, which
would be both the most performance-hostile thing the mod could do and the moment it stopped
being vanilla-shaped. A chest whose link is dead (broken and replaced, so its id has changed)
keeps a greyed row until dismissed rather than silently vanishing.

The global quick-stack inherits this bound rather than inventing a second one: it acts on linked
chests from the interface, or on the open chest. There is no radius anywhere in the mod.

A config option can relax linking to wireless for players who want the techy version. Its scope
is deliberately one branch in link resolution and nothing else: it changes only whether an Echo
Interface needs a sculk path to reach a chest. There is no wireless quick-stack, no wireless
bundle behaviour and no remote Category assignment. Scoped that narrowly it costs one code path;
scoped broadly it would double the test surface of the mod's most complex subsystem.

Which blocks can carry a Link is read from an `echostorage:connectors` block tag rather than
hardcoded, so pack authors can add their own and the mod can add more without touching the
traversal code. In v1 that tag holds sculk vein and sculk block. Sculk block carries a known risk: a sculk
catalyst blooming near a storage room can wire chests together unintentionally. The 128-block
traversal cap bounds how far that can spread, and the link visual below is what makes it
diagnosable rather than mysterious.

While an Echo Interface screen is open, sculk particles trace the resolved path at a low rate.
This costs nothing when the UI is closed and is what makes the link model legible — it turns
"why isn't my chest listed" from a mystery into something the player can look at, which is the
whole reason this model was preferred to a radius.
