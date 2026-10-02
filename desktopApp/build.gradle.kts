import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "app.prafullkumar.stats.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "Prafull Stats"
            packageVersion = "2.0.0"
            description = "Personal operating system: goals, tasks, focus, habits and diet"
            copyright = "© 2026 Prafull Kumar"
            modules("java.net.http", "jdk.crypto.ec", "java.naming")
            macOS {
                bundleID = "app.prafullkumar.stats"
                appCategory = "public.app-category.productivity"
                iconFile.set(project.file("icons/PrafullStats.icns"))
                dockName = "Prafull Stats"
            }
        }
    }
}
