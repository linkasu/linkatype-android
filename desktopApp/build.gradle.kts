plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(21)
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "ru.ibakaidov.distypepro.desktopapp.MainKt"
        
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "LINKa Type"
            packageVersion = "3.2.0"
            
            macOS {
                bundleID = "ru.ibakaidov.distypepro.desktopapp"
                iconFile.set(project.file("icon.icns"))
            }
            
            windows {
                menuGroup = "LINKa Type"
                upgradeUuid = "18159995-d967-4cd2-8885-77BFA97CFA9F"
                iconFile.set(project.file("icon.ico"))
            }
            
            linux {
                iconFile.set(project.file("icon.png"))
            }
        }
    }
}