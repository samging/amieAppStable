package org.example.project.components.terminal.window.controller.modules

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.example.project.components.terminal.integrations.amiePilot.InitMod
import org.example.project.components.terminal.integrations.amiePilot.InteractionModel
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.logInternal
import java.io.File
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** How a module is served; kept in AMP/modules/<name>/serving.json */
@Serializable
data class ServingSettings(
    val enabled: Boolean = false,
    // Loopback by default so only this machine can reach the model
    val host: String = "127.0.0.1",
    val port: Int = 8765,
    val path: String = "",
    val methods: List<String> = listOf("POST", "GET"),
) {
    fun pathFor(model: String) = (path.ifBlank { "/models/$model" }).let { if (it.startsWith("/")) it else "/$it" }.trimEnd('/') + "/predict"
    fun url(model: String) = "http://$host:$port${pathFor(model)}"
}

private val servingJson = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

fun readServing(name: String): ServingSettings =
    runCatching { servingJson.decodeFromString<ServingSettings>(File(moduleDir(name), "serving.json").readText()) }.getOrElse { ServingSettings() }

fun writeServing(name: String, settings: ServingSettings) {
    val dir = moduleDir(name).apply { mkdirs() }
    File(dir, "serving.json").writeText(servingJson.encodeToString(settings))
}

/**
 * Serves modules over HTTP: POST {"text": "..."} (or a plain-text body) or GET ?text=... to <path>/predict returns
 * {"model", "predictions": [{"label", "score"}]}. Requests run on background threads and inference goes through OrtSessions.
 * Every request is logged as internal and appended to AMP/modules/<name>/incoming.jsonl.
 */
object ModelServer {
    private class Listener(val name: String, val settings: ServingSettings, val model: InteractionModel) {
        val startedAt: Instant = Instant.now()
        val requests = AtomicInteger()
        val errors = AtomicInteger()
        val lastRequestAt = AtomicLong(0)
        // Inference latency in milliseconds: total, worst and last, for the statistics page
        val latencyTotalMs = AtomicLong(0)
        val latencyMaxMs = AtomicLong(0)
        val latencyLastMs = AtomicLong(0)
    }

    /** What a listening model has done since it started: shown on the models page */
    data class ServingStats(
        val name: String,
        val startedAt: Instant,
        val url: String,
        val methods: List<String>,
        val requests: Int,
        val errors: Int,
        val lastRequestAt: Instant?,
        val averageLatencyMs: Double,
        val maxLatencyMs: Long,
        val lastLatencyMs: Long,
    ) {
        val uptime: java.time.Duration get() = java.time.Duration.between(startedAt, Instant.now())
    }

    fun stats(name: String): ServingStats? = listeners[name]?.let { l ->
        val n = l.requests.get()
        ServingStats(
            name = l.name,
            startedAt = l.startedAt,
            url = l.settings.url(l.name),
            methods = l.settings.methods,
            requests = n,
            errors = l.errors.get(),
            lastRequestAt = l.lastRequestAt.get().takeIf { it > 0 }?.let(Instant::ofEpochMilli),
            averageLatencyMs = if (n == 0) 0.0 else l.latencyTotalMs.get().toDouble() / n,
            maxLatencyMs = l.latencyMaxMs.get(),
            lastLatencyMs = l.latencyLastMs.get(),
        )
    }

    val listening: List<String> get() = listeners.keys.sorted()

    private val threads = AtomicInteger()
    private val executor = Executors.newFixedThreadPool(4) { Thread(it, "amie-serve-${threads.incrementAndGet()}").apply { isDaemon = true } }
    private val servers = ConcurrentHashMap<String, HttpServer>()
    private val listeners = ConcurrentHashMap<String, Listener>()

    fun isListening(name: String) = listeners.containsKey(name)
    fun requests(name: String) = listeners[name]?.requests?.get() ?: 0
    fun lastRequestAt(name: String): Instant? = listeners[name]?.lastRequestAt?.get()?.takeIf { it > 0 }?.let(Instant::ofEpochMilli)

    /** Opens the module's session and starts listening; the setting is saved so it resumes on the next start */
    @Synchronized
    fun start(name: String, settings: ServingSettings = readServing(name)): Boolean {
        stop(name, persist = false)
        val model = try {
            InitMod(name).create()
        } catch (e: Exception) {
            logInternal("serve '$name': can't load the model: ${e.message}", LogLevel.ERROR)
            return false
        }
        val key = "${settings.host}:${settings.port}"
        val server = try {
            servers.getOrPut(key) {
                HttpServer.create(InetSocketAddress(settings.host, settings.port), 0).apply {
                    executor = this@ModelServer.executor
                    start()
                }
            }
        } catch (e: Exception) {
            model.close()
            logInternal("serve '$name': can't listen on $key: ${e.message}", LogLevel.ERROR)
            return false
        }
        val listener = Listener(name, settings, model)
        try {
            server.createContext(settings.pathFor(name)) { exchange -> handle(listener, exchange) }
        } catch (e: IllegalArgumentException) {
            model.close()
            logInternal("serve '$name': ${settings.pathFor(name)} is already served on $key", LogLevel.ERROR)
            return false
        }
        listeners[name] = listener
        writeServing(name, settings.copy(enabled = true))
        logInternal("serve '$name': listening on ${settings.methods.joinToString("/")} ${settings.url(name)}")
        return true
    }

    @Synchronized
    fun stop(name: String, persist: Boolean = true) {
        val listener = listeners.remove(name) ?: run {
            if (persist) writeServing(name, readServing(name).copy(enabled = false))
            return
        }
        val key = "${listener.settings.host}:${listener.settings.port}"
        servers[key]?.let { server ->
            runCatching { server.removeContext(listener.settings.pathFor(name)) }
            // The last model on a port takes the server down with it
            if (listeners.values.none { "${it.settings.host}:${it.settings.port}" == key }) {
                server.stop(0)
                servers.remove(key)
            }
        }
        listener.model.close()
        if (persist) writeServing(name, listener.settings.copy(enabled = false))
        logInternal("serve '$name': stopped")
    }

    /** Resumes every module that was listening when the app closed */
    fun restore() {
        val names = (readRegistry().modules.keys + (modulesDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList())).distinct()
        names.filter { readServing(it).enabled }.forEach { start(it) }
    }

    fun stopAll() = listeners.keys.toList().forEach { stop(it, persist = false) }

    private fun handle(listener: Listener, exchange: HttpExchange) {
        exchange.use {
            try {
                val method = exchange.requestMethod.uppercase()
                if (method !in listener.settings.methods) {
                    respond(exchange, 405, error("${method} not allowed, use ${listener.settings.methods}"))
                    return
                }
                val text = when (method) {
                    "GET" -> query(exchange)["text"]
                    else -> readBody(exchange)
                }?.trim()
                if (text.isNullOrEmpty()) {
                    respond(exchange, 400, error("send text as {\"text\": \"...\"}, a plain-text body or ?text="))
                    return
                }
                listener.requests.incrementAndGet()
                listener.lastRequestAt.set(System.currentTimeMillis())

                val begun = System.nanoTime()
                val predictions = listener.model.inference(text).map { entry ->
                    val label = entry.substringBeforeLast(':')
                    label to (entry.substringAfterLast(':').toDoubleOrNull() ?: 0.0)
                }
                val tookMs = (System.nanoTime() - begun) / 1_000_000
                listener.latencyTotalMs.addAndGet(tookMs)
                listener.latencyLastMs.set(tookMs)
                listener.latencyMaxMs.accumulateAndGet(tookMs, ::maxOf)
                val top = predictions.firstOrNull()
                logInternal("serve '${listener.name}': $method from ${exchange.remoteAddress.address.hostAddress} \"${text.take(60)}\" -> ${top?.first ?: "-"}")
                record(listener.name, method, text, top)

                respond(exchange, 200, buildJsonObject {
                    put("model", listener.name)
                    put("predictions", buildJsonArray {
                        predictions.forEach { (label, score) -> add(buildJsonObject { put("label", label); put("score", score) }) }
                    })
                })
            } catch (e: Exception) {
                listener.errors.incrementAndGet()
                logInternal("serve '${listener.name}': ${e.message}", LogLevel.ERROR)
                respond(exchange, 500, error(e.message ?: e.toString()))
            }
        }
    }

    // Incoming data is kept per model so it can be reviewed or labeled later
    private fun record(name: String, method: String, text: String, top: Pair<String, Double>?) {
        val line = buildJsonObject {
            put("time", Instant.now().toString())
            put("method", method)
            put("text", text)
            top?.let { put("label", it.first); put("score", it.second) }
        }
        runCatching { File(moduleDir(name), "incoming.jsonl").appendText(Json.encodeToString(JsonObject.serializer(), line) + "\n") }
    }

    private fun readBody(exchange: HttpExchange): String? {
        val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
        if (body.isBlank()) return null
        // JSON {"text": ...} when it parses as such, otherwise the body itself is the text
        return runCatching { Json.parseToJsonElement(body).jsonObject["text"]?.jsonPrimitive?.contentOrNull }.getOrNull() ?: body
    }

    private fun query(exchange: HttpExchange): Map<String, String> =
        exchange.requestURI.rawQuery?.split("&")?.mapNotNull { pair ->
            val (k, v) = pair.split("=", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            URLDecoder.decode(k, Charsets.UTF_8) to URLDecoder.decode(v, Charsets.UTF_8)
        }?.toMap() ?: emptyMap()

    private fun error(message: String) = buildJsonObject { put("error", message) }

    private fun respond(exchange: HttpExchange, status: Int, body: JsonObject) {
        val bytes = Json.encodeToString(JsonObject.serializer(), body).toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
