# SEPARE-WORLD-ITEMS
SEPARE WORLD ITEMS,  It's a echo plugin to be for your invention by world.

## Download

[Donwload 2.0.1 (Folia Support) / Minecraft Version Support 1.8.8 - 26.1.2 ](https://github.com/Happyuky7/SEPARE-WORLD-ITEMS/releases/)

---

## Install

- Download Lasted Plugin Version.
- Move you plugin folder `/plugins`.
- Start or Restart your server.
- Join a plugin folder `/plugins/SepareWorldItems`.
- Configure plugin in `config.yml`.
- Restart Your Server.

**Note:** If you use any `DEV-VERSION` of the plugin,
I **recommend** you to make a **backup** and **check** the **changelist** of

---

## Version 3 (IN DEVELOPMENT) — plugin + mods

SepareWorldItems v3 runs on Bukkit-based servers **and** as a server-side mod (also singleplayer/LAN),
with one shared core, one config format and one developer API. The v2 plugin is kept untouched in
[`Code/SepareWorldItems-Original`](Code/SepareWorldItems-Original).

### Plugin (Spigot, Paper, Purpur, Folia)

| Jar | Minecraft | Server Java |
|-----|-----------|-------------|
| `SepareWorldItems-bukkit-<ver>-java8.jar` | 1.8.8 – 1.16.5 | 8+ |
| `SepareWorldItems-bukkit-<ver>-java17.jar` | 1.17 – 1.20.4 | 17+ |
| `SepareWorldItems-bukkit-<ver>-java21.jar` | 1.20.5 – 26.x | 21+ |

WorldGuard regions work with **WorldGuard 6** (1.8.8 – 1.12.2) and **WorldGuard 7** (1.13 – 26.x).

### Mods

| Minecraft | Fabric | NeoForge | Forge |
|-----------|--------|----------|-------|
| 26.3, 26.2, 26.1.2 | ✅ (Java 25) | ✅ | ✅ |
| 1.21.11 | ✅ | ✅ | ✅ |
| 1.21.1 | ✅ | ✅ | ✅ |
| 1.20.6, 1.19.4, 1.18.2 | ✅ | – | – |
| 1.16.5 | ✅ (Java 8) | – | – |
| 1.12.2 | ✅ Legacy Fabric | – | – |

- Put `SepareWorldItems-<loader>-<version>.jar` in `mods/` (Fabric also needs Fabric API).
- Config: `config/separeworlditems/config.yml` (same keys as the plugin; worlds are dimension ids such as `"minecraft:the_nether"`).
- Data: `<world save>/separeworlditems/` (each singleplayer world keeps its own data).
- Storage: `YAML`, `SQLITE`, `MYSQL`, `MARIADB`, `MONGODB`, `REDIS` (drivers bundled in the jar).
- Backups: zip of the data folder + config in `<world save>/separeworlditems/backups/` (`settings.backups`, same keys as the plugin).
- Languages: the same `langs/<lang>.yml` files as the plugin (`settings.langs.lang`), downloaded to `config/separeworlditems/langs/`.
- Commands: `/swi`, `/swi group [player]`, `/swi reload`, `/swi backup`. Admin permission `separeworlditems.admin`
  (LuckPerms through fabric-permissions-api / NeoForge and Forge PermissionAPI, OP level 2 otherwise).

**Mod integrations** (activated automatically when the mod is installed):

| Mod | What SepareWorldItems does |
|-----|----------------------------|
| Curios (NeoForge) | Curio slots (and cosmetic slots) are saved per group |
| Trinkets (Fabric) | Trinket slots are saved per group |
| Accessories (Fabric / NeoForge) | Accessory slots (and cosmetic slots) are saved per group; takes over when it emulates Curios/Trinkets |
| YAWP | Region name → group, like `worldguard-regions` |
| Flan | Group per claim, per owner, for admin claims or any claim |
| Open Parties and Claims | Group per party, per owner or any claimed chunk |
| Create and other content mods | Their items (with all their data) are saved and restored untouched |

Items that do not fit in the destination group (e.g. fewer accessory slots) are given back to the player, never deleted.

**Note:** mods store items as SNBT and the plugin in Bukkit format, so a mod server and a plugin
server must not share the same database. Mods refuse to switch (instead of losing items) if they find plugin records.

---

## Commands

- `/swi` - Main Command.
- `/swi reload` - Reload the plugin.
- `/swi backup` - Create a backup.
- `/swi backupforce` - Force a backup. (Require Confirm)
- `/swi migrate <from> <to> [overwrite]` - Migrate storage backend. (Require Confirm)
- `/swi confirm <code>` - Confirm a critical command.
- `/swi cancel` - Cancel a pending confirmation.

---

## Permissions

- `separeworlditems.admin` - Permission to use the admin commands.

---

## How To (Compiling From Source)

Easiest: run `Code/bootstrap.cmd` (Windows) or `Code/bootstrap.sh` (Linux/macOS). It downloads any missing
JDK (portable, into `Code/.jdks/`), builds every platform and puts the jars and default configs in
`Code/dist/SepareWorldItems-<version>/` with a `BUILD-REPORT.txt`.

Manually: you need **JDK 25 or newer** to run Gradle, and git. Targets are compiled for their own Java level
(Java 8 for the oldest plugin/mod jars). NeoForge 26.x and Forge also need a **Java 25** toolchain installed.

```bash
$ git clone https://github.com/Happyuky7/SEPARE-WORLD-ITEMS.git
$ cd SEPARE-WORLD-ITEMS/Code

# Everything: plugin + Fabric 1.16.5 -> 26.3
$ ./gradlew build

# Only the Bukkit plugin
$ ./gradlew :bukkit:build

# One Fabric version
$ ./gradlew :fabric:1.21.11:build

# Minecraft 1.12.2 (Legacy Fabric, separate build)
$ cd fabric-legacy && ./gradlew build

# NeoForge / Forge (separate builds)
$ cd neoforge && ./gradlew build
$ cd forge && ./gradlew build
```

Jars are collected in `Code/build/libs/<platform>/`.
See [Code/README.md](Code/README.md) for the project layout and the multi-version workflow.

---

## Developer API

One API for the Bukkit plugin **and** the Fabric mod: `com.github.happyuky7.swi.api` (Java 8+, no platform dependency).
Your code works the same on every platform; only the player type changes (`org.bukkit.entity.Player` or `ServerPlayer`).

### Dependency

Install the API into your local Maven repository:

```bash
$ cd SEPARE-WORLD-ITEMS/Code && ./gradlew :api:publishToMavenLocal
```

Gradle (Kotlin DSL):

```kotlin
repositories { mavenLocal() }
dependencies {
    compileOnly("com.github.happyuky7:separeworlditems-api:3.0.0")
    // Fabric mods: modCompileOnly(...) on remapped versions works too, the API has no Minecraft classes.
}
```

Maven:

```xml
<dependency>
    <groupId>com.github.happyuky7</groupId>
    <artifactId>separeworlditems-api</artifactId>
    <version>3.0.0</version>
    <scope>provided</scope>
</dependency>
```

Declare the dependency so SepareWorldItems loads first: `depend: [SepareWorldItems]` (or `softdepend`) in `plugin.yml`,
`"depends": {"separeworlditems": "*"}` (or `suggests`) in `fabric.mod.json`.

### Usage

```java
SepareWorldItemsApi api = SepareWorldItems.get();          // or getIfLoaded() when optional

// Events (always on the server thread)
api.events().subscribe(GroupSwitchPreEvent.class, e -> {
    if (e.to().group().equals("pvp") && isInCombat(e.playerUuid())) e.setCancelled(true); // keeps current items
});
api.events().subscribe(GroupSwitchPostEvent.class, e -> log(e.playerName() + " " + e.from() + " -> " + e.to()));

// Where is a player?
GroupContext ctx = api.groups().currentContext(player.getUniqueId());

// Read saved data of any group (async, works with every storage backend)
api.playerData().load(uuid, GroupContext.world("survival"))
        .thenAccept(snapshot -> snapshot.ifPresent(s -> System.out.println(s.expLevel())));

// Save your own data per group (mana, skills, backpacks...)
api.sections().register(new DataSection<Player>() {          // ServerPlayer on Fabric
    public String id() { return "myplugin:mana"; }
    public Class<Player> playerType() { return Player.class; }
    public void save(Player p, DataNode out) { out.set("value", Mana.get(p)); }
    public void load(Player p, DataNode in) { Mana.set(p, in.getDouble("value", 0)); }
    public void clear(Player p) { Mana.set(p, 0); }
});
```

Server owners can toggle your section like the built-in ones: `settings.options.saves."myplugin:mana": false`.
The older Bukkit-only API (`com.github.happyuky7.separeWorldItems.api`) keeps working.

---

## Join us

* Feel free to open a PR! We accept contributions.
* [Discord](https://discord.gg/3EebYUyeUX)

---

## Aditional Information

[My website](https://happyuky7.com), My website

[Discord](https://discord.gg/3EebYUyeUX), Support My Server Discord

---

## Contributors

- [**Happyuky7**](https://github.com/Happyuky7) - Main Developer and Maintainer.
- [**HyperekDerg**](https://github.com/HyperekDerg) - Implement Backup Utility.
- [**wildmaster84**](https://github.com/wildmaster84) - Fix Folia Support and Implement auto worlds adding to config.


---

© Copyright Happyuky7 2017-2025 ©
RIGHTS RESERVED

## Special Thanks To

![IntelliJ IDEA logo](https://resources.jetbrains.com/storage/products/company/brand/logos/IntelliJ_IDEA_icon.png?size=100px) ![IntelliJ IDEA logo](https://resources.jetbrains.com/storage/products/company/brand/logos/IntelliJ_IDEA.png) ![Azul Java logo](https://www.azul.com/wp-content/themes/azul/dist/img/logo.svg)

[IntelliJ IDEA](https://www.jetbrains.com/idea/), Code editor for Java and other programming languages and programs.

[Azul Java](https://www.azul.com/) The world’s largest commercial provider of OpenJDK
