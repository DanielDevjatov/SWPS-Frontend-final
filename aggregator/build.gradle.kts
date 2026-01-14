// Summary (FinalFinal): Added code to *.\r\n// Purpose: document changes and explain behavior.\r\nimport org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

group = "org.fim.deer.aggregator"
version = "0.1.0"

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.dokka")
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
  }
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

  js(IR) {
    nodejs {
      testTask { }
    }
    binaries.executable()
  }
  if (System.getenv("DOKKA") == "true") jvm()

  sourceSets {
    commonMain.dependencies {
    }
    commonTest.dependencies {
      implementation(kotlin("test"))
    }
    jsMain.dependencies {

    }
    jsTest.dependencies {
      implementation(kotlin("test"))
    }
    dependencies { }
  }
}

