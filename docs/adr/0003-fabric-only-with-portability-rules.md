# Fabric-only on 1.21.1, with portability enforced as concrete rules

Modrinth counts (queried 2026-09-22) show 1.21.1 is NeoForge's high-water mark, not the
start of a trend: 20,730 NeoForge mods against 19,168 Fabric on 1.21.1, but on every later
version Fabric leads 1.5:1 to 2.2:1 and NeoForge's modpack presence collapses from 2,031 to
under 80. So the NeoForge audience on our target version is real — about half the market —
and shrinking everywhere else. Only 8% of 1.21.1 projects ship both loaders.

We ship Fabric-only, because multiloader's cost is recurring (four Gradle subprojects, two
runClient loops, two jars to test, two release channels, forever) and it lands before we
know whether the design works. Roughly 80% of this mod's surface is vanilla and ports
untouched; ~12% is mechanical adapter work; ~8% is genuine rewrite.

Portability is therefore bought as seven concrete rules rather than good intentions:

1. Stay on Mojang mappings. NeoForge is Mojang-mapped natively; this is the single biggest
   win and the repo already has it.
2. A `platform` package with a plain `ServiceLoader` lookup from day one (the
   MultiLoader-Template pattern), without adopting its Gradle layout.
3. Every network payload is a `record implements CustomPacketPayload` with a vanilla
   `StreamCodec`, sent and received through one small `Net` facade.
4. Menu open-data is a record + `StreamCodec`, never ad-hoc `buf.writeX` calls. This turns
   the hardest port surface into a five-line adapter.
5. Vanilla APIs over Fabric sugar: `Item.Properties` not `FabricItemSettings`, `Container`
   internally rather than `Storage<ItemVariant>`, adapting to the Transfer API only at the
   outer edge.
6. Our own small config rather than a Fabric-only config library. The asymmetry runs our
   way: NeoForge has a built-in `ModConfigSpec`, Fabric has nothing, so anything hand-rolled
   for Fabric works verbatim on NeoForge.
7. Mixin restraint, especially around `ItemStack` and `Inventory` — NeoForge patches those
   heavily via its own source patches, making them the one surface with an unpriceable
   port cost.

Architectury was considered and rejected: it abstracts nothing we need (no data components,
no tags, no config, no mixins), it downgrades Fabric's typed `ExtendedScreenHandlerType<T, D>`
to NeoForge's raw `FriendlyByteBuf` model, and it is a runtime dependency our users must
install. If we ever go multiloader, `jaredlll08/MultiLoader-Template`'s 1.21.1 branch is the
better vehicle.
