/*
 * Copyright 2021 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

pluginManagement {
    includeBuild("plugin")

    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.icerock.gradle.talaiot")
}
