plugins {
    `java-library`
    `maven-publish`
}

description = "Platform-independent API for SepareWorldItems (Bukkit plugin and Fabric mod)."

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Java 8 bytecode: the API must load on 1.12.2 / 1.16.5 servers too.
    options.release = 8
    options.compilerArgs.add("-Xlint:-options")
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        addStringOption("Xdoclint:none", "-quiet")
    }
}

dependencies {
    compileOnlyApi("org.jetbrains:annotations:26.1.0")
}

base {
    archivesName = "separeworlditems-api"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "separeworlditems-api"
            from(components["java"])
            pom {
                name = "SepareWorldItems API"
                description = project.description
                url = "https://github.com/Happyuky7/SepareWorldItems"
                licenses {
                    license {
                        name = "See LICENSE.txt"
                        url = "https://github.com/Happyuky7/SepareWorldItems/blob/master/LICENSE.txt"
                    }
                }
                developers {
                    developer {
                        id = "Happyuky7"
                        name = "Happyuky7"
                    }
                }
                scm {
                    url = "https://github.com/Happyuky7/SepareWorldItems"
                }
            }
        }
    }
    repositories {
        // ./gradlew :api:publishAllPublicationsToProjectRepository -> Code/build/repo (upload it anywhere).
        maven {
            name = "project"
            url = rootProject.layout.buildDirectory.dir("repo").get().asFile.toURI()
        }
    }
}
