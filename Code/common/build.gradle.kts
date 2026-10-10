plugins {
    `java-library`
    `maven-publish`
}

description = "Shared SepareWorldItems logic used by the Bukkit plugin and the Fabric mod."

java {
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 8
    options.compilerArgs.add("-Xlint:-options")
}

dependencies {
    api(project(":api"))
    compileOnly("org.jetbrains:annotations:26.1.0")
    // Provided by Bukkit at runtime; bundled (jar-in-jar) by the Fabric mod.
    compileOnly("org.yaml:snakeyaml:2.7")
    // Remote storage drivers: bundled by the plugin and the mods, optional at runtime.
    compileOnly("org.mongodb:mongodb-driver-sync:5.2.0")
    compileOnly("redis.clients:jedis:5.1.5")

    testImplementation("org.yaml:snakeyaml:2.7")
    testImplementation("org.xerial:sqlite-jdbc:3.53.4.0")
    testImplementation("org.mongodb:mongodb-driver-sync:5.2.0")
    testImplementation("redis.clients:jedis:5.1.5")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

base {
    archivesName = "separeworlditems-common"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "separeworlditems-common"
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "project"
            url = rootProject.layout.buildDirectory.dir("repo").get().asFile.toURI()
        }
    }
}
