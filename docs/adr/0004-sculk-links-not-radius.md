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
chests from the interface, or on the open chest. There is no radius around an interface.

**Amended: wireless is the Echo Relay, not a config switch.** An earlier draft allowed a
`links.wireless` config option to drop the sculk path. It was removed before it shipped. With no
path, something else had to bound the list, and every candidate ("the nearest 27 loaded chests",
"all loaded chests in the dimension") came down to "whichever 27 chests within simulation distance
the interface saw first": a radius set by server settings instead of the mod, hidden, and with
nothing for the link visual to trace. A switch that makes reach free also brings back the balance
problem that made us choose sculk in the first place.

Wireless reach is instead a block the player builds. An Echo Relay is a Connector that *hears*
an Echo Chest when a player opens it within 8 blocks (a sculk sensor's range; opening it from an
interface counts), and from then on carries the Link one hop through the air to that chest while
it stays within 8 blocks with no wool in between, checked on each resolution like the sculk path.
A relay hears whether or not an interface is linked to it, and forgets what it heard when broken.
Only the last hop is wireless: relays do not hear relays, so sculk still does the long-distance
work. Chaining relays was considered and deferred; it would need a second way to join (nobody
opens a relay), and as a config option it would be the removed switch again. The relay counts
toward the 128-connector cap, and the 27-row and unloaded-chunk rules are unchanged. The radius
this ADR rejects is one around the interface; a relay's 8 blocks belong to a block in the world,
and the range comes from vanilla.

Which blocks can carry a Link is read from an `echostorage:connectors` block tag rather than
hardcoded, so pack authors can add their own and the mod can add more without touching the
traversal code. That tag holds sculk vein, sculk block and the Echo Relay. Sculk block carries a known risk: a sculk
catalyst blooming near a storage room can wire chests together unintentionally. The 128-block
traversal cap bounds how far that can spread, and the link visual below is what makes it
diagnosable rather than mysterious.

While an Echo Interface screen is open, sculk particles trace the resolved path at a low rate.
A relay's wireless hop shows as a vanilla-style vibration going out to the chest and an echo
coming back, which also plays once when a relay first hears a chest. The particles only show what
the check has already decided; they never decide anything themselves.
This costs nothing when the UI is closed and is what makes the link model legible — it turns
"why isn't my chest listed" from a mystery into something the player can look at, which is the
whole reason this model was preferred to a radius.
