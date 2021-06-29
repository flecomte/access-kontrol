plugins {
    jacoco
    `maven-publish`
    kotlin("jvm") version "+"
    id("net.nemerosa.versioning") version "+"
    id("org.jlleitschuh.gradle.ktlint") version "+"
    id("org.sonarqube") version "+"
}

group = "io.github.flecomte"
version = versioning.info.run {
    if (dirty) {
        versioning.info.full
    } else {
        versioning.info.lastTag
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        sourceCompatibility = "11"
        targetCompatibility = "11"
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
    repositories {
        maven {
            name = "access-control"
            url = uri("https://maven.pkg.github.com/flecomte/access-control")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("access-control") {
            from(components["java"])
            artifact(sourcesJar)
        }
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf {
        versioning.info.run {
            !dirty && tag != null && tag.matches("""[0-9]+\.[0-9]+\.[0-9]+""".toRegex())
        }
    }

    dependsOn(tasks.test)
}

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.+")
}
