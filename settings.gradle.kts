// Summary (FinalFinal): Added code to *.\r\n// Purpose: document changes and explain behavior.\r\nrootProject.name = "deer-prototype"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage")
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }

  versionCatalogs {
    create("libs") {
      version("kotlin", "2.0.20")
    }

    create("kotlinWrappers") {
      val wrappersVersion = "0.0.1-pre.814"
      from("org.jetbrains.kotlin-wrappers:kotlin-wrappers-catalog:$wrappersVersion")
    }
  }
}

include(":wallet")
include(":aggregator")

