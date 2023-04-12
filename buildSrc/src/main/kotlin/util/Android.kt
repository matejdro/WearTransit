package util

import com.android.build.api.dsl.BuildFeatures
import com.android.build.api.dsl.BuildType
import com.android.build.api.dsl.DefaultConfig
import com.android.build.api.dsl.ProductFlavor
import com.android.build.gradle.internal.dsl.InternalTestedExtension
import com.android.build.gradle.internal.utils.KOTLIN_ANDROID_PLUGIN_ID
import org.gradle.api.Action
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

/**
 * android {} block that can be used without applying specific android plugin
 */
fun Project.commonAndroid(
   block: Action<InternalTestedExtension<
         BuildFeatures,
         BuildType,
         DefaultConfig,
         ProductFlavor>>
) {
   (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("android", block)
}

/**
 * kotlinOptions {} block that can be used without applying specific android plugin
 */
fun InternalTestedExtension<*, *, *, *>.commonKotlinOptions(
   block: Action<KotlinJvmOptions>
) {
   (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("kotlinOptions", block)
}

fun Project.isAndroidProject(): Boolean {
   return pluginManager.hasPlugin(KOTLIN_ANDROID_PLUGIN_ID)
}
