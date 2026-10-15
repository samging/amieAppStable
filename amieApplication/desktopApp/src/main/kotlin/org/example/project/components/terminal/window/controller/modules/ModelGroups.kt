package org.example.project.components.terminal.window.controller.modules

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project.components.terminal.integrations.amiePilot.Ask
import org.example.project.components.terminal.integrations.amiePilot.InteractionModel
import org.example.project.components.terminal.integrations.amiePilot.Prediction
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.envBuilder.DEFAULT_ENV
import org.example.project.components.terminal.window.controller.envBuilder.ScriptEnvironments
import org.example.project.components.terminal.window.controller.envBuilder.envRoot
import org.example.project.components.terminal.window.controller.logInternal
import java.io.File
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * A named set of intent models that are asked together: `group("name")` in a script opens them as one
 * [ModelSwarm]. A group belongs to an env, as the packages do: it is kept in `AMP/env/<env>/groups.json`, so the
 * env a script is sourced in brings its groups, and `internalEnv()` lists them with the packages. The icon is a
 * name from the Models page's catalog, for the group's row.
 */
@Serializable
data class ModelGroup(
    val name: String,
    val models: List<String> = emptyList(),
    val icon: String? = null,
    val env: String = DEFAULT_ENV,
    val createdAt: String = Instant.now().toString(),
)

@Serializable
private data class GroupsFile(val groups: List<ModelGroup> = emptyList())

/** Group names: letters, digits, dots, dashes, underscores and spaces */
val GROUP_NAME: Regex = Regex("""[A-Za-z0-9._ -]{1,64}""")

/**
 * The model groups of every env and the swarms open on them. `groups.print()`, `groups.names()`,
 * `groups.create("name", listOf("a", "b"))`, `groups.add("name", "model")`, `groups.remove("name", "model")`,
 * `groups.delete("name")` and `groups.closeAll()` from a script; the Models page does the same with buttons.
 */
object ModelGroups {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val ids = AtomicInteger()
    private val swarms = linkedMapOf<Int, ModelSwarm>()

    /** The env a script is sourced in right now, else the default one: where a new group goes and where `group()` looks first */
    val currentEnv: String get() = ScriptEnvironments.current?.name ?: DEFAULT_ENV

    /** The folder of an env under AMP/env, named as ScriptEnvironment names it */
    fun dirOf(env: String): File = File(envRoot, env.replace(Regex("""[^A-Za-z0-9._-]"""), "_"))

    private fun fileOf(env: String) = File(dirOf(env), "groups.json")

    /** Every env that has a folder, the default one first, plus the one a script is sourcing right now */
    fun envs(): List<String> {
        val dirs = envRoot.listFiles { f -> f.isDirectory && f.name != "caches" }?.map { it.name } ?: emptyList()
        return (listOf(DEFAULT_ENV, currentEnv) + dirs).distinct()
    }

    @Synchronized
    fun of(env: String): List<ModelGroup> =
        runCatching { json.decodeFromString<GroupsFile>(fileOf(env).readText()).groups }.getOrDefault(emptyList())
            .map { it.copy(env = env) }.sortedBy { it.name }

    /** Every group of every env, the current env's first */
    fun all(): List<ModelGroup> = envs().flatMap { of(it) }

    fun names(): List<String> = all().map { if (it.env == DEFAULT_ENV) it.name else "${it.name} (${it.env})" }

    /** The group of that name: in [env] when given, else the current env first, then any env */
    fun find(name: String, env: String? = null): ModelGroup? {
        val wanted = name.trim()
        if (env != null) return of(env).firstOrNull { it.name == wanted }
        return of(currentEnv).firstOrNull { it.name == wanted } ?: all().firstOrNull { it.name == wanted }
    }

    /** The groups a model is in, as "name" or "name (env)" */
    fun ofModel(model: String): List<String> = all().filter { model in it.models }.map { if (it.env == DEFAULT_ENV) it.name else "${it.name} (${it.env})" }

    private fun write(env: String, groups: List<ModelGroup>) {
        val file = fileOf(env).apply { parentFile.mkdirs() }
        file.writeText(json.encodeToString(GroupsFile(groups.sortedBy { it.name })))
    }

    /** Makes a group with these models in the env, or adds them to the one of that name; models that are not exported are refused */
    @Synchronized
    fun create(name: String, models: List<String>, env: String = currentEnv, icon: String? = null): ModelGroup {
        val clean = name.trim()
        require(GROUP_NAME.matches(clean)) { "'$clean' is not a group name: letters, digits, dots, dashes, underscores and spaces, up to 64" }
        val missing = models.filter { !moduleExists(it) }
        require(missing.isEmpty()) { "not exported to AMP/modules: ${missing.joinToString()}" }
        val current = of(env)
        val existing = current.firstOrNull { it.name == clean }
        val group = existing?.copy(models = (existing.models + models).distinct(), icon = icon ?: existing.icon)
            ?: ModelGroup(clean, models.distinct(), icon, env)
        write(env, current.filter { it.name != clean } + group)
        logInternal("groups: '$clean' in env '$env' holds ${group.models.size} model${if (group.models.size == 1) "" else "s"}: ${group.models.joinToString()}")
        return group
    }

    fun add(group: String, model: String, env: String = currentEnv): ModelGroup = create(group, listOf(model), env)

    @Synchronized
    fun remove(group: String, model: String, env: String = currentEnv): Boolean {
        val current = of(env)
        val existing = current.firstOrNull { it.name == group } ?: return false
        write(env, current.filter { it.name != group } + existing.copy(models = existing.models - model))
        logInternal("groups: '$model' taken out of '$group' (env '$env')")
        return true
    }

    @Synchronized
    fun setIcon(group: String, icon: String?, env: String = currentEnv): Boolean {
        val current = of(env)
        val existing = current.firstOrNull { it.name == group } ?: return false
        write(env, current.filter { it.name != group } + existing.copy(icon = icon))
        return true
    }

    /** Removes the group (its swarm is closed first); the models stay */
    @Synchronized
    fun delete(group: String, env: String = currentEnv): Boolean {
        val current = of(env)
        if (current.none { it.name == group }) return false
        swarms.values.filter { it.group.name == group && it.group.env == env }.forEach { it.close() }
        write(env, current.filter { it.name != group })
        logInternal("groups: '$group' removed from env '$env'; its models stay")
        return true
    }

    /** Takes a model out of every group, when the model is destroyed or renamed */
    @Synchronized
    fun forget(model: String, renamedTo: String? = null) {
        envs().forEach { env ->
            val current = of(env)
            if (current.none { model in it.models }) return@forEach
            write(env, current.map { g -> if (model in g.models) g.copy(models = g.models.map { if (it == model) renamedTo else it }.filterNotNull().distinct()) else g })
        }
    }

    /* ---- swarms: the groups open right now ---- */

    val openSwarms: List<ModelSwarm> @Synchronized get() = swarms.values.toList()

    /**
     * The swarm of the group: the one already open on it, else one opened now with a session per model (each
     * the model's one session, reused when it is open already). A model that cannot be opened is skipped with an
     * error line; a group with no openable model is an error.
     */
    @Synchronized
    fun open(name: String, env: String? = null): ModelSwarm {
        val group = find(name, env) ?: throw IllegalArgumentException(
            "no model group '$name'" + (if (env != null) " in env '$env'" else " in env '$currentEnv' or any other") +
                "; groups: ${names().ifEmpty { listOf("none") }.joinToString()}. groups.create(\"$name\", listOf(\"model\")) makes one"
        )
        swarms.values.firstOrNull { it.group.name == group.name && it.group.env == group.env && !it.closed }?.let {
            it.log.add("open", "reused: a group has one swarm")
            logInternal("group: '${group.name}' is already open as swarm #${it.id}, that one is used")
            return it
        }
        val members = group.models.mapNotNull { model ->
            runCatching { ModelSessions.acquire(model) }
                .onFailure { logInternal("group '${group.name}': '$model' could not be opened: ${it.message}", LogLevel.ERROR) }
                .getOrNull()
        }
        if (members.isEmpty()) throw IllegalStateException("group '${group.name}' has no model that opens (${group.models.ifEmpty { listOf("it is empty") }.joinToString()})")
        val swarm = ModelSwarm(ids.incrementAndGet(), group, members)
        swarms[swarm.id] = swarm
        swarm.log.add("open", "opened as swarm #${swarm.id} over ${members.joinToString { "${it.first.name} #${it.first.id}" }}")
        logInternal("group: '${group.name}' open as swarm #${swarm.id} with ${members.size} of ${group.models.size} models (env '${group.env}')")
        return swarm
    }

    fun closeSwarm(id: Int): Boolean {
        val swarm = synchronized(this) { swarms[id] } ?: return false
        swarm.close()
        return true
    }

    @Synchronized
    fun closeAll() = swarms.values.toList().forEach { it.close() }

    internal fun unregister(swarm: ModelSwarm) {
        synchronized(this) { swarms.remove(swarm.id) }
    }

    /** The groups of every env in the console, with their models and whether a swarm is open on them */
    fun print(): List<String> {
        val lines = all().map { g ->
            val open = openSwarms.firstOrNull { it.group.name == g.name && it.group.env == g.env }
            "${g.name} (env ${g.env}): ${g.models.ifEmpty { listOf("no models") }.joinToString()}" + (open?.let { "  [open as swarm #${it.id}, ${it.runs} runs]" } ?: "")
        }
        logInternal(if (lines.isEmpty()) "groups: none; groups.create(\"name\", listOf(\"model\")) makes one in env '$currentEnv'" else "groups (${lines.size}):\n" + lines.joinToString("\n") { "  $it" })
        return lines
    }
}

/**
 * A group open as one model: `ask(text)` is every model's raw answer by name, `each(text)` each model's label,
 * `vote(text)` the label most models give, and the `InteractionModel` calls merge them: `scores` is each label's
 * best score across the models, `predict` the label with the best score, `inference` the merged `label:score`
 * lines. Closing the swarm closes the sessions it opened; one a script opened itself with `initMod` stays.
 */
class ModelSwarm internal constructor(
    val id: Int,
    val group: ModelGroup,
    private val members: List<Pair<ModelSession, Boolean>>,
) : InteractionModel {
    val name: String get() = group.name
    val models: List<ModelSession> get() = members.map { it.first }
    val names: List<String> get() = models.map { it.name }
    val openedAt: Instant = Instant.now()
    private val runningCount = AtomicInteger()
    private val runCount = AtomicInteger()

    /** What the swarm was asked and answered; also appended to AMP/env/<env>/<group>.sessions.log */
    val log = SessionLog("group ${group.name}", id, File(ModelGroups.dirOf(group.env), "${group.name.replace(' ', '_')}.sessions.log"))

    val running: Int get() = runningCount.get()
    val runs: Int get() = runCount.get()

    @Volatile
    var closed: Boolean = false
        private set

    /** Every model's raw answer (`label:score` lines, best first), by model name; a model that fails is left out with an error line */
    fun ask(text: String): Map<String, List<String>> = run(text, Ask.INFERENCE).mapValues { (_, p) -> p.map { it.toString() } }

    /** Each model's own label */
    fun each(text: String): Map<String, String> = run(text, Ask.PREDICT).mapValues { (_, p) -> p.firstOrNull()?.label ?: "" }

    /** The label most of the models answer; a tie goes to the higher best score */
    fun vote(text: String): String {
        val answers = run(text, Ask.PREDICT)
        val tops = answers.values.mapNotNull { it.firstOrNull() }
        if (tops.isEmpty()) throw IllegalStateException("no model of '$name' answered \"$text\"")
        return tops.groupBy { it.label }.entries
            .sortedWith(compareByDescending<Map.Entry<String, List<Prediction>>> { it.value.size }.thenByDescending { e -> e.value.maxOf { it.score ?: 0f } })
            .first().key
    }

    override fun inference(text: String): List<String> = inference(text, Ask.INFERENCE)

    /** The labels merged over the models: each label's best score, best first */
    override fun inference(text: String, ask: Ask): List<String> = merge(run(text, ask)).map { it.toString() }

    private fun merge(answers: Map<String, List<Prediction>>): List<Prediction> =
        answers.values.flatten().groupBy { it.label }
            .map { (label, ps) -> Prediction(label, ps.mapNotNull { it.score }.maxOrNull()) }
            .sortedByDescending { it.score ?: 0f }

    // Every model asked in turn (the encoder takes one text at a time anyway); the merged answer is logged and kept
    private fun run(text: String, ask: Ask): Map<String, List<Prediction>> {
        check(!closed) { "group $name (swarm #$id) is closed" }
        runningCount.incrementAndGet()
        val begun = System.nanoTime()
        try {
            val answers = linkedMapOf<String, List<Prediction>>()
            models.forEach { session ->
                runCatching { session.inference(text, ask).map(Prediction::parse) }
                    .onSuccess { answers[session.name] = it }
                    .onFailure { logInternal("group '$name': ${session.name} failed on \"${text.take(60)}\": ${it.message}", LogLevel.ERROR) }
            }
            val merged = merge(answers)
            log.run(text, merged.map { it.toString() }, (System.nanoTime() - begun) / 1_000_000)
            val shown = if (ask == Ask.PREDICT) merged.take(1) else merged
            logInternal(
                "$name ${quote(text)} -> " + shown.joinToString(" · ") { p ->
                    val by = answers.filter { (_, ps) -> ps.firstOrNull()?.label == p.label }.keys
                    (if (p.score == null) p.label else "${p.label} ${"%.3f".format(java.util.Locale.US, p.score)}") + (if (by.isEmpty()) "" else " (${by.joinToString()})")
                },
                LogLevel.DATA,
            )
            return answers
        } finally {
            runningCount.decrementAndGet()
            runCount.incrementAndGet()
        }
    }

    override fun getStatus(): Any = mapOf(
        "group" to name, "env" to group.env, "swarm" to "#$id",
        "models" to models.map { "${it.name} #${it.id}${if (it.closed) " (closed)" else ""}" },
        "runs" to runs,
    )

    /** Closes the sessions this swarm opened; a second call does nothing */
    override fun close() {
        if (closed) return
        closed = true
        members.filter { it.second }.forEach { (session, _) -> runCatching { session.close() } }
        ModelGroups.unregister(this)
        log.add("close", "closed after $runs runs")
        logInternal("group '$name' (swarm #$id): closed after $runs runs")
    }

    override fun toString(): String = "#$id group $name · ${models.size} models · ${if (running > 0) "running $running" else "idle"} · runs $runs"

    private fun quote(text: String): String {
        val one = text.replace('\n', ' ').trim()
        return "\"" + (if (one.length > 60) one.take(57) + "..." else one) + "\""
    }
}
