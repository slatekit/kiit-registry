plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "dev.kiit"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "sample.SampleAppKt"
}

dependencies {
    //implementation(libs.kiit.entities)

    testImplementation(kotlin("test"))
}

// Todo: Publish to maven central
tasks.test {
    useJUnitPlatform()
}

