// https://youtrack.jetbrains.com/issue/KTIJ-19369
// AGP 7.4.0 has a bug where it marks most things as incubating
@file:Suppress("DSL_SCOPE_VIOLATION", "UnstableApiUsage")

dependencyResolutionManagement {
   versionCatalogs {
      create("libs") {
         from(files("../config/libs.toml"))
      }
   }
}

rootProject.name = "buildSrc"
