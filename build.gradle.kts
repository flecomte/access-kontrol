plugins {
    jacoco
    `maven-publish`
    kotlin("jvm") version "+"
    id("net.nemerosa.versioning") version "+"
    id("org.sonarqube") version "+"
}

group "io.github.flecomte"
version = versioning.info.run {
    if (dirty) {
        versioning.info.full
    } else {
        versioning.info.lastTag
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
    }
}

tasks.sonarqube.configure {
    dependsOn(tasks.jacocoTestReport)
}

val sourcesJar by tasks.registering(Jar::class) {
    group = "build"
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

publishing {
    if (versioning.info.dirty == false) {
        repositories {
            maven {
                name = "skilningur"
                group = "io.github.flecomte"
                url = uri("https://maven.pkg.github.com/flecomte/skilningur")
                credentials {
                    username = System.getenv("GITHUB_USERNAME")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }

        publications {
            create<MavenPublication>("skilningur") {
                from(components["java"])
                artifact(sourcesJar)
            }
        }
    } else {
        org.slf4j.LoggerFactory.getLogger("gradle")
                .error("The git is DIRTY (${versioning.info.full})")
    }
}

repositories {
    mavenCentral()
}