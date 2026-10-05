# Quick-stack matches a chest's Category or what it holds, and each button is a world switch

Quick-stack moves an item into an Echo Chest when the item is in the chest's Category **or** the
chest already holds it, unless the chest refuses it (ADR-0009). #7 matched on held items only, so
an empty chest with a Category never received anything until the player seeded it by hand; the
Category is the player's statement of what belongs there, and Quick-stack should honour it.

The nested write into a bundle or shulker box inside the chest (ADR-0007) ignores any Category
the bundle or shulker box has itself and adds only items it already holds, so Quick-stack never
starts a new kind of item inside either. Where those items go is ADR-0007's to say: onto the
stacks already there, and for a shulker box of known size into its empty slots as well (ADR-0007's
2026-10-01 amendment).

Global Quick-stack from an Echo Interface makes two passes over every Linked chest: first the
chests that already hold the item, then those whose Category matches it. Like items join each
other before a Category chest starts a new pile, whatever order the list is in.

## Consequences

- Two world-preference switches in `EchoConfig`: `chestQuickStack` (default on) for the Echo
  Chest screen's button, and `interfaceQuickStack` (default off) for the Echo Interface's. Global
  Quick-stack shipped on in #11; defaulting it off is a deliberate step back, making it opt-in for
  a pack rather than on in every world.
- A switched-off action is refused by the server, not merely hidden on the client.
