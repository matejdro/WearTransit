// https://youtrack.jetbrains.com/issue/KTIJ-19369
// AGP 7.4.0 has a bug where it marks most things as incubating
@file:Suppress("DSL_SCOPE_VIOLATION", "UnstableApiUsage")

import com.android.build.api.variant.BuildConfigField

plugins {
   id("com.android.application")
   id("kotlin-kapt")
   commonAndroid
   compose
}

android {
   namespace = "com.matejdro.weartransit"

   defaultConfig {
      applicationId = "com.matejdro.weartransit"
      minSdk = 26
      targetSdk = 33
      versionCode = 1
      versionName = "1.0"

      androidComponents {
         onVariants {
            it.buildConfigFields.put("GIT_HASH", gitVersionProvider.flatMap { task ->
               task.gitVersionOutputFile.map { file ->
                  val gitHash = file.asFile.readText(Charsets.UTF_8)

                  BuildConfigField(
                     "String",
                     "\"$gitHash\"",
                     "Git Version"
                  )
               }
            })
         }
      }
   }


   signingConfigs {
      getByName("debug") {
         // SHA1: 22:F7:AC:97:62:55:D7:7A:E2:DC:E4:CA:F9:25:8E:78:2C:81:B8:4B
         // SHA256: A3:6D:9C:00:15:1C:D8:92:65:71:5B:58:6F:FE:18:BD:18:7B:2D:04:5C:33:5E:A7:EE:A5:A7:50:C3:59:45:80

         storeFile = File(rootDir, "keys/debug.jks")
         storePassword = "android"
         keyAlias = "androiddebugkey"
         keyPassword = "android"
      }
      create("release") {
         // SHA1: 7D:0A:F7:8B:87:0D:8F:16:76:0F:AC:2A:E9:41:22:B8:CF:A6:BB:AE
         // SHA256: 53:6C:E1:BC:29:76:C4:67:12:1D:8F:A5:A1:58:E7:FF:47:7A:06:CC:75:0E:CD:17:49:C5:45:13:F0:64:70:7F

         storeFile = File(rootDir, "keys/release.jks")
         storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
         keyAlias = "app"
         keyPassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
      }
   }

   buildTypes {
      getByName("release") {
         isMinifyEnabled = true
         proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
         )

         signingConfig = signingConfigs.getByName("release")
      }
   }
}

dependencies {
   implementation(projects.common)

   implementation(libs.androidx.activity.compose)
   implementation(libs.androidx.core)
   implementation(libs.androidx.lifecycle.runtime)
   implementation(libs.kotlin.coroutines.playServices)
   implementation(libs.kotlinova.core)
   implementation(libs.logcat)
   implementation(libs.moshi.runtime)
   implementation(libs.playServices.wear)
   implementation(libs.wire.runtime)
   implementation(libs.wire.moshi)

   kapt(libs.moshi.compiler)

   testImplementation(libs.junit4)
}

abstract class GitVersionTask : DefaultTask() {
   @get:OutputFile
   abstract val gitVersionOutputFile: RegularFileProperty

   @TaskAction
   fun taskAction() {
      val gitProcess = ProcessBuilder("git", "rev-parse", "--short", "HEAD").start()
      val error = gitProcess.errorStream.readBytes().decodeToString()
      if (error.isNotBlank()) {
         throw IllegalStateException("Git error : $error")
      }

      val gitVersion = gitProcess.inputStream.readBytes().decodeToString().trim()

      gitVersionOutputFile.get().asFile.writeText(gitVersion)
   }
}

val gitVersionProvider = tasks.register<GitVersionTask>("gitVersionProvider") {
   val targetFile = File(project.buildDir, "intermediates/gitVersionProvider/output")

   targetFile.also {
      it.parentFile.mkdirs()
      gitVersionOutputFile.set(it)
   }
   outputs.upToDateWhen { false }
}
