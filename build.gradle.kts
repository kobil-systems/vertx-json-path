import org.jetbrains.dokka.gradle.DokkaTask

plugins {
  kotlin("jvm") version "2.0.0"

  `java-library`
  `maven-publish`

  id("org.jetbrains.kotlinx.kover")
  id("org.sonarqube") version "3.5.0.2730"

  id("org.jetbrains.dokka") version "1.9.20"

  id("org.jlleitschuh.gradle.ktlint")
}

group = "com.kobil.vertx"
version = "1.0.0-SNAPSHOT"

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
  jvmToolchain(17)

  compilerOptions {
    allWarningsAsErrors.set(true)
  }
}

java {
  withSourcesJar()
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

tasks.withType<DokkaTask>().configureEach {
  suppressObviousFunctions.set(true)
  failOnWarning.set(true)

  dokkaSourceSets.configureEach {
    reportUndocumented.set(true)
  }
}

val dokkaJavadocJar: Jar by tasks.creating(Jar::class) {
  group = "documentation"

  dependsOn(tasks.dokkaJavadoc)
  from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
  archiveClassifier.set("javadoc")
}

tasks.assemble.configure {
  dependsOn(dokkaJavadocJar)
}

val documentationElements: Configuration by configurations.creating {
  outgoing {
    artifact(dokkaJavadocJar) {
      type = "jar"
      classifier = "javadoc"
      builtBy(dokkaJavadocJar)
    }
  }

  attributes {
    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class, Category.DOCUMENTATION))
    attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class, Bundling.EXTERNAL))
    attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType::class, DocsType.JAVADOC))
    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, Usage.JAVA_RUNTIME))
  }
}

components.getByName<AdhocComponentWithVariants>("java") {
  addVariantsFromConfiguration(documentationElements) {}
}

publishing {
  publications {
    create<MavenPublication>("vertx-json-path") {
      artifactId = "vertx-json-path"
      from(components["java"])
    }
  }
}
