# A test helper for boxes with no Category

`VacuumTest.vacuumingBox(EchoChestAssignment, ItemStack...)` builds every Vacuum-on Echo Shulker Box in that test. Call sites that set no Category or Strictness pass `EchoChestAssignment.DEFAULT`:

```java
player.setItem(3, vacuumingBox(EchoChestAssignment.DEFAULT, fullOfCobble));
```

No overload `vacuumingBox(ItemStack...)` and no named helper such as `unassigned()` stands in for it.

## Why this is out of scope

`EchoChestAssignment.DEFAULT` already says "no Category, not Strict" in the domain's own terms, and it is the same constant the production code uses. An overload next to the varargs signature would surprise a reader, and an `unassigned()` helper would only rename the constant. Either one would make six call sites shorter while adding test code that has to be kept in step with the record.

## Prior requests

- #100: "Review of #92: judgement calls on VacuumTest's vacuumingBox helper"
