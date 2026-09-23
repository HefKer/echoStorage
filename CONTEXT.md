# Echo Storage

A Minecraft mod that makes *putting items away* cheap without making *finding them*
automatic. Storage stays physical and named; the mod removes the tedium of sorting,
never the player's knowledge of where things are.

## Language

**Echo Chest**:
A placed container that knows which Category it holds and carries a player-given name.
_Avoid_: filtered chest, smart chest, storage unit

**Echo Bundle**:
A carried container holding four bundles' worth of mixed items, optionally assigned a
Category, for keeping an inventory manageable while away from base.
_Avoid_: big bundle, sack, pouch

**Echo Interface**:
A placed block that lists the Echo Chests linked to it by sculk and opens a chosen one
from where the player stands. It shows chests, never their merged contents.
_Avoid_: terminal, controller, network hub, access point

**Category**:
A named set of items an Echo Chest can be assigned to hold, resolved from item tags,
item predicates and datapack overrides.
_Avoid_: filter, group, class

**Link**:
A path of sculk connecting an Echo Chest to an Echo Interface. Built by the player,
visible in the world, and the only thing that defines what an interface can reach.
_Avoid_: connection, channel, network, pairing

**Connector**:
A block that can carry a Link. Membership is a block tag, so packs and future versions
can add their own; in v1 it is sculk vein and sculk block.
_Avoid_: cable, conduit, wire, node

**Quick-stack**:
A player-initiated action that moves matching items from the player's inventory into
one or more Echo Chests. Never happens on its own.
_Avoid_: auto-sort, deposit, dump
