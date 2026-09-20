import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    // 桌面端不是交付目标，只是为了「不开模拟器就能看到同一个 UI」：
    // commonMain 里的界面代码完全共用，desktop 只是换一个 Ktor 引擎 + 一个窗口壳。
    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        // KMP 会给 jvm target 生成一个 desktopRun（IDE 也用它的 main 跑代码），
        // 但入口要在这里单独声明：下面 compose.desktop.application 里的 mainClass
        // 只作用于它自己那套 run / 打包任务，管不到 desktopRun。
        // 少了这三行，`./gradlew :composeApp:desktopRun` 会直接报
        // 「No main class specified and classpath is not an executable jar」。
        mainRun {
            mainClass.set("com.miuier.hub.MainKt")
        }
    }

    sourceSets {
        // Android 与桌面端都是 JVM，小米 OTA 接口要用的 javax.crypto 只写一份放这里，
        // 两边各自 dependsOn，避免同一段 AES 代码抄两遍。
        val jvmCommonMain by creating { dependsOn(commonMain.get()) }
        androidMain.get().dependsOn(jvmCommonMain)
        val desktopMain by getting
        desktopMain.dependsOn(jvmCommonMain)

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            // MIUIX 已经传递依赖 compose foundation 1.12.0 与 material3-window-size-class，
            // 不需要再显式声明 material3。
            implementation(libs.miuix.ui)
            implementation(libs.miuix.icons)
            implementation(libs.miuix.preference)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            // 机型照片：coil-svg 用来兜底小米品牌图 mi.svg
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.coil.svg)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.cio)
        }
    }
}

android {
    namespace = "com.miuier.hub"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.miuier.hub"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        getByName("release") {
            // 骨架阶段先不开混淆，等接完真实数据再开
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

compose.desktop {
    application {
        mainClass = "com.miuier.hub.MainKt"
    }
}

// 无头截图：不开模拟器、不开窗口，直接把界面渲染成 PNG（见 Screenshot.kt）。
// 用来快速确认「数据能拉到 + MIUIX 能渲染」这条链路。
tasks.register<JavaExec>("screenshot") {
    group = "verification"
    description = "把界面无头渲染成 PNG，产物在 composeApp/build/screenshots"
    dependsOn("desktopMainClasses")

    val desktopMainCompilation = kotlin.targets.getByName("desktop").compilations.getByName("main")
    classpath = files(
        desktopMainCompilation.output.allOutputs,
        desktopMainCompilation.runtimeDependencyFiles,
    )
    mainClass.set("com.miuier.hub.ScreenshotKt")
    args(layout.buildDirectory.dir("screenshots").get().asFile.absolutePath)
    systemProperty("java.awt.headless", "true")
}



