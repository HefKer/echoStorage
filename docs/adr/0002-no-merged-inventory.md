# The mod never presents a merged inventory, and never moves items between containers on its own

Every tech storage mod's central feature is a single searchable view of everything you own.
Echo Storage deliberately refuses it. The Echo Interface lists linked Echo Chests by name and
opens one of them; it does not pool their contents. Search exists only over what is already on
screen (the open container) or over names the player typed themselves — never over the contents
of chests that aren't open.

The scope rule is: **the mod never moves items between containers on its own. Movement between a
container and the player's hand or inventory is exempt.**

That exemption is deliberate and load-bearing. It legalises the Echo Bundle's conveniences —
auto-vacuum on pickup, auto-refill into the hand, placing directly from the bundle — which are
what make the bundle worth carrying. It keeps illegal the thing that actually makes a storage
mod feel like AE2: autonomous inter-container logistics. Hoppers feeding Categories, chests
rebalancing themselves, background tidying — all out, permanently.

Each of the three bundle conveniences is individually configurable, so modpack authors who want
storage kept manual can switch them off without losing the Category system.

This is recorded because a future reader will look at the code and ask why the obvious feature
is missing. It is missing on purpose: the mod's claim is that it helps you sort rather than
sorting for you, and a merged searchable pool is the point where that stops being true.
