pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        google()
        mavenCentral()
    }
}

rootProject.name = "Sample"
include(":app")

// Core
include(":core:base")
include(":core:network")
include(":core:storage")
include(":core:cache")
include(":core:common")
include(":core:ui")

// Service
include(":service:startup")
include(":service:crash")
include(":service:logger")

// Middleware
include(":middleware:router")
include(":middleware:router-annotations")
include(":middleware:router-compiler")
include(":middleware:permission")

// Features
// include(":features:feature_login") // removed - use as template for new features
 