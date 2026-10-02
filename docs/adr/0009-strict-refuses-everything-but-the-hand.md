# A Strict Echo Chest refuses Strays from everything but the player's hand

A Strict chest refuses a Stray on every path that inserts on the player's behalf or without
them, and on none where the player puts the stack there themselves. Placing a stack by hand,
dragging it across slots or number-key swapping it in always works: that is the player choosing
to. Shift-click and Quick-stack are refused, and so is every insert that asks
`Container.canPlaceItem` — hoppers, droppers, hopper minecarts, `/loot insert`, and other mods'
pipes through Fabric's Transfer API. A Strict chest with no Category refuses nothing, because it
has no Strays.

#6 named only "hopper insert". The wider reach is intended: the line that matters is whether the
player placed the stack, not which machine did, and `canPlaceItem` cannot tell its callers apart
anyway. Exempting mod pipes would make strictness meaningless in exactly the modded bases where
automated inserts are common.

## Consequences

- A Stray placed by hand in a Strict chest stays there, but Quick-stack will not top it up. A
  Permissive chest would. This is the only place strictness touches Quick-stack (ADR-0010).
- Echo Bundles have a Category but no strictness. Nothing that writes into a bundle would consult
  it: Vacuum already takes only what the Category matches (or, with none, what the bundle holds),
  and Quick-stack's nested write only tops up what a bundle already holds.

## Amendment (2026-09-29): Vacuum takes Category or held, and a Strict box refuses Strays

The last consequence above is out of date. Vacuum now follows Quick-stack's rule (ADR-0010): a
carried Echo Bundle or Echo Shulker Box takes a picked-up item when the item is in its Category
**or** it already holds that item. Echo Bundles still have no strictness, so a bundle with a
Category now tops up a Stray it holds.

A carried Echo Shulker Box keeps its strictness. A Strict box vacuums only what its Category
matches, into the bundles inside it as well as its own slots: Vacuum is not the player's hand, so
it is refused like Quick-stack. Strictness therefore touches two bulk actions, Quick-stack and
Vacuum, and both refuse the same Strays.

## Amendment (2026-09-30): an Echo Shulker Box refuses shulker boxes even by hand

One exception to "the hand always works": an Echo Shulker Box's slots refuse a shulker box of any
kind, vanilla or Echo, even when the player places it by hand, as vanilla's shulker box slots do.
Every other path refuses them too, Strict or Permissive. This is not strictness: it keeps a
carried box from nesting inside another, and it applies to no other kind of Echo Chest.
