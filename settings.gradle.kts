// https://youtrack.jetbrains.com/issue/KTIJ-19369
// AGP 7.4.0 has a bug where it marks most things as incubating
@file:Suppress("DSL_SCOPE_VIOLATION", "UnstableApiUsage")

pluginManagement {
   repositories {
      google()
      mavenCentral()
      gradlePluginPortal()
   }
}

dependencyResolutionManagement {
   repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

   repositories {
      google()
      mavenCentral()
   }

   versionCatalogs {
      create("libs") {
         from(files("config/libs.toml"))
      }
   }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "WearTransit"

include(":mobile")
