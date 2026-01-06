@file:OptIn(ExperimentalStdlibApi::class)

import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import java.net.URI

group = "org.fim.wallet"
version = "0.1.0"

plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization")
  kotlin("plugin.noarg")
  id("org.jetbrains.dokka")
  id("maven-publish")
  id("dev.opensavvy.resources.producer") version "0.5.1"
}

dokka {
  dokkaSourceSets.configureEach {
    documentedVisibilities(
      VisibilityModifier.Public,
      VisibilityModifier.Private,
      VisibilityModifier.Protected,
      VisibilityModifier.Internal,
      VisibilityModifier.Package
    )

    externalDocumentationLinks {
      register("serialization") { url.set(URI("https://kotlinlang.org/api/kotlinx.serialization/")) }
      register("datetime") {
        url.set(URI("https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime/"))
        packageListUrl.set(URI("https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/package-list"))
      }
      register("coroutines") {
        url.set(URI("https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/"))
        packageListUrl.set(URI("https://kotlinlang.org/api/kotlinx.coroutines/package-list"))
      }
      register("kotlincrypto") { url.set(URI("https://hash.kotlincrypto.org/")) }
    }
  }
}

kotlin {
  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  compilerOptions {
    freeCompilerArgs.add("-Xexpect-actual-classes")
  }

  noArg {
    annotation("org.fim.wallet.domain.NoArgConstructor")
  }

  js(IR) {
    nodejs {
      testTask {
        useMocha {
          timeout = "600000"
        }
      }
    }
    binaries.executable()
  }

  if (System.getenv("DOKKA") == "true") jvm()

  sourceSets {
    all {
      languageSettings.optIn("kotlin.uuid.ExperimentalUuidApi")
    }
    commonMain.dependencies {
      api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
      implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
      implementation(project.dependencies.platform("org.kotlincrypto.hash:bom:0.5.1"))
      implementation("org.kotlincrypto.hash:md")
      api("com.ionspin.kotlin:bignum:0.3.10")
      implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
      implementation("io.github.oshai:kotlin-logging:7.0.0")
    }
    commonTest.dependencies {
      implementation(kotlin("test"))
      implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    }
    jsMain.dependencies {
      implementation(npm("snarkjs", ">=0.7.4"))
      implementation(npm("circomlibjs", ">=0.1.7"))
      implementation(kotlinWrappers.node)

      // Express dependencies
      implementation(npm("express", "4.18.2"))
      implementation(npm("swagger-jsdoc", "6.2.8"))
      implementation(npm("swagger-ui-express", "4.6.3"))
      implementation(npm("body-parser", ">=2.2.0")) // or latest
    }
    jsTest.dependencies { }
    dependencies {

    }
  }
}

tasks.withType<Copy>().configureEach {
  duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.named("jsProcessResources", ProcessResources::class) {
  exclude("**/circomlib/**")
  exclude("**/snarkjs_ceremony/circomlib/**")
}

publishing {
  repositories {
    // Zum Testen lokal veröffentlichen
    //mavenLocal()
    maven {
      url = uri("https://gitlab.cc-asp.fraunhofer.de/api/v4/projects/64448/packages/maven")
      credentials(HttpHeaderCredentials::class) {
        name =  if (System.getenv("CI") == "true") "Job-Token" else "Private-Token"
        value = if (System.getenv("CI") == "true") System.getenv("CI_JOB_TOKEN")
        else findProperty("gitLabPrivateToken") as String? // the variable resides in $GRADLE_USER_HOME/gradle.properties
      }
      authentication {
        create("header", HttpHeaderAuthentication::class)
      }
    }
  }
}
