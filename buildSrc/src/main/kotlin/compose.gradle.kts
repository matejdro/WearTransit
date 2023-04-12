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
   add("implementation", libs.androidx.compose.ui)
   add("implementation", libs.androidx.compose.ui.graphics)
   add("implementation", libs.androidx.compose.ui.tooling.preview)
   add("implementation", libs.androidx.compose.material3)

   add("debugImplementation", libs.androidx.compose.ui.test.manifest)
   add("debugImplementation", libs.androidx.compose.ui.tooling)

   add("androidTestImplementation", libs.androidx.compose.ui.test.junit4)
}
