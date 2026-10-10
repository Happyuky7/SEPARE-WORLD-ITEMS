plugins {
    // Declared once here so every Fabric version subproject shares the same plugin classloader.
    id("net.fabricmc.fabric-loom") version "1.18.2" apply false
    id("net.fabricmc.fabric-loom-remap") version "1.18.2" apply false
    id("com.gradleup.shadow") version "9.6.1" apply false
    id("swi.preprocess") apply false
}

allprojects {
    group = property("group") as String
    version = property("version") as String

    repositories {
        mavenCentral()
    }
}
