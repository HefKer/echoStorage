# CarriedStorage deciding which shulker box slots are written

`CarriedStorage.shulkerBoxSlots` says how many slots a shulker box has, where that is known. Whether Quick-stack may fill the box's empty slots is worked out in `QuickStack.intoShulkerBox`, from that count and the number of slots the box's `container` component lists. The rule is not moved into `CarriedStorage`.

## Why this is out of scope

`CarriedStorage` answers what a stack is: whether it is carried storage, what it holds, how many slots it has. What to write into it is Quick-stack's question, and `QuickStack.intoShulkerBox` is the only place that asks it.

The rule there is two lines:

```java
OptionalInt known = CarriedStorage.shulkerBoxSlots(box);
boolean fills = known.isPresent() && listed <= known.getAsInt();
SimpleContainer slots = new SimpleContainer(fills ? known.getAsInt() : listed);
```

Moving it would not shorten the caller. `intoShulkerBox` needs the listed count either way, to size the container it works in when the box is only topped up. A `CarriedStorage` method returning "the slots that may be written" would hand back one number and leave Quick-stack to read the component again for the other.

If a second writer of shulker box items appears, the rule would have two callers and this is worth another look.

## Prior requests

- #66: "QuickStack works out for itself whether a shulker box's empty slots may be written"
