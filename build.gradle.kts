plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "ewc.openspec"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("com.intellij.java")
    }
    implementation("org.commonmark:commonmark:0.24.0")
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        name = "Test to Spec"
        ideaVersion {
            sinceBuild = "243"
            untilBuild = provider { null }
        }
    }
}
