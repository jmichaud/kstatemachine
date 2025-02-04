plugins {
    kotlin("multiplatform")
    application
}

group = rootProject.group
version = rootProject.version

kotlin {
    jvmToolchain(Versions.jdkVersion)
    jvm {}

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kstatemachine-coroutines"))
            }
        }
    }
}
