# Reading an Echo Chest's assignment from its block data

When an Echo Chest is placed, it takes its Category and strictness only from the item's `echostorage:echo_chest_assignment` component. It does not use the `Category` or `Strict` keys in the item's `minecraft:block_entity_data` as a fallback. If the component is missing, the chest places unassigned and Permissive.

This matters for items copied before #12. Back then, `removeComponentsFromTag` stripped only the name, so a ctrl + pick-block copy kept `Category` and `Strict` inside its block data. Those copies now place blank.

## Why this is out of scope

The item's components are the only source for the assignment. Vanilla handles the custom name the same way: `applyImplicitComponents` runs after `block_entity_data` loads, and whatever it sets wins.

A fallback can't tell two items apart:

- an old copy that never had the component, and
- a chest whose Category the player cleared. `collectImplicitComponents` leaves the component off a blank chest, so it drops a plain item.

Both items have no component. Reading the block data for either one risks bringing back a Category the player removed. Stale data would come back quietly, which is worse than a creative copy placing blank.

The only items affected are creative copies made before #12, which never shipped in a release. Copying the chest again fixes it.

```java
// EchoChestBlockEntity: the component is the whole answer
assignment = components.getOrDefault(EchoComponents.ECHO_CHEST_ASSIGNMENT, EchoChestAssignment.DEFAULT);
```

## Prior requests

- #19: "Decide whether pre-#12 creative copies of an Echo Chest should keep their Category when placed"
