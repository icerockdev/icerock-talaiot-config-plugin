/*
 * Copyright 2021 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.gradle.talaiot

import BuildConfig
import io.github.cdsap.talaiot.plugin.TalaiotPlugin
import io.github.cdsap.talaiot.plugin.TalaiotPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class TalaiotConfigPlugin : Plugin<Project> {
    private val talaiotPlugin = TalaiotPlugin()

    override fun apply(target: Project) {
        talaiotPlugin.apply(target)

        target.talaiot {
            publishers {
                customPublishers(
                    InfluxDbPublisher(
                        configuration = InfluxDbPublisher.Configuration(
                            url = BuildConfig.influxUrl,
                            org = BuildConfig.influxOrg,
                            bucket = BuildConfig.influxBucket,
                            token = BuildConfig.influxToken
                        ),
                        logger = project.logger
                    )
                )
            }
            metrics.customTaskMetrics("projectName" to target.name)
        }

        configureKotlinVersionMetric(target)
        configureDependenciesMetric(target)
        configureMobilePluginMetric(target)
    }

    private fun configureKotlinVersionMetric(target: Project) {
        target.allprojects { project ->
            project.configurations.configureEach { configuration ->
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
        }
    }

    private fun configureDependenciesMetric(target: Project) {
        target.allprojects { project ->
            project.configurations.configureEach { configuration ->
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

    private fun Project.talaiot(block: TalaiotPluginExtension.() -> Unit) {
        extensions.configure(TalaiotPluginExtension::class.java) { ext ->
            ext.block()
        }
    }
}
