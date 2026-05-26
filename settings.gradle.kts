// Toolchain resolver: lets Gradle automatically download the required JDK
// (declared as jvmToolchain(N) in submodules) if it is not already
// installed locally. This makes the project reproducible on any machine.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "tfg-movies-recsys"

include(":server")

// Future modules — uncomment as they get created:
include(":shared")
// include(":composeApp")