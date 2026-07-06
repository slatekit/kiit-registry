pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/slatekit/kiit")
            credentials {
                username = System.getenv("GIT_PACKAGES_INSTALL_ACTOR")
                password = System.getenv("GIT_PACKAGES_INSTALL_TOKEN")
            }
        }
    }
}

rootProject.name = "kiit-ace"
