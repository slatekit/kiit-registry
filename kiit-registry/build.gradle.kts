plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.vanniktech.publish)
}

group = "dev.kiit"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
}

dependencies {
    //implementation(libs.kiit.entities)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("dev.kiit", "kiit-registry", version.toString())

    pom {
        name.set("kiit-registry")
        description.set("A service locator that knows what it registered.")
        url.set("https://github.com/slatekit/kiit-registry")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("author")
                name.set("author")
                email.set("author")
                organization.set("CodeHelix")
            }
        }
        scm {
            url.set("https://github.com/slatekit/kiit-registry")
            connection.set("scm:git:https://github.com/slatekit/kiit-registry.git")
            developerConnection.set("scm:git:ssh://git@github.com/slatekit/kiit-registry.git")
        }
    }
}
