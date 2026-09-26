# Quick-stack matches a chest's Category or what it holds, and each button is a world switch

Quick-stack moves an item into an Echo Chest when the item is in the chest's Category **or** the
chest already holds it, unless the chest refuses it (ADR-0009). #7 matched on held items only, so
an empty chest with a Category never received anything until the player seeded it by hand; the
Category is the player's statement of what belongs there, and quick-stack should honour it.

The nested write into a bundle inside the chest (ADR-0007) ignores the bundle's own Category and
tops up only what the bundle already holds, so quick-stack never starts a new kind of item inside
a bundle.

Global quick-stack from an Echo Interface makes two passes over every linked chest: first the
chests that already hold the item, then those whose Category matches it. Like items join each
other before a Category chest starts a new pile, whatever order the list is in.

## Consequences

- Two world-preference switches in `EchoConfig`: `chestQuickStack` (default on) for the Echo
  Chest screen's button, and `interfaceQuickStack` (default off) for the Echo Interface's. Global
  quick-stack shipped on in #11; defaulting it off is a deliberate step back, making it opt-in for
  a pack rather than on in every world.
- A switched-off action is refused by the server, not merely hidden on the client.
