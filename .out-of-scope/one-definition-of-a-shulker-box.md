# One definition of a shulker box

The mod identifies a shulker box in two ways, and they are not merged into one.

- `CarriedStorage` goes by the `c:shulker_boxes` item tag. That decides which stacks are see-through inside an Echo Chest, and which stacks Quick-stack and Vacuum never move (ADR-0007).
- `EchoChestBlockEntity.refuses`, the menu's slots and `EchoBundleContents.canHold` go by `canFitInsideContainerItems()`. That decides what an Echo Shulker Box or an Echo Bundle keeps out.

The two agree for vanilla shulker boxes and the Echo Shulker Box. They can disagree for another mod's item: one in the tag that fits inside containers, or one outside the tag that does not.

## Why this is out of scope

They answer two different questions.

"May this item go inside a carried container?" is vanilla's question, and `canFitInsideContainerItems()` is vanilla's answer. Vanilla's own shulker box slot asks it, and the Echo Chest menu uses that slot for a chest kind that refuses shulker boxes. Every mod with a backpack or a box of its own sets this flag to stop containers nesting without limit, whether or not it tags the item as a shulker box.

```java
// EchoChestBlockEntity: the refusal is about nesting, not about being a shulker box
return (refusesShulkerBoxes && !stack.getItem().canFitInsideContainerItems())
		|| (assignment.strict() && isStray(assignment.category(), stack));
```

"Is this a shulker box we read into and never move?" is the mod's own question. ADR-0007 answers it with the tag, plus the vanilla `container` component for reading.

Either merge loses something:

- Refusing by the tag would let an untagged backpack from another mod go inside an Echo Shulker Box, and from there nest without limit.
- Reading through everything that does not fit inside containers would treat items as shulker boxes that keep their contents somewhere the mod cannot read.

The one disagreement that did harm was a tagged box that fits inside containers being vacuumed into an Echo Bundle. That was a bug in Vacuum's guard, fixed in #54: Vacuum now asks `CarriedStorage` before anything else. It did not need the definitions merged.

## Prior requests

- #57: "Two definitions of a shulker box: the c:shulker_boxes tag and canFitInsideContainerItems"
