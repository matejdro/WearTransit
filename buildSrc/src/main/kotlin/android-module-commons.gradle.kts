import util.commonAndroid

plugins {
   id("org.jetbrains.kotlin.android")

   id("checks")
   id("org.gradle.android.cache-fix")
}

commonAndroid {
   compileSdk = 34

   compileOptions {
      sourceCompatibility = JavaVersion.VERSION_17
      targetCompatibility = JavaVersion.VERSION_17
   }

   defaultConfig {
      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
   }

   packagingOptions {
      resources {
         excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
   }
}

kotlin {
   jvmToolchain(17)
}
