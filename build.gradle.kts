plugins {
  kotlin("jvm") version "2.0.0"

  id("org.jetbrains.kotlinx.kover")
  id("org.sonarqube") version "3.5.0.2730"

  id("org.jlleitschuh.gradle.ktlint")
}

group = "com.kobil.vertx"
version = "1.0.0"

repositories {
  mavenCentral()
}

val arrowVersion: String by project
val caffeineVersion: String by project
val kotlinCoroutinesVersion: String by project
val vertxVersion: String by project

val junitJupiterVersion: String by project
val kotestVersion: String by project
val kotestArrowVersion: String by project

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

  implementation(platform("io.arrow-kt:arrow-stack:$arrowVersion"))
  implementation("io.arrow-kt:arrow-core")

  implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")

  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-core")
  implementation("io.vertx:vertx-lang-kotlin")
  implementation("io.vertx:vertx-lang-kotlin-coroutines")

  testImplementation(platform("io.kotest:kotest-bom:$kotestVersion"))
  testImplementation("io.kotest:kotest-runner-junit5")
  testImplementation("io.kotest:kotest-assertions-core")
  testImplementation("io.kotest:kotest-property")
  testImplementation("io.kotest.extensions:kotest-assertions-arrow:$kotestArrowVersion")

  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}

tasks.test {
  useJUnitPlatform()

  finalizedBy(tasks.koverHtmlReport, tasks.koverXmlReport)
}

kotlin {
  jvmToolchain(21)
}

ktlint {
  version.set("1.3.1")
}

kover {
  reports {
    filters {
      excludes {
        classes("com.kobil.vertx.jsonpath.Build", "com.kobil.vertx.jsonpath.Compile")
      }
    }
  }
}
