# Automated tests for which widgets a screen shows

No automated test checks which buttons or boxes a screen builds, such as whether the Quick-stack button on `EchoChestScreen` or `EchoInterfaceScreen` is hidden when its world switch is off. Reading the code is how that gets checked.

## Why this is out of scope

The only way to test a screen's widgets in this project is a client game test. Those are turned off (`enableClientGameTests = false`). Turning them on means running a real client with rendering in every test run, which costs far more than the checks it would add.

What such a test would cover is small. Each switched widget is a single `if` on `EchoConfig.get()` around an `addRenderableWidget` call:

```java
if (EchoConfig.get().chestQuickStack()) {
	addRenderableWidget(/* the quick-stack button */);
}
```

The behaviour that matters is already tested on the server. A Quick-stack press while the switch is off is refused, and the chest and interface game tests cover that. A button that shows by mistake would do nothing when pressed.

Moving the "which widgets show" decision into a class a unit test can reach would add a layer just to test one condition per widget.

## Prior requests

- #22: "Decide three follow-ups from the quick-stack world switches" (point 1)
