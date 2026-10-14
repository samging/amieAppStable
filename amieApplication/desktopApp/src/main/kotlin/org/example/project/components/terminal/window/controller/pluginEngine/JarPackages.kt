package org.example.project.components.terminal.window.controller.pluginEngine

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.project.backend.BackendConfig
import org.example.project.components.terminal.integrations.amiePilot.InteractionModel
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.envBuilder.ScriptEnvironments
import org.example.project.components.terminal.window.controller.logInternal
import org.example.project.components.terminal.window.controller.modules.SessionLog
import org.example.project.getDirectory
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.URLClassLoader
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.jar.JarFile

/** One jar the Amie server offers: what the website's `/api/jars` (or the S3 service's `/api/aws/jars`) lists */
@Serializable
data class ServerJar(
    val name: String,
    val size: Long = 0,
    val version: String? = null,
    val kind: String = "package",
    val lastModified: String? = null,
)

/**
 * Where a package jar came from, kept next to it as `AMP/pluginDir/<name>.source.json` when it was fetched from
 * the Amie server, so its owner can go back to the page that lists it and find out how to use it.
 */
@Serializable
data class JarOrigin(
    val name: String,
    /** The exact URL the bytes were downloaded from */
    val url: String,
    /** The website page that lists the jar, where its description lives */
    val page: String,
    val version: String? = null,
    val size: Long = 0,
    val lastModified: String? = null,
    val fetchedAt: String = Instant.now().toString(),
)

/**
 * One open package: a jar loaded into its own class loader with one instance of its package class, the
 * class that has `run(String): List` and `close()` (the contract `POST /upload` checks on the server).
 *
 * Every `initMod("x.jar")` makes a new one, like `initMod("model")` opens a new ONNX session: packages are
 * self-contained bottles, each session owns its instance and its loader, and `close()` releases both.
 */
class JarPackageSession internal constructor(
    val id: Int,
    val jarName: String,
    val file: File,
    val className: String,
    /** Where the jar came from: env, pluginDir, path or server */
    val source: String,
    private val loader: URLClassLoader,
    private val instance: Any,
    private val runMethod: Method,
    private val closeMethod: Method?,
) : InteractionModel {
    val openedAt: Instant = Instant.now()
    private val runCount = AtomicInteger()
    private val runningCount = AtomicInteger()

    /** What this session was asked and answered; also appended to AMP/pluginDir/<jar>.sessions.log */
    val log = SessionLog(jarName, id, JarPackages.sessionsLogFile(jarName))

    /** Finished calls of run(text) */
    val runs: Int get() = runCount.get()

    /** Calls of run(text) in flight right now */
    val running: Int get() = runningCount.get()

    @Volatile
    var closed: Boolean = false
        private set

    /** Hands the text to the package's run(text) and returns what it answered, one line per element */
    override fun inference(text: String): List<String> {
        check(!closed) { "package $jarName (#$id) is closed" }
        val previous = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = loader
        runningCount.incrementAndGet()
        val begun = System.nanoTime()
        try {
            val result = lines(runMethod.invoke(instance, text))
            log.run(text, result, (System.nanoTime() - begun) / 1_000_000)
            return result
        } catch (e: InvocationTargetException) {
            val cause = e.cause ?: e
            log.error(text, cause)
            logInternal("package $jarName: run failed: $cause", LogLevel.ERROR)
            throw cause
        } finally {
            runningCount.decrementAndGet()
            runCount.incrementAndGet()
            Thread.currentThread().contextClassLoader = previous
        }
    }

    override fun getStatus(): Any = mapOf(
        "id" to id,
        "jar" to jarName,
        "class" to className,
        "source" to source,
        "file" to file.absolutePath,
        "url" to JarPackages.origin(jarName)?.url,
        "page" to JarPackages.origin(jarName)?.page,
        "openedAt" to openedAt.toString(),
        "runs" to runs,
        "running" to running,
        "closed" to closed,
    )

    /** Calls the package's close() and drops its class loader; a second call does nothing */
    override fun close() {
        if (closed) return
        closed = true
        try {
            closeMethod?.invoke(instance)
        } catch (e: InvocationTargetException) {
            logInternal("package $jarName: close failed: ${e.cause}", LogLevel.ERROR)
        } finally {
            runCatching { loader.close() }
            JarPackages.unregister(this)
            log.add("close", "closed after $runs runs")
            logInternal("package $jarName (#$id): closed after $runs runs")
        }
    }

    override fun toString(): String = "#$id $jarName ($className) · $source · runs $runs"

    private fun lines(result: Any?): List<String> = when (result) {
        null -> emptyList()
        is String -> listOf(result)
        is Collection<*> -> result.map { it.toString() }
        is Array<*> -> result.map { it.toString() }
        is Iterable<*> -> result.map { it.toString() }
        else -> listOf(result.toString())
    }
}

/**
 * Opens package jars as sessions: `initMod("x.jar")` resolves the jar (the active env, then `AMP/pluginDir`,
 * then a path, then the Amie server, which stores it in `AMP/pluginDir`), finds the class that satisfies
 * the package contract, instantiates it and returns it behind the `inference / getStatus / close` interface.
 *
 * Scripts also reach this as `amie.packages`: `packages.available()` lists what the server offers,
 * `packages.fetch("x.jar")` downloads one, `packages.print()` shows the open sessions, `packages.closeAll()`.
 */
object JarPackages {
    private const val LISTING_TTL_MS = 60_000L
    private val ids = AtomicInteger()
    private val sessions = linkedMapOf<Int, JarPackageSession>()
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient(Java) {
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 120_000
        }
        expectSuccess = false
    }

    @Volatile private var listing: Pair<Long, List<ServerJar>>? = null

    /** The open package sessions, oldest first */
    val open: List<JarPackageSession> @Synchronized get() = sessions.values.toList()

    /** Opens a new session on the jar; see the class comment for where the jar is looked up */
    fun open(name: String): JarPackageSession {
        val (file, source) = resolve(name) ?: throw IllegalArgumentException(
            "package $name not found: not in the env, AMP/pluginDir, as a path, or on the server (${BackendConfig.load().site})"
        )
        val env = ScriptEnvironments.current
        val parent = env?.classLoader(JarPackages::class.java.classLoader) ?: JarPackages::class.java.classLoader
        val id = ids.incrementAndGet()
        val loader = URLClassLoader("amie-package-${file.name}-$id", arrayOf(file.toURI().toURL()), parent)
        try {
            val entry = findEntryClass(file, loader)
                ?: throw IllegalArgumentException("${file.name} has no package class: a public class with run(String): List and close(), and a no-arg constructor")
            val previous = Thread.currentThread().contextClassLoader
            Thread.currentThread().contextClassLoader = loader
            val instance = try {
                entry.clazz.getConstructor().newInstance()
            } catch (e: InvocationTargetException) {
                throw IllegalStateException("${file.name}: ${entry.clazz.name} could not be created: ${e.cause}", e.cause)
            } finally {
                Thread.currentThread().contextClassLoader = previous
            }
            val session = JarPackageSession(id, file.name, file, entry.clazz.name, source, loader, instance, entry.run, entry.close)
            session.log.add("open", "opened as session #$id on ${entry.clazz.name} from $source")
            synchronized(this) { sessions[id] = session }
            logInternal("package ${file.name} (#$id): session ready on ${entry.clazz.name} from $source")
            return session
        } catch (e: Throwable) {
            runCatching { loader.close() }
            throw e
        }
    }

    /** The jar as a file with where it was found, downloading it from the server when it is nowhere local */
    fun resolve(name: String): Pair<File, String>? {
        val env = ScriptEnvironments.current
        env?.localJar(name)?.let { return it to "env" }
        val pluginDir = pluginDir()
        File(pluginDir, File(name).name).takeIf { it.isFile }?.let { return it to "pluginDir" }
        (env?.jarSearchPath(name) ?: emptyList()).firstOrNull { it.isFile }?.let { return it to "path" }
        File(name).takeIf { it.isFile }?.let { return it to "path" }
        return fetch(name)?.let { it to "server" }
    }

    /**
     * Downloads a jar from the Amie server into `AMP/pluginDir`: the website's `/api/jar-download` first,
     * then the S3 service's `/api/aws/jars/<name>`. Null when neither has it.
     */
    fun fetch(name: String): File? {
        val fileName = File(name).name
        if (!fileName.endsWith(".jar", ignoreCase = true)) return null
        val config = BackendConfig.load()
        val urls = listOf(
            "${config.site}/api/jar-download?name=${java.net.URLEncoder.encode(fileName, Charsets.UTF_8)}",
            "${config.s3}/api/aws/jars/${java.net.URLEncoder.encode(fileName, Charsets.UTF_8)}",
        )
        for (url in urls) {
            val bytes = runCatching {
                runBlocking {
                    val response = http.get(url)
                    if (!response.status.isSuccess()) null else response.bodyAsBytes()
                }
            }.onFailure { logInternal("package $fileName: $url unreachable: ${it.message}", LogLevel.ERROR) }.getOrNull() ?: continue
            if (bytes.size < 4 || bytes[0] != 'P'.code.toByte() || bytes[1] != 'K'.code.toByte()) {
                logInternal("package $fileName: $url did not answer with a jar", LogLevel.ERROR)
                continue
            }
            val target = File(pluginDir(), fileName)
            target.writeBytes(bytes)
            val listed = available().firstOrNull { it.name == fileName }
            val origin = JarOrigin(fileName, url, downloadsPage(), listed?.version, bytes.size.toLong(), listed?.lastModified)
            runCatching { originFile(fileName).writeText(json.encodeToString(JarOrigin.serializer(), origin)) }
                .onFailure { logInternal("package $fileName: couldn't record its origin: ${it.message}", LogLevel.ERROR) }
            logInternal("package $fileName: received ${bytes.size} bytes from $url into ${target.absolutePath}")
            return target
        }
        return null
    }

    /** The package jars the server lists, cached for a minute (an unreachable server is remembered as empty for as long) */
    fun available(refresh: Boolean = false): List<ServerJar> {
        val now = System.currentTimeMillis()
        listing?.let { (at, jars) -> if (!refresh && now - at < LISTING_TTL_MS) return jars }
        val config = BackendConfig.load()
        val urls = listOf("${config.site}/api/jars", "${config.s3}/api/aws/jars")
        var jars = emptyList<ServerJar>()
        for (url in urls) {
            val text = runCatching {
                runBlocking {
                    val response = http.get(url)
                    if (!response.status.isSuccess()) null else response.bodyAsText()
                }
            }.getOrNull() ?: continue
            jars = runCatching { json.decodeFromString<List<ServerJar>>(text) }.getOrDefault(emptyList())
                .filter { it.kind != "application" && it.name.endsWith(".jar", ignoreCase = true) }
            break
        }
        listing = now to jars
        return jars
    }

    /** Where the jar was fetched from, when it was fetched from the server; null for a jar that was added locally */
    fun origin(name: String): JarOrigin? =
        runCatching { json.decodeFromString(JarOrigin.serializer(), originFile(File(name).name).readText()) }.getOrNull()

    /** The URL a jar of that name is fetched from first (the website), for a jar that is still only on the server */
    fun downloadUrl(name: String): String =
        "${BackendConfig.load().site}/api/jar-download?name=${java.net.URLEncoder.encode(File(name).name, Charsets.UTF_8)}"

    /** The website page that lists every package jar with its description */
    fun downloadsPage(): String = "${BackendConfig.load().site}/downloads"

    private fun originFile(fileName: String): File = File(pluginDir(), "$fileName.source.json")

    /** Where the sessions of a jar log what they were asked and answered */
    fun sessionsLogFile(fileName: String): File = File(pluginDir(), "${File(fileName).name}.sessions.log")

    /**
     * The name of the class a session would be opened on, found with a throwaway loader; null when no class in
     * the jar satisfies the contract. Used by the Models page to describe a jar without opening it.
     */
    fun entryClassName(file: File): String? {
        val env = ScriptEnvironments.current
        val parent = env?.classLoader(JarPackages::class.java.classLoader) ?: JarPackages::class.java.classLoader
        return URLClassLoader("amie-inspect-${file.name}", arrayOf(file.toURI().toURL()), parent).use { loader ->
            runCatching { findEntryClass(file, loader)?.clazz?.name }.getOrNull()
        }
    }

    /** Closes the session with that id */
    fun close(id: Int): Boolean {
        val session = synchronized(this) { sessions[id] } ?: return false
        session.close()
        return true
    }

    @Synchronized
    fun closeAll() = sessions.values.toList().forEach { it.close() }

    /** One line per open session, also printed to the console */
    fun print(): List<String> {
        val lines = open.map { it.toString() }.ifEmpty { listOf("no open packages") }
        lines.forEach { logInternal(it) }
        return lines
    }

    internal fun unregister(session: JarPackageSession) {
        synchronized(this) { sessions.remove(session.id) }
    }

    private fun pluginDir(): File = File(getDirectory().toString(), "pluginDir").apply { mkdirs() }

    private class Entry(val clazz: Class<*>, val run: Method, val close: Method?)

    /**
     * The class to instantiate: the manifest's Main-Class when it satisfies the contract, else a class that
     * implements an interface named AmiePackage, else the first class in the jar that satisfies it.
     */
    private fun findEntryClass(file: File, loader: ClassLoader): Entry? {
        val candidates = mutableListOf<Entry>()
        var mainClass: String? = null
        JarFile(file).use { jar ->
            mainClass = jar.manifest?.mainAttributes?.getValue("Main-Class")
            val names = jar.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".class") }
                .map { it.name.removeSuffix(".class").replace('/', '.') }
                .filter { !it.endsWith("module-info") && !it.endsWith("package-info") }
                .toList()
            for (name in names) {
                val clazz = runCatching { Class.forName(name, false, loader) }.getOrNull() ?: continue
                contract(clazz)?.let { candidates += it }
            }
        }
        if (candidates.isEmpty()) return null
        mainClass?.let { main -> candidates.firstOrNull { it.clazz.name == main }?.let { return it } }
        candidates.firstOrNull { c -> c.clazz.interfaces.any { it.simpleName == "AmiePackage" } }?.let { return it }
        return candidates.first()
    }

    /** The run/close methods when the class is a concrete public class with them and a no-arg constructor */
    private fun contract(clazz: Class<*>): Entry? {
        val mods = clazz.modifiers
        if (clazz.isInterface || clazz.isAnnotation || clazz.isEnum || Modifier.isAbstract(mods) || !Modifier.isPublic(mods)) return null
        val methods = runCatching { clazz.methods }.getOrNull() ?: return null
        val run = methods.firstOrNull {
            it.name == "run" && it.parameterCount == 1 && it.parameterTypes[0] == String::class.java &&
                !Modifier.isStatic(it.modifiers) && it.returnType != Void.TYPE
        } ?: return null
        val close = methods.firstOrNull { it.name == "close" && it.parameterCount == 0 && !Modifier.isStatic(it.modifiers) }
        runCatching { clazz.getConstructor() }.getOrNull() ?: return null
        return Entry(clazz, run, close)
    }
}
