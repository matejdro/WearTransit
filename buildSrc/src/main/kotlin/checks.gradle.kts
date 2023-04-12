import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.the
import util.commonAndroid
import util.isAndroidProject

val libs = the<LibrariesForLibs>()

plugins {
   id("com.github.ben-manes.versions")
   id("io.gitlab.arturbosch.detekt")
}

if (isAndroidProject()) {
   commonAndroid {
      lint {
         lintConfig = file("$rootDir/config/android-lint.xml")
         abortOnError = true

         warningsAsErrors = true
      }
   }
}

tasks.withType<DependencyUpdatesTask> {
   gradleReleaseChannel = "current"

   rejectVersionIf {
      candidate.version.contains("alpha", ignoreCase = true) ||
         candidate.version.contains("beta", ignoreCase = true) ||
         candidate.version.contains("RC", ignoreCase = true) ||
         candidate.version.contains("M", ignoreCase = true) ||
         candidate.version.contains("eap", ignoreCase = true)
   }
}

detekt {
   config = files("$rootDir/config/detekt.yml")
}

dependencies {
   detektPlugins(libs.detekt.formatting)
}
