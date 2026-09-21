import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// ── 版本号 ────────────────────────────────────────────────────────────────
// 状态只有一个：app/android/version.properties 里的 versionCode。
// versionName 按约定拼成 0.1.<versionCode>，所以自增 versionCode 时两个一起走。
// CI 上手动填了 version_code / version_name 时用 -P 覆盖（见 .github/workflows/android-build.yml）。
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    versionPropsFile.inputStream().use { stream -> load(stream) }
}
val fileVersionCode = versionProps.getProperty("versionCode")
    ?.trim()
    ?.toIntOrNull()
    ?: error("${versionPropsFile.path} 里的 versionCode 缺失或不是整数")

val effectiveVersionCode = providers.gradleProperty("versionCode").orNull?.trim()?.toIntOrNull()
    ?: fileVersionCode
val effectiveVersionName = providers.gradleProperty("versionName").orNull?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: "0.1.$effectiveVersionCode"

// 自增 versionCode。刻意不做成每次构建自动 +1 —— 那样本地随便编一次也会把号吃掉，
// 而且 CI 与本地会各涨一份，数字就乱了。发版前显式跑一次、连同代码一起提交。
tasks.register("bumpVersion") {
    group = "versioning"
    description = "把 version.properties 里的 versionCode 自增 1（versionName 随之变成 0.1.<新值>）"
    doLast {
        val next = fileVersionCode + 1
        val text = versionPropsFile.readText()
        val bumped = text.replace(
            Regex("(?m)^versionCode\\s*=.*$"),
            "versionCode=$next",
        )
        check(bumped != text) { "${versionPropsFile.name} 里没找到 versionCode 那一行" }
        versionPropsFile.writeText(bumped)
        println("versionCode $fileVersionCode → $next，versionName 变成 0.1.$next（记得提交 ${versionPropsFile.name}）")
    }
}

// 把最终生效的版本号落盘，供 CI 命名产物 / 打 tag。
// 由构建自己产出，workflow 里就不用再抄一遍「0.1.<versionCode>」这条规则。
val writeResolvedVersion = tasks.register("writeResolvedVersion") {
    val outFile = layout.buildDirectory.file("resolved-version.txt")
    // 版本来自这两个地方：文件里的 versionCode + 命令行覆盖值。
    // 不声明成输入的话，改完 version.properties 再构建会命中「已是最新」而被跳过。
    inputs.file(versionPropsFile)
    inputs.property("versionName", effectiveVersionName)
    inputs.property("versionCode", effectiveVersionCode)
    outputs.file(outFile)
    doLast { outFile.get().asFile.writeText("$effectiveVersionName\n$effectiveVersionCode\n") }
}
tasks.matching { it.name == "assembleDebug" || it.name == "assembleRelease" }
    .configureEach { dependsOn(writeResolvedVersion) }

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
        // 自增版本号，来源见文件顶部：version.properties 的 versionCode + 约定前缀 0.1
        versionCode = effectiveVersionCode
        versionName = effectiveVersionName
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



