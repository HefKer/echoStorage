# A broken Echo Chest keeps its name, Category and strictness; it stacks only while blank

Breaking an Echo Chest drops an item that carries everything the player set on it: the typed
name, the Category and the strict flag. The chest's id and contents do not travel (the id dies
with the block, per its lifecycle; the contents spill). A chest with nothing set drops as a plain
item that stacks with a freshly crafted one.

The alternative was that a Category belongs to a chest's place in the base, not to the item, so
only the name should travel. In practice that meant rebuilding a storage wall kept every name and
lost every assignment, leaving the player to reassign each chest from memory, which is exactly
the kind of tedium the mod exists to remove. The Echo Bundle already carries its Category on the
item, so the chest was the odd one out.

Strictness travels with the Category rather than on its own terms: it only has an effect when a
Category is set, and carrying one without the other would keep half of the player's intent.

## Consequences

- A chest with a Category, or set to strict, stops stacking with blank chests, just as a named
  chest already does. The item's tooltip shows the Category and strictness so the reason is
  visible.
- The Category is saved on the item by name and read leniently: a name no preset has any more
  loads as unassigned and must never make the item or the chest fail to load.
- The Category name shown on an unnamed chest stays display-only. It is never written as the
  item's custom name.
