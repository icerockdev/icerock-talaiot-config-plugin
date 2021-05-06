/*
 * Copyright 2021 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 *
 * Copyright (c) 2019 Iñaki Villar. MIT License
 * original https://github.com/cdsap/Talaiot/blob/17ff22858d1add5b83b64fe3a024141a9b7bff43/library/core/talaiot/src/main/kotlin/io/github/cdsap/talaiot/metrics/DefaultTaskDataProvider.kt
 */

package dev.icerock.gradle.talaiot

import io.github.cdsap.talaiot.entities.ExecutionReport
import io.github.cdsap.talaiot.entities.TaskLength
import io.github.cdsap.talaiot.metrics.TaskMetrics
import io.github.cdsap.talaiot.metrics.ValuesProvider
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

class DefaultTaskDataProvider(
    private val task: TaskLength,
    private val report: ExecutionReport
) : ValuesProvider {

    @OptIn(ExperimentalTime::class)
    override fun get(): Map<String, Any> {
        return mapOf(
            TaskMetrics.State.toKey() to task.state.name,
            TaskMetrics.Module.toKey() to task.module,
            TaskMetrics.RootNode.toKey() to task.rootNode.toString(),
            TaskMetrics.Task.toKey() to task.taskName,
            TaskMetrics.WorkerId.toKey() to task.workerId,
            TaskMetrics.Critical.toKey() to task.critical.toString(),
            TaskMetrics.Value.toKey() to task.ms.toDuration(DurationUnit.MILLISECONDS).inSeconds,
            TaskMetrics.CacheEnabled.toKey() to task.isCacheEnabled,
            TaskMetrics.LocalCacheHit.toKey() to task.isLocalCacheHit,
            TaskMetrics.LocalCacheMiss.toKey() to task.isLocalCacheMiss,
            TaskMetrics.RemoteCacheHit.toKey() to task.isRemoteCacheHit,
            TaskMetrics.RemoteCacheMiss.toKey() to task.isRemoteCacheMiss
        ) + report.customProperties.taskProperties
    }
}
