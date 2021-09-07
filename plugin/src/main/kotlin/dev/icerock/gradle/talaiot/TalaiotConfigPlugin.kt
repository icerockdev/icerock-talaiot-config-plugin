/*
 * Copyright 2021 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.gradle.talaiot

import dev.icerock.gradle.talaiot.BuildConfig
import com.gradle.enterprise.gradleplugin.GradleEnterprisePlugin
import io.github.cdsap.talaiot.plugin.influxdb.InfluxdbExtension
import io.github.cdsap.talaiot.plugin.influxdb.TalaiotInfluxdbPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.gradleEnterprise

class TalaiotConfigPlugin : Plugin<Settings> {
    private val talaiotPlugin = TalaiotInfluxdbPlugin()
    private val gradleScanPlugin = GradleEnterprisePlugin()

    override fun apply(target: Settings) {
        gradleScanPlugin.apply(target)

        target.gradleEnterprise {
            buildScan {
                it.publishAlways()
                it.termsOfServiceUrl = "https://gradle.com/terms-of-service"
                it.termsOfServiceAgree = "yes"
                it.isUploadInBackground = true
            }
        }

        target.gradle.rootProject { configureRootProject(it) }
    }

    private fun configureRootProject(target: Project) {
        talaiotPlugin.apply(target)

        target.talaiot {
            publishers {
                customPublishers(
                    InfluxDbPublisher(
                        configuration = InfluxDbPublisher.Configuration(
                            url = BuildConfig.influxUrl,
                            org = BuildConfig.influxOrg,
                            bucket = BuildConfig.influxBucket,
                            token = BuildConfig.influxToken,
                            publishTaskMetrics = false
                        ),
                        logger = project.logger
                    )
                )
            }
            metrics.customTaskMetrics("projectName" to target.name)
        }

        configureConfigurationsMetrics(target)
        configureMobilePluginMetric(target)
    }

    private fun configureConfigurationsMetrics(target: Project) {
        target.allprojects { project ->
            project.configurations.configureEach { configuration ->
                configureConfigurationsMetrics(configuration, target)
                configureDependenciesMetric(configuration, target)
            }
        }
    }

    private fun configureConfigurationsMetrics(
        configuration: Configuration,
        target: Project
    ) {
        configuration.dependencies.matching { dependency ->
            dependency.group == "org.jetbrains.kotlin" && dependency.name.startsWith("kotlin-stdlib")
        }.configureEach { dependency ->
            val version = dependency.version ?: return@configureEach

            target.talaiot {
                metrics.customTaskMetrics("kotlinVersion" to version)
                metrics.customBuildMetrics("kotlinVersion" to version)
            }
        }
    }

    private fun configureDependenciesMetric(
        configuration: Configuration,
        target: Project
    ) {
        configuration.dependencies.matching { dependency ->
            val group = dependency.group ?: return@matching false
            if (!group.startsWith("dev.icerock")) return@matching false

            val ignoreList =
                listOf("iosx64", "iosarm64", "metadata", "android", "android-debug")
            if (ignoreList.any { dependency.name.endsWith("-$it") }) return@matching false

            return@matching true
        }.configureEach { dependency ->
            val version = dependency.version ?: return@configureEach
            val name = dependency.name
            val group = dependency.group.orEmpty()
            val metricName = "$group:$name"

            target.talaiot {
                metrics.customBuildMetrics(metricName to version)
            }
        }
    }

    private fun configureMobilePluginMetric(target: Project) {
        val resPath = "META-INF/gradle-plugins/dev.icerock.mobile.multiplatform.properties"

        target.allprojects { project ->
            project.plugins.withId("dev.icerock.mobile.multiplatform") {
                val url = it.javaClass.classLoader.getResource(resPath) ?: return@withId
                val urlString = url.toString()
                val regex = "mobile-multiplatform-(.*)\\.jar".toRegex()
                val version = regex.find(urlString)?.groupValues?.get(1) ?: return@withId

                target.talaiot {
                    metrics.customBuildMetrics("dev.icerock:mobile-multiplatform" to version)
                }
            }
        }
    }

    private fun Project.talaiot(block: InfluxdbExtension.() -> Unit) {
        extensions.configure(InfluxdbExtension::class.java) { ext ->
            ext.block()
        }
    }
}
