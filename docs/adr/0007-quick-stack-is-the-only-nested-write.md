# Quick-stack is the only thing that writes into a nested bundle

Because capacity comes from Echo Bundles placed inside Echo Chests (ADR-0005), matching has to
see through them — an ore chest whose ore is in bundles would otherwise be invisible to its own
matcher. Recursion is one level deep, which Q18's refusal of bundle-in-bundle makes exhaustive.

The split is read versus write. Category matching, contents-matching for display and the
interface's view are all **read-only** through nested bundles. The explicit quick-stack action is
the **only** path that writes into one: it prefers topping up bundles already in the chest until
they are full, then falls through to chest slots.

The risk being managed is writing to a data component on a stack that lives inside a container.
That is genuinely dangerous through interactive menu clicks, where the client cursor, the slot
stack and the component can drift apart. A quick-stack button is not that — it is one
server-authoritative operation computing a whole transfer and writing it atomically, with the
client receiving the result. Confined there it is about as safe as any container write in the mod.

So shift-clicking a stack into a chest puts it in a slot and never hunts for a bundle to fill;
only the button recurses.

## Consequences

The button and a shift-click produce materially different results in the same chest. That is
intended — the button is the bulk action, the click is the precise one — but it must be visible
in the UI rather than left as a hidden rule.

## Amendment (2026-09-29): one level, through bundles and shulker boxes, for bulk actions

Two things change; the read-versus-write split and its reasoning stand.

**Shulker boxes are see-through too.** A shulker box item (vanilla or Echo) inside an Echo Chest
is read and written exactly as a bundle there is: matching sees into it, Quick-stack tops up
what it already holds, ignoring its own Category (ADR-0010). Recursion is still exactly one
level, whatever the containers: Quick-stack into a chest holding a shulker box that holds a
bundle fills the shulker box, never the bundle inside it.

**Vacuum is the second nested write.** A carried Echo Shulker Box that vacuums an item tops up
the bundles inside it before its own slots. The risk this ADR manages lives in interactive menu
clicks, and vacuum on pickup is, like the button, one server-authoritative operation. So the
rule becomes: server-authoritative bulk actions (Quick-stack, Vacuum) are the only nested
writes; a menu click never is.
