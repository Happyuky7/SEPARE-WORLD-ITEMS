pluginManagement {
    // Shared build logic (multi-version preprocessor), also used by neoforge/ and forge/.
    includeBuild("build-logic")
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "SepareWorldItems"

// Platform-agnostic modules (Java 8 bytecode so every target, including 1.12.2, can use them).
include("api", "common")

// Bukkit/Spigot/Paper/Folia plugin (v3). The untouched v2 plugin lives in SepareWorldItems-Original/ (Maven).
include("bukkit")
project(":bukkit").projectDir = file("bukkit")

// Fabric mod: one shared source tree (fabric/src) preprocessed for every Minecraft version.
// Each entry needs a folder fabric/versions/<mc>/ with its gradle.properties.
val fabricVersions = listOf("1.16.5", "1.18.2", "1.19.4", "1.20.6", "1.21.1", "1.21.11", "26.1.2", "26.2", "26.3")
include("fabric")
for (mc in fabricVersions) {
    include("fabric:$mc")
    project(":fabric:$mc").projectDir = file("fabric/versions/$mc")
}

// 1.12.2 uses Legacy Fabric (its own Loom fork), so it lives in a separate build: fabric-legacy/.
