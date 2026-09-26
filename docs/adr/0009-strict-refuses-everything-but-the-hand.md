# A strict Echo Chest refuses strays from everything but the player's hand

A strict chest refuses a stray on every path that inserts on the player's behalf or without
them, and on none where the player puts the stack there themselves. Placing a stack by hand,
dragging it across slots or number-key swapping it in always works: that is the player choosing
to. Shift-click and quick-stack are refused, and so is every insert that asks
`Container.canPlaceItem` — hoppers, droppers, hopper minecarts, `/loot insert`, and other mods'
pipes through Fabric's Transfer API. A strict chest with no Category refuses nothing, because it
has no strays.

#6 named only "hopper insert". The wider reach is intended: the line that matters is whether the
player placed the stack, not which machine did, and `canPlaceItem` cannot tell its callers apart
anyway. Exempting mod pipes would make strictness meaningless in exactly the modded bases where
automated inserts are common.

## Consequences

- A stray placed by hand in a strict chest stays there, but quick-stack will not top it up. A
  permissive chest would. This is the only place strictness touches quick-stack (ADR-0010).
- Echo Bundles have a Category but no strictness. Nothing that writes into a bundle would consult
  it: vacuum already takes only what the Category matches (or, with none, what the bundle holds),
  and quick-stack's nested write only tops up what a bundle already holds.
