/*
 * Copyright 2021 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("jvm") version ("1.4.31")
    kotlin("plugin.serialization") version ("1.4.31")
    id("com.gradle.plugin-publish") version ("0.14.0")
    id("com.github.kukuhyoniatmoko.buildconfigkotlin") version ("1.0.5")
    id("java-gradle-plugin")
    id("org.gradle.maven-publish")
}

group = "dev.icerock.gradle"
version = "2.1.0"

repositories {
    jcenter()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation("io.github.cdsap:talaiot:1.5.0")
    implementation("io.github.cdsap.talaiot:talaiot:1.5.0")
    implementation("com.influxdb:influxdb-client-kotlin:2.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
}

buildConfigKotlin {
    sourceSet("main") {
        buildConfig("influxUrl", properties["influx.url"] as String)
        buildConfig("influxOrg", properties["influx.org"] as String)
        buildConfig("influxBucket", properties["influx.bucket"] as String)
        buildConfig("influxToken", properties["influx.token"] as String)
        buildConfig("slackWebHook", properties["slack.webhook"] as String)
    }
}

gradlePlugin {
    plugins {
        create("icerock-talaiot") {
            id = "dev.icerock.gradle.talaiot"
            implementationClass = "dev.icerock.gradle.talaiot.TalaiotConfigPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/icerockdev/icerock-talaiot-config-plugin"
    vcsUrl = "https://github.com/icerockdev/icerock-talaiot-config-plugin"
    description =
        "Wrapper for https://github.com/cdsap/Talaiot with predefined configuration for IceRock projects analytics"
    tags = listOf("talaiot")

    plugins {
        getByName("icerock-talaiot") {
            displayName = "Talaiot Wrapper for IceRock projects"
        }
    }

    mavenCoordinates {
        groupId = project.group as String
        artifactId = project.name
        version = project.version as String
    }
}
