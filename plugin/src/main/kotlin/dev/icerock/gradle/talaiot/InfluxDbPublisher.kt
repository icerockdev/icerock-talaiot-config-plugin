/*
 * Copyright 2021 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 *
 * Copyright (c) 2019 Iñaki Villar. MIT License
 * original https://github.com/cdsap/Talaiot/blob/17ff22858d1add5b83b64fe3a024141a9b7bff43/library/plugins/influxdb/influxdb-publisher/src/main/kotlin/io/github/cdsap/talaiot/publisher/influxdb/InfluxDbPublisher.kt
 */

package dev.icerock.gradle.talaiot

import com.influxdb.LogLevel
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.influxdb.client.kotlin.WriteKotlinApi
import com.influxdb.client.write.Point
import io.github.cdsap.talaiot.entities.ExecutionReport
import io.github.cdsap.talaiot.metrics.ValuesProvider
import io.github.cdsap.talaiot.publisher.Publisher
import kotlinx.coroutines.runBlocking
import org.gradle.api.logging.Logger

class InfluxDbPublisher(
    private val configuration: Configuration,
    private val logger: Logger
) : Publisher {

    data class Configuration(
        val url: String,
        val token: String,
        val bucket: String,
        val org: String,
        val publishTaskMetrics: Boolean = true,
        val publishBuildMetrics: Boolean = true,
        val taskMetricName: String = "task",
        val buildMetricName: String = "build",
        val buildExtractor: Extractor = DefaultExtractor(),
        val taskExtractor: Extractor = DefaultExtractor()
    ) {
        interface Extractor {
            fun extractTags(input: Map<String, Any>): Map<String, String>
            fun extractFields(input: Map<String, Any>): Map<String, Any>
        }

        class DefaultExtractor : Extractor {
            override fun extractTags(input: Map<String, Any>): Map<String, String> {
                return input
                    .filterValues { it is String || it is Boolean }
                    .mapValues { it.value.toString() }
            }

            override fun extractFields(input: Map<String, Any>): Map<String, Any> {
                return input.filterValues { it !is String }
            }
        }
    }

    override fun publish(report: ExecutionReport) {
        runBlocking {
            try {
                val writeApi: WriteKotlinApi = createApi()

                logger.info("================")
                logger.info("InfluxDbPublisher")
                logger.info("publishBuildMetrics: ${configuration.publishBuildMetrics}")
                logger.info("publishTaskMetrics: ${configuration.publishTaskMetrics}")
                logger.info("================")

                if (configuration.publishTaskMetrics) {
                    val measurements = createTaskPoints(report)
                    if (!measurements.isNullOrEmpty()) {
                        logger.info("Sending task points to InfluxDb server $measurements")
                        writeApi.writePoints(measurements)
                    }
                }

                if (configuration.publishBuildMetrics) {
                    val buildMeasurement = createBuildPoint(report)
                    logger.info("Sending build point to InfluxDb server $buildMeasurement")
                    writeApi.writePoint(buildMeasurement)
                }

                logger.lifecycle("")
                logger.lifecycle("Build analytics was sent on IceRock's InfluxDB.")
                logger.lifecycle("Detailed information about the collected metrics can be viewed using the build option -info")
                logger.lifecycle("To disable analytics just remove plugin \"dev.icerock.gradle.talaiot\"")
            } catch (e: Exception) {
                logger.error("InfluxDbPublisher-Error", e)
            }
        }
    }

    private fun createTaskPoints(report: ExecutionReport): List<Point>? {
        return report.tasks?.map { task ->
            val dataProvider: ValuesProvider = DefaultTaskDataProvider(task, report)

            Point.measurement(configuration.taskMetricName)
                .time(System.currentTimeMillis(), WritePrecision.MS)
                .addData(dataProvider, configuration.taskExtractor)
        }
    }

    private fun createBuildPoint(report: ExecutionReport): Point {
        val dataProvider: ValuesProvider = DefaultBuildMetricsProvider(report)

        return Point.measurement(configuration.buildMetricName)
            .time(report.endMs?.toLong() ?: System.currentTimeMillis(), WritePrecision.MS)
            .addData(dataProvider, configuration.buildExtractor)
    }

    private fun Point.addData(
        valuesProvider: ValuesProvider,
        extractor: Configuration.Extractor
    ): Point {
        val data: Map<String, Any> = valuesProvider.get()
        val tags: Map<String, String> = extractor.extractTags(data)
        val fields: Map<String, Any> = extractor.extractFields(data)

        addTags(tags)
        addFields(fields)

        return this
    }

    private fun createApi(): WriteKotlinApi {
        val client = InfluxDBClientKotlinFactory.create(
            url = configuration.url,
            token = configuration.token.toCharArray(),
            org = configuration.org,
            bucket = configuration.bucket
        )
        client.setLogLevel(LogLevel.BASIC)
        client.enableGzip()

        return client.getWriteKotlinApi()
    }
}
