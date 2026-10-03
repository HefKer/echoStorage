# Quick-stack is the only thing that writes into a nested bundle

Because capacity comes from Echo Bundles placed inside Echo Chests (ADR-0005), matching has to
see through them — an ore chest whose ore is in bundles would otherwise be invisible to its own
matcher. Recursion is one level deep, which Q18's refusal of bundle-in-bundle makes exhaustive.

The split is read versus write. Category matching, contents-matching for display and the
interface's view are all **read-only** through nested bundles. The explicit Quick-stack action is
the **only** path that writes into one: it prefers topping up bundles already in the chest until
they are full, then falls through to chest slots.

The risk being managed is writing to a data component on a stack that lives inside a container.
That is genuinely dangerous through interactive menu clicks, where the client cursor, the slot
stack and the component can drift apart. A Quick-stack button is not that — it is one
server-authoritative operation computing a whole transfer and writing it atomically, with the
client receiving the result. Confined there it is about as safe as any container write in the mod.

So shift-clicking a stack into a chest puts it in a slot and never hunts for a bundle to fill;
only the button recurses.

## Consequences

The button and a shift-click produce materially different results in the same chest. That is
intended — the button is the bulk action, the click is the precise one — but it must be visible
in the UI rather than left as a hidden rule.

## Amendment (2026-09-29): one level, through bundles and shulker boxes, for bulk actions

Two things change; the read-versus-write split and its reasoning stand.

**Shulker boxes are See-through too.** A shulker box item (vanilla or Echo) inside an Echo Chest
is read and written exactly as a bundle there is: matching sees into it, Quick-stack tops up
what it already holds, ignoring its own Category (ADR-0010). Recursion is still exactly one
level, whatever the containers: Quick-stack into a chest holding a shulker box that holds a
bundle fills the shulker box, never the bundle inside it.

**Vacuum is the second nested write.** A carried Echo Shulker Box that Vacuums an item tops up
the bundles inside it before its own slots. The risk this ADR manages lives in interactive menu
clicks, and Vacuum on pickup is, like the button, one server-authoritative operation. So the
rule becomes: server-authoritative bulk actions (Quick-stack, Vacuum) are the only nested
writes; a menu click never is.

**What a shulker box is.** Anything in the `c:shulker_boxes` item tag, so another mod's shulker
boxes count as well. One is See-through when it keeps its items in the vanilla `container`
component and its loot has been rolled. One that is not See-through is neither read nor written.

**Reads stop at one level too.** A bundle inside a shulker box inside a chest is judged as the
item it is, not by what it holds, so that shulker box is a Stray unless the Category covers
bundles. One depth for reads and writes keeps a single rule; the hand still gets it past Strict.

**A shulker box is carried storage, like a bundle.** Quick-stack and Vacuum never move one,
whatever it holds, See-through or not. Seeing through them would otherwise put a player's box
of ore in an ore chest's Category, and a bulk action would take the whole box.

## Amendment (2026-10-01): a shulker box of unknown size is only topped up

This adds to "Shulker boxes are See-through too" and "What a shulker box is" in the 2026-09-29
amendment, which make any See-through shulker box in the `c:shulker_boxes` tag a target for
Quick-stack, another mod's included. It also widens that amendment's "Quick-stack tops up what
it already holds": for vanilla's shulker boxes and the Echo Shulker Box, an item the box already
holds may now go into its empty slots as well, not only onto its existing stacks.

**How many slots a shulker box has.** The `container` component does not say. Vanilla's shulker
boxes and the Echo Shulker Box are known to have 27, so Quick-stack tops up their matching stacks
and then fills their empty slots. Any other shulker box only has the stacks it already holds
topped up: same item, same components, up to the stack's limit. No empty slot in it is written
and no slot is added past those its component lists, so a smaller box loses nothing and a bigger
one is topped up wherever its stacks are. A box listing more than 27 slots is treated this way
whatever its kind. Reads are the same for every kind.
