# Echo Chests never form double chests

Two adjacent Echo Chests stay two chests. Vanilla chest pairing is opt-in behaviour we simply do
not implement, and trapped chests already set the precedent that a chest can decline to pair.

Pairing collides with three decisions at once. Each chest has an id assigned at placement, so a
pair would have two — and the Echo Interface needs to list one entry, not half of one. Each chest
has a Category, so pairing would need a merge policy and a rule for what happens when one half's
Category changes. And because a configured chest carries its name and Category as data components
(ADR-0008), a pair would carry two names.

Implementing it would mean a merge/split state machine and a concept of half-a-chest threaded
through link resolution, the interface list and the stacking rule — the same class of machinery
that produces Create's toolbox item-loss and duplication bugs. The mod is built on a chest being
an addressable, named, categorised thing; two chests named *Ores 1* and *Ores 2* is a perfectly
good storage wall.

## Consequences

The block model must not use vanilla's double-chest model variants, so an adjacent pair renders
as two single chests.
