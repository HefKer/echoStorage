# The Echo Chest is 27 vanilla slots; depth comes from bundles, not bigger stacks

The design originally called for 256-item stacks. That is not available on 1.21.1: vanilla
hard-caps stack count at 99 through `ExtraCodecs.intRange(1, 99)` in both
`DataComponents.MAX_STACK_SIZE` and `ItemStack.CODEC`'s `count` field, and because DFU validates
on encode, `ItemStack.save` *throws* above it. `Item.Properties.stacksTo` does not clamp, so
`stacksTo(256)` compiles, runs, fills to 256, and then detonates on the first world save.

Four ways past it were considered and all rejected:

- **Global codec mixin** (StackSizeTweaks' approach) — needs ~12 further mixins to clean up the
  fallout: hoppers silently destroy the excess via `limitSize`, comparators return 37 instead of
  15 with no clamp, `ItemEntity.merge` hardcodes 64, the count string overflows the slot. It also
  targets `ItemStack`/`Inventory`, the exact surface ADR-0003 rule 7 keeps us off.
- **Private unbounded codec** (Sophisticated Storage) — still exposes oversized `ItemStack`s to
  other mods, which breaks them repeatedly and publicly (Create #6600, Sophisticated #931, #616).
- **N backing slots painted as one** (Create's Toolbox: 4 real 64-slots per compartment, three
  parked off-screen at -10000, the sum rendered as a decoration). Fully compatible — nothing
  oversized ever escapes — but it means owning the settle/sync state machine that Create still
  has open item-loss and duplication bugs against on 1.21.1.
- **Configurable slot count** — container size is world data, not a preference. Shrinking it
  strands the contents of the removed slots in NBT with no code path to reach them.

So the chest is an ordinary 27-slot vanilla container, and capacity comes from putting Echo
Bundles inside it. That converts a global config decision into a per-chest player decision made
in-world by spending resources, makes the cheap feature (the 4x bundle, which needs no codec or
network change) carry the capacity story, and leaves the expensive one — the chest — as a vanilla
chest that knows a word. If a larger variant is ever wanted it is a separate block, never a
config toggle.
