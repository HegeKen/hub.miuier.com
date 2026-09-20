// MiROMS HUB Android —— Gradle 设置
//
// 版本矩阵（互相兼容，见 gradle/libs.versions.toml 注释）：
//   Gradle 9.4.1 · AGP 9.4.1 · Kotlin 2.4.20 · Compose Multiplatform 1.12.0 · MIUIX 0.9.4
// MIUIX 0.9.4 自身要求 Kotlin 2.4.20 + Compose Multiplatform 1.12.0，这两项不可随意升降。
pluginManagement {
    repositories {
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
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "miroms-hub-android"

include(":composeApp")
