# SepareWorldItems v3 – development guide

## Layout

```
Code/
├── api/                        Public API (Java 8, no platform deps) -> separeworlditems-api
├── common/                     Shared logic (Java 8): config, storage (YAML/SQL/MongoDB/Redis), backups, langs, switch engine, event bus
├── minecraft/                  Vanilla (Mojang-named) code shared by Fabric, NeoForge and Forge:
│                               sections, item codec, SwiCore, commands, regions, self test
├── integrations/
│   ├── accessories/            Accessories slots (Fabric + NeoForge)
│   └── regions/                YAWP, Flan, Open Parties and Claims adapters (Fabric + NeoForge)
├── bukkit/                     Bukkit/Spigot/Paper/Folia plugin v3 (3 jars: java8 / java17 / java21)
│   └── src/wg6/                WorldGuard 6 support, compiled against WorldGuard 6.2
├── fabric/                     Fabric mod: loader glue in fabric/src, versions in fabric/versions/<mc>/
├── fabric-legacy/              Minecraft 1.12.2 on Legacy Fabric (separate build, Loom 1.16)
├── neoforge/                   NeoForge mod (separate build, ModDevGradle), versions/<mc>/
├── forge/                      Forge mod (separate build, ForgeGradle 7), versions/<mc>/
├── build-logic/                The multi-version preprocessor (included by every build)
└── SepareWorldItems-Original/  The v2 plugin before the expansion (Maven, reference only)
```

`api` and `common` compile to Java 8 so every target can use them. The plugin relocates `common`
(not `api`) into its jars. The mods compile `api` + `common` + `minecraft` into the mod jar and bundle
SnakeYAML, SQLite and MariaDB drivers as jar-in-jar.

Loader modules only forward events to `SwiCore` (join/quit, dimension change, respawn, server tick,
commands). Everything else is shared.

## Quick start: bootstrap

One command prepares Java, builds every platform and collects the result:

```bash
bootstrap.cmd                          # Windows (double-click works too)
./bootstrap.ps1 -Only bukkit,fabric    # PowerShell, some platforms
./bootstrap.ps1 -SelfTest              # also run every in-game self test (slow)
./bootstrap.sh                         # Linux / macOS (SELFTEST=1 / NO_DOWNLOAD=1 env vars)
```

- **Java**: needs JDK 8, 17, 21 and 25. Installed JDKs are reused (Zulu, Adoptium, Oracle, `JAVA_HOME`);
  missing ones are downloaded as portable Azul Zulu builds into `Code/.jdks/` (nothing is installed on the system).
- **Output**: `Code/dist/SepareWorldItems-<version>/`
  - `plugin/` 3 plugin jars, `fabric/` `neoforge/` `forge/` mod jars, `api/` API jars
  - `configs/plugin/config.yml` and `configs/mods/config.yml` (the current default configs)
  - `BUILD-REPORT.txt`: what built, what failed and which jar is missing

## Requirements

- Gradle runs on **JDK 25+** (Fabric Loom 1.18). Old targets are compiled with `--release 8/17/21`.
- **NeoForge 26.x and Forge** need an installed **Java 25** toolchain (exact version).
- In-game tests of old servers need matching runtimes (Java 8 for 1.8.8 – 1.16.5 servers).

## Common commands

```bash
./gradlew build                          # api, common, plugin (3 jars) and every Fabric version
./gradlew :bukkit:build                  # plugin only -> build/libs/bukkit
./gradlew :fabric:1.21.1:build           # one Fabric version
./gradlew :common:test :build-logic:test # unit tests
./gradlew :api:publishToMavenLocal       # install the API for other projects (Gradle or Maven)
(cd neoforge && ./gradlew build)         # NeoForge
(cd forge && ./gradlew build)            # Forge
(cd fabric-legacy && ./gradlew build)    # 1.12.2
```

## Multi-version sources

Write code for the newest version (26.3). Where older versions differ, use directives; inactive
branches are commented with `//$$ `:

```java
//#if MC >= 1.21.11
return level.dimension().identifier().toString();
//#elseif MC >= 1.17
//$$ return level.dimension().location().toString();
//#else
//$$ return legacyWay();
//#endif
```

Conditions: `MC >= | > | <= | < | == | != <version>`, combined with `&&` and `||`, nestable.
Keep version differences inside `minecraft/.../compat/`.

The compiler does NOT catch methods annotated `@Environment(CLIENT)`: they exist at compile time but are
stripped on dedicated servers (e.g. `Abilities.setFlyingSpeed` on 1.16.5). Use the self test.

### Adding a Minecraft version

1. Add `<loader>/versions/<mc>/gradle.properties` (copy a neighbour, update versions).
2. Add `<mc>` to the version list in the loader's settings file.
3. Build, fix directives, run the self test.

## In-game self tests

Every platform has a self test that starts a throwaway dev server (accepting the EULA of its `run/`
folder), moves a test player with damaged items, armor, off-hand, ender chest, XP, food and effects
between two groups through the real engine, logs `[SelfTest] PASSED/FAILED` and stops.

```bash
./gradlew :fabric:<mc>:runServer -PswiSelftest
(cd neoforge && ./gradlew :<mc>:runServer -PswiSelftest)
(cd fabric-legacy && ./gradlew runServer -PswiSelftest)

# with integrations (the mods are only added to the dev runtime)
./gradlew :fabric:1.21.1:runServer -PswiSelftest -PswiTestMods=trinkets
./gradlew :fabric:1.21.1:runServer -PswiSelftest -PswiTestMods=accessories,owo-lib
(cd neoforge && ./gradlew :1.21.1:runServer -PswiSelftest -PswiTestMods=curios)
(cd neoforge && ./gradlew :1.21.1:runServer -PswiSelftest -PswiTestMods=yawp,flan,open-parties-and-claims,forge-config-api-port)
(cd neoforge && ./gradlew :1.21.1:runServer -PswiSelftest -PswiTestMods=create -PswiTestItems=create:wrench,create:copper_backtank)
```

Production jars can run the same test with `-Dsepareworlditems.selftest=true`. The mods also check the
bundled language files, a backup zip and a save/load round trip through the configured `storage.type`
(set it to `MONGODB` / `REDIS` to prove the jar-in-jar drivers load).

MongoDB / Redis unit tests run only against real servers:

```bash
docker run -d --rm --name swi-mongo -p 27018:27017 mongo:7
docker run -d --rm --name swi-redis -p 6391:6379 redis:7
SWI_TEST_MONGO_URI=mongodb://localhost:27018 SWI_TEST_REDIS_URI=redis://localhost:6391 ./gradlew :common:test
```
The plugin has the same switch (`-Dsepareworlditems.selftest=true`): it checks the API, a storage round
trip and, with WorldGuard installed and a `swi_selftest` region around 0,0, the region lookup.

## Integrations

- Slots (Curios, Trinkets, Accessories) are `DataSection`s registered through the public API, stored under
  `custom.<id>` and toggled with `settings.options.saves.<id>`. Items that no longer fit are given back.
- Regions (YAWP, Flan, OPAC) implement `mc.region.RegionSource`; `SwiCore` checks positions every
  `regions.check-interval-ticks` and needs two identical checks before switching.
- Integrations are compiled against the newest available mod API and wrapped in `Integrations`: an
  incompatible mod version only disables that integration with a warning.

## Data compatibility

- Same config keys, storage layout and SQL schema as the plugin (`groups/<group>/<name>-<uuid>.yml`,
  `swi_player_data`).
- The plugin keeps Bukkit's item serialization; mods write SNBT and mark records with
  `swi.format: snbt`, `swi.platform`, `swi.data-version`. Mods refuse to load plugin item records.
