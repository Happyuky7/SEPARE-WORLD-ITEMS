import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow")
}

description = "SepareWorldItems Bukkit/Spigot/Paper/Folia plugin (v3)"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://repo.onarandombox.com/content/groups/public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.viaversion.com")
}

/**
 * One jar per Java level. The sources are Java 8, so every variant contains the same features;
 * variants differ in bytecode level, bundled library versions and plugin.yml api-version.
 */
data class Variant(val java: Int, val minecraft: String, val hikari: String, val apiVersion: String)

val variants = listOf(
    Variant(8, "1.8.8 - 1.16.5", "4.0.3", "1.13"),    // HikariCP 5+ needs Java 11
    Variant(17, "1.17 - 1.20.4", "7.0.2", "1.17"),
    Variant(21, "1.20.5 - 26.x", "7.0.2", "1.20"),
)

val spigotApi = "org.spigotmc:spigot-api:26.1.2-R0.1-SNAPSHOT"

// WorldGuard 6 (1.8.8 - 1.12.2) support, compiled against the real WorldGuard 6.2 API.
val wg6: SourceSet by sourceSets.creating {
    java.srcDir("src/wg6/java")
}

// Libraries bundled in every variant (HikariCP is added per variant).
val shadeCommon: Configuration by configurations.creating
configurations.compileOnly { extendsFrom(shadeCommon) }

dependencies {
    shadeCommon(project(":api"))
    shadeCommon(project(":common"))
    shadeCommon("org.json:json:20251224")
    shadeCommon("org.xerial:sqlite-jdbc:3.51.3.0")
    shadeCommon("com.mysql:mysql-connector-j:9.0.0")
    shadeCommon("org.mariadb.jdbc:mariadb-java-client:3.5.7")
    shadeCommon("org.mongodb:mongodb-driver-sync:5.2.0")
    shadeCommon("redis.clients:jedis:5.1.5")
    shadeCommon("org.slf4j:slf4j-api:2.0.17")
    shadeCommon("org.slf4j:slf4j-nop:2.0.17")

    implementation(project(":api"))
    implementation(project(":common"))
    compileOnly("com.zaxxer:HikariCP:7.0.2") // same API as 4.0.3 for what the plugin uses

    compileOnly(spigotApi)
    compileOnly("net.essentialsx:EssentialsX:2.21.2") { isTransitive = false }
    compileOnly("com.onarandombox.multiversecore:Multiverse-Core:4.3.1") { isTransitive = false }
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.15")
    compileOnly("dev.aurelium:auraskills-api-bukkit:2.3.9")
    compileOnly("com.viaversion:viaversion-api:5.7.3-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.1.0")

    "wg6CompileOnly"(sourceSets.main.get().output)
    "wg6CompileOnly"(spigotApi)
    "wg6CompileOnly"("com.sk89q.worldguard:worldguard-legacy:6.2") { isTransitive = false }
    "wg6CompileOnly"("com.sk89q.worldedit:worldedit-core:6.1") { isTransitive = false }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 8
    options.compilerArgs.add("-Xlint:-options")
}

tasks.processResources {
    val props = mapOf("project" to mapOf("version" to project.version))
    inputs.properties(mapOf("version" to project.version))
    filesMatching("plugin.yml") {
        expand(props)
    }
}

val variantJars = variants.map { v ->
    val shade = configurations.create("shadeJava${v.java}") {
        extendsFrom(shadeCommon)
    }
    dependencies.add(shade.name, "com.zaxxer:HikariCP:${v.hikari}")

    val compileMain = tasks.register<JavaCompile>("compileJava${v.java}") {
        source = sourceSets.main.get().java
        classpath = sourceSets.main.get().compileClasspath
        destinationDirectory = layout.buildDirectory.dir("variants/java${v.java}/main")
        options.release = v.java
    }
    val compileWg6 = tasks.register<JavaCompile>("compileWg6Java${v.java}") {
        source = wg6.java
        classpath = wg6.compileClasspath
        destinationDirectory = layout.buildDirectory.dir("variants/java${v.java}/wg6")
        options.release = v.java
    }

    tasks.register<ShadowJar>("shadowJava${v.java}") {
        group = "build"
        description = "Plugin jar for Java ${v.java} servers (Minecraft ${v.minecraft})"
        from(compileMain, compileWg6, tasks.processResources)
        configurations = listOf(shade)
        archiveFileName = "SepareWorldItems-bukkit-${project.version}-java${v.java}.jar"
        destinationDirectory = rootProject.layout.buildDirectory.dir("libs/bukkit")
        filesMatching("plugin.yml") {
            filter { line -> if (line.startsWith("api-version:")) "api-version: '${v.apiVersion}'" else line }
        }

        // The public API keeps its package so other plugins can compile against separeworlditems-api.
        val lib = "com.github.happyuky7.separeWorldItems.lib"
        relocate("com.github.happyuky7.swi.common", "$lib.swicommon")
        relocate("com.zaxxer.hikari", "$lib.hikari")
        relocate("org.mongodb", "$lib.mongodb")
        relocate("com.mongodb", "$lib.mongodb2")
        relocate("org.bson", "$lib.bson")
        relocate("redis.clients", "$lib.redis")
        relocate("org.apache.commons.pool2", "$lib.pool2")
        relocate("org.slf4j", "$lib.slf4j")
        mergeServiceFiles()
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/**/module-info.class", "module-info.class")
    }
}

tasks.shadowJar {
    enabled = false
}

tasks.jar {
    archiveClassifier = "plain"
}

tasks.build {
    dependsOn(variantJars)
}
