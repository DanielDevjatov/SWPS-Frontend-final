// Summary (FinalFinal): Added code to *.\r\n// Purpose: document changes and explain behavior.\r\nimport com.github.gradle.node.npm.task.NpxTask

group = "org.fim"
version = "0.1.0"

plugins {
  kotlin("multiplatform") version libs.versions.kotlin apply false
  kotlin("plugin.serialization") version libs.versions.kotlin apply false
  kotlin("plugin.noarg") version libs.versions.kotlin apply false
  id("com.github.node-gradle.node") version "7.1.0"
  id("org.jetbrains.dokka") version "2.0.0"
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  dokka(project(":wallet"))
}

node {
  version = "22.4.0"
  npmVersion = "10.8.1"
  download = System.getenv("CI") == "true"
}

task<NpxTask>("nycReport") {
  dependsOn(tasks.npmInstall)
  command = "nyc"
  args = listOf("report")
}

subprojects.forEach {
  task<NpxTask>("${it.name}-nyc") {
    dependsOn(tasks.npmInstall)
    command = "nyc"
    args = if (System.getenv("CI") == "true") listOf(
      "--",
      "./gradlew",
      "-g .gradle_home",
      "--build-cache",
      "cleanTest",
      ":${it.name}:jsNodeTest"
    )
    else listOf("--", "./gradlew", "cleanTest", ":${it.name}:jsNodeTest")
  }
}

