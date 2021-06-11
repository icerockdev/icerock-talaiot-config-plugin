/*
 * Copyright 2021 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

buildscript {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath("dev.icerock.gradle:icerock-talaiot-config")
    }
}

apply(plugin = "dev.icerock.gradle.talaiot")

tasks.create("hello") {
    group = "testing"

    doFirst {
        println("Hello world!")
    }
}
