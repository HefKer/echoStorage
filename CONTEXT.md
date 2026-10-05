# Echo Storage

A Minecraft mod that makes *putting items away* cheap without making *finding them*
automatic. Storage stays physical and named; the mod removes the tedium of sorting,
never the player's knowledge of where things are.

## Language

A glossary term takes a capital in every form, singular, plural or inflected, and so does a word derived from one, such as "Strictness" from Strict or "Linked" from Link. An ordinary word the term was built from is not derived from it: "see through" and "look into" stay lower-case beside See-through, as "strict" does in "a strict rule".

Player-facing text, in the language file and the config file's comments, uses a glossary term only where the game shows that term as a name: an item or block name, or a button or label. There it takes its capital, tooltips included. Anything else is said in plain words, so Vacuum is "Pick up".

**Echo Chest**:
A placed container that knows which Category it holds and carries a player-given name.
_Avoid_: filtered chest, smart chest, storage unit

**Deep Echo Chest**:
An Echo Chest with twice the room, crafted from a chest or by upgrading an Echo Chest, which
keeps its name and Category. Everything said of an Echo Chest holds for it.
_Avoid_: large echo chest, double echo chest, echo chest tier 2

**Echo Bundle**:
A carried container holding four bundles' worth of mixed items, optionally assigned a
Category, for keeping an inventory manageable while away from base.
_Avoid_: big bundle, sack, pouch

**Selected item**:
The one item an Echo Bundle acts with when used from the hand: the block it places or the food
it eats. The player moves it from Stop to Stop; it passes to the next Stop when used up.
_Avoid_: selection, active item, current slot, most recent

**Stop**:
One group of an Echo Bundle's contents that would stack together, however many there are and
however many entries they span: a place the Selected item can be. Ordered newest first.
_Avoid_: distinct item, kind

**Echo Shulker Box**:
An Echo Chest the player can carry: placed, it is an Echo Chest in every respect; broken, it
keeps its contents, and while carried it can Vacuum.
_Avoid_: echo shulker, portable chest, backpack

**Vacuum**:
A carried Echo Bundle or Echo Shulker Box taking an item the player picks up, when the item is
in its Category or it already holds that item. Off on each item until the player turns it on.
A picked-up bundle or shulker box is never Vacuumed: like Quick-stack, Vacuum treats both as
carried storage, not something to put away.
_Avoid_: auto-pickup, magnet, absorb, siphon

**Echo Interface**:
A placed block that lists the Echo Chests Linked to it and opens a chosen one
from where the player stands. It shows chests, never their merged contents.
_Avoid_: terminal, controller, network hub, access point

**Category**:
A named set of items an Echo Chest can be assigned to hold, resolved from item tags,
item predicates and datapack overrides.
_Avoid_: filter, group, class

**Stray**:
An item outside the Category of the Echo Chest it is in or being put into. A chest with no
Category has no Strays. An Echo Bundle or shulker box is judged by what it holds, one level
deep: a Stray if anything inside is, and never one while empty. A vanilla bundle, or a shulker
box that is not See-through, is judged as the item it is.
_Avoid_: misfit, foreign item, unsorted item

**See-through**:
Of an Echo Bundle or a shulker box: its items can be looked into, one level deep, so in an Echo
Chest it is judged by what it holds, and Quick-stack or Vacuum can top it up. An Echo Bundle
always is; a shulker box is once its loot has been rolled, and only if it keeps its items the
way vanilla's shulker boxes do (ADR-0007).

**Strict**:
The Echo Chest setting under which the chest refuses Strays from every source except the
player's own hand. Has no effect on a chest with no Category.
_Avoid_: locked, whitelist mode, filtered

**Permissive**:
The default setting, the opposite of Strict: Strays are accepted from any source.
_Avoid_: open, unfiltered

**Link**:
A path of Connectors from an Echo Interface to an Echo Chest, optionally ending in one hop
through the air from an Echo Relay to a chest it has heard. Built by the player, visible in
the world, and the only thing that defines what an interface can reach.
_Avoid_: connection, channel, network, pairing

**Connector**:
A block that can carry a Link. Membership is a block tag, so packs and future versions
can add their own; it is sculk vein, sculk block and the Echo Relay.
_Avoid_: cable, conduit, wire, node

**Echo Relay**:
A Connector that hears an Echo Chest being opened nearby and, from then on, carries a
Link to it through the air while it stays in range with no wool in between.
_Avoid_: receiver, listener, antenna, node

**Quick-stack**:
A player-initiated action that moves items from the player's inventory into one or more
Echo Chests, each item going to a chest whose Category it falls in or that already holds it.
Never happens on its own, and never moves a bundle or shulker box: those are carried storage,
not something to put away.
_Avoid_: auto-sort, deposit, dump
