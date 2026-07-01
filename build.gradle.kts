// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

subprojects {
    plugins.withId("com.google.devtools.ksp") {
        tasks.matching { it.name.startsWith("ksp") }.configureEach {
            outputs.cacheIf { false }
            inputs.property("kspProcessorVersion", "2.0")
        }
    }
}
