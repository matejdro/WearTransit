import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.the
import util.commonAndroid

val libs = the<LibrariesForLibs>()

commonAndroid {
   buildFeatures {
      compose = true
   }
   composeOptions {
      kotlinCompilerExtensionVersion = libs.versions.androidx.compose.compiler.get()
   }
}

dependencies {
   add("implementation", libs.androidx.compose.ui.tooling.preview)
   add("implementation", libs.androidx.compose.wear.foundation)
   add("implementation", libs.androidx.compose.wear.material)

   add("debugImplementation", libs.androidx.compose.ui.test.manifest)
   add("debugImplementation", libs.androidx.compose.ui.tooling)

   add("androidTestImplementation", libs.androidx.compose.ui.test.junit4)
}
