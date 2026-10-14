package org.example.project.components.terminal.window.controller.modules

import org.example.project.components.terminal.window.controller.modelLoaders.OrtSessions
import java.lang.management.ManagementFactory
import java.time.Duration
import java.util.Locale

/** What the app process is using right now, for the models page */
data class SystemStats(
    val uptime: Duration,
    val heapUsedBytes: Long,
    val heapMaxBytes: Long,
    val nonHeapUsedBytes: Long,
    // 0..1, negative when the platform doesn't report it
    val processCpu: Double,
    val systemCpu: Double,
    val processors: Int,
    val threads: Int,
    val ortSessions: Int,
    val ortRunning: Int,
    val ortRuns: Int,
    val ortRunTimeMs: Long,
    val listening: Int,
    // What the whole process holds (resident set, heap and native together), 0 when the platform can't say
    val residentBytes: Long = 0,
    // The machine's RAM and what is still free of it, 0 when the platform can't say
    val physicalBytes: Long = 0,
    val physicalFreeBytes: Long = 0,
) {
    /** The share of the machine's RAM this process holds, 0..1, or -1 when unknown */
    val residentShare: Double get() = if (physicalBytes <= 0 || residentBytes <= 0) -1.0 else residentBytes.toDouble() / physicalBytes
    val heapShare: Double get() = if (heapMaxBytes <= 0) 0.0 else heapUsedBytes.toDouble() / heapMaxBytes

    companion object {
        private val runtimeBean = ManagementFactory.getRuntimeMXBean()
        private val memoryBean = ManagementFactory.getMemoryMXBean()
        private val threadBean = ManagementFactory.getThreadMXBean()
        private val osBean = ManagementFactory.getOperatingSystemMXBean()

        fun read(): SystemStats {
            val heap = memoryBean.heapMemoryUsage
            val sessions = OrtSessions.all
            val os = osBean as? com.sun.management.OperatingSystemMXBean
            return SystemStats(
                uptime = Duration.ofMillis(runtimeBean.uptime),
                heapUsedBytes = heap.used,
                heapMaxBytes = heap.max.takeIf { it > 0 } ?: heap.committed,
                nonHeapUsedBytes = memoryBean.nonHeapMemoryUsage.used,
                processCpu = os?.processCpuLoad ?: -1.0,
                systemCpu = os?.cpuLoad ?: -1.0,
                processors = osBean.availableProcessors,
                threads = threadBean.threadCount,
                ortSessions = sessions.size,
                ortRunning = sessions.sumOf { it.running },
                ortRuns = sessions.sumOf { it.runs },
                ortRunTimeMs = sessions.sumOf { it.runTimeMs },
                listening = ModelServer.listening.size,
                residentBytes = residentBytes(),
                physicalBytes = os?.totalMemorySize ?: 0,
                physicalFreeBytes = os?.freeMemorySize ?: 0,
            )
        }

        // ps reports the resident set in KB on macOS and Linux; the JVM has no API for it
        private fun residentBytes(): Long = runCatching {
            val process = ProcessBuilder("ps", "-o", "rss=", "-p", ProcessHandle.current().pid().toString()).redirectErrorStream(true).start()
            val out = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            out.toLongOrNull()?.times(1024) ?: 0L
        }.getOrDefault(0L)
    }
}

fun bytes(value: Long): String = when {
    value >= 1L shl 30 -> "%.2f GB".format(Locale.US, value / 1073741824.0)
    value >= 1L shl 20 -> "%.1f MB".format(Locale.US, value / 1048576.0)
    value >= 1024 -> "${value / 1024} KB"
    else -> "$value B"
}

fun percent(load: Double): String = if (load < 0) "n/a" else "${(load * 100).toInt()}%"

/** "41s", "12m 03s", "3h 07m" or "2d 05h" */
fun uptime(duration: Duration): String {
    val s = duration.seconds
    return when {
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m ${"%02d".format(s % 60)}s"
        s < 86400 -> "${s / 3600}h ${"%02d".format((s % 3600) / 60)}m"
        else -> "${s / 86400}d ${"%02d".format((s % 86400) / 3600)}h"
    }
}

fun ms(value: Double): String = if (value >= 1000) "%.2f s".format(Locale.US, value / 1000) else "%.0f ms".format(Locale.US, value)
