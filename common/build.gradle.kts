plugins {
   id("com.android.library")
   id("org.jetbrains.kotlin.android")
   commonAndroid
   alias(libs.plugins.wire)
}

android {
   namespace = "com.matejdro.weartransit.common"

   defaultConfig {
      minSdk = 26
   }
}

wire {
   kotlin {
      android = true
   }
}

dependencies {
   implementation(libs.wire.runtime)
}
