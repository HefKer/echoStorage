# Echo Storage

A Minecraft mod that makes *putting items away* cheap without making *finding them*
automatic. See [CONTEXT.md](CONTEXT.md) for what that means.

Built on the upstream
[fabric-example-mod](https://github.com/FabricMC/fabric-example-mod) for Minecraft
1.21.1, plus a flake devShell that makes `runClient` actually work without an FHS
wrapper.

## Versions

| | |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.116.17+1.21.1 |
| Loom | 1.17.21 |
| Gradle | 9.5.1 (via wrapper) |
| Java | 21 |

Mappings are Mojang official (`loom.officialMojangMappings()`), not Yarn. Change the
`mappings` line in `build.gradle` if you want Yarn instead.

Bump any of these in `gradle.properties`. Java 21 is what Mojang's version manifest
declares for 1.21.1 (`javaVersion.majorVersion`); it's a floor, not an exact pin, but
it needs to come from the devShell — Gradle's toolchain auto-provisioning downloads
prebuilt JDK tarballs that don't run on NixOS.

## Use it

```sh
direnv allow     # or: nix develop
./gradlew build
./gradlew runClient
```

The mod id is `echostorage` and the root package is `dev.hefker.echostorage`.

## Why the devShell is shaped like this

Minecraft on NixOS fails in a specific way. LWJGL ships its native `.so` files inside
its jars, extracts them to a temp directory at runtime, and `dlopen`s their
dependencies by bare soname. Those extracted binaries have no RUNPATH, and a nixpkgs
JDK has no FHS fallback, so nothing resolves. The devShell answers that with
`LD_LIBRARY_PATH`:

- **`addDriverRunpath.driverLink` first.** This is `/run/opengl-driver/lib`, where the
  vendor GL/Vulkan ICD lives. `libGL` alone only gets you the dispatch layer. Omitting
  this is the difference between hardware rendering and a failed GL context.
- **`alsa-lib`, `libpulseaudio`, `pipewire`, `libjack2`.** LWJGL's *bundled*
  `libopenal.so` dlopens these backends itself. Adding `openal` alone gets you silence.
- **`libdecor`, `wayland`, `libxkbcommon`** for GLFW's Wayland backend, and the X11 set
  for the XWayland fallback.
- **`ncurses`, `zlib`** are not for Minecraft. Gradle extracts its own JNI natives
  (`libnative-platform-curses.so`) which need `libncursesw`/`libtinfo`.

The list mirrors the one in nixpkgs' `prismlauncher` wrapper, which is the best
maintained reference for this.

## Gotchas

**Don't mix nixpkgs `gradle` with `./gradlew`.** Gradle caches its extracted natives
under `$GRADLE_USER_HOME` keyed on version alone, ignoring file contents. nixpkgs'
`gradle` is autoPatchelf'd and the wrapper's is not, so alternating between them
poisons the cache and produces confusing load failures. Use the wrapper.

**Wayland works.** LWJGL 3.3.3 bundles GLFW 3.4 built with both the Wayland and X11
backends, and GLFW 3.4 prefers Wayland when `XDG_SESSION_TYPE=wayland`. If you hit the
known Wayland cursor-grab or fractional-scaling bugs, force the patched GLFW that's
already in the shell — but do it through Loom's DSL, not an environment variable:

```groovy
loom {
    runConfigs.all {
        property "org.lwjgl.glfw.libname", "/nix/store/.../lib/libglfw.so"
    }
}
```

`JAVA_TOOL_OPTIONS` looks like it should work here and mostly doesn't: the forked
client inherits the *Gradle daemon's* environment, and the daemon is long-lived, so a
daemon started outside the devShell ignores it. It also makes every JVM print `Picked
up JAVA_TOOL_OPTIONS` to stderr.

**Editors.** `jdtls` is in the shell. Zed has no built-in Java support — install the
Java extension, which prefers a `jdtls` on `$PATH` over downloading its own.

## License

CC0, same as the upstream template it's based on.
