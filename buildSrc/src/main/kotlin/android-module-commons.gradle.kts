import org.gradle.accessors.dm.LibrariesForLibs
import util.commonAndroid

plugins {
   id("org.jetbrains.kotlin.android")

   id("checks")
   id("org.gradle.android.cache-fix")
}

val libs = the<LibrariesForLibs>()

commonAndroid {
   compileSdk = 34

   compileOptions {
      sourceCompatibility = JavaVersion.VERSION_17
      targetCompatibility = JavaVersion.VERSION_17

      isCoreLibraryDesugaringEnabled = true
   }

   defaultConfig {
      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
   }

   packaging {
      resources {
         excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
   }
}

kotlin {
   jvmToolchain(17)
}

dependencies {
   add("coreLibraryDesugaring", libs.desugarJdkLib)
}
