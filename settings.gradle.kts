pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }

  val koverVersion: String by settings
  val ktlintGradleVersion: String by settings

  plugins {
    id("org.jetbrains.kotlinx.kover") version koverVersion

    id("org.jlleitschuh.gradle.ktlint") version ktlintGradleVersion
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "vertx-json-path"
