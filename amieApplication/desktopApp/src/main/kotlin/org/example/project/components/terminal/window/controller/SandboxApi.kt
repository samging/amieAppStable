package org.example.project.components.terminal.window.controller

/**
 * The script API as the editor sees it: one entry per public member of [ScriptLogger] (keep them in sync).
 * Feeds three things: the completion dropdown, the API colour in the highlighter and the prelude that
 * lets a script call `log("hi")` or `source("train.csv")` without the `amie.` prefix.
 */
object SandboxApi {

    enum class Kind { FUNCTION, PROPERTY }

    /** Parameter of a function entry; types are fully qualified so the prelude compiles without extra imports */
    data class Param(val name: String, val type: String, val default: String? = null)

    /**
     * [target] is the `amie` member the prelude delegate calls (null when the name gets no top-level delegate,
     * like `import`, which the script parser would read as an import directive at the start of a statement).
     */
    data class Entry(
        val name: String,
        val kind: Kind,
        val description: String,
        val params: List<Param> = emptyList(),
        val returns: String? = null,
        val target: String? = name,
        val aliasOf: String? = null,
    ) {
        /** Display form: `source(path: String): DataSource?` or `sessions: OrtSessions` */
        val signature: String
            get() = when (kind) {
                Kind.PROPERTY -> "$name: ${short(returns ?: "Any")}"
                Kind.FUNCTION -> name + params.joinToString(", ", "(", ")") { p ->
                    "${p.name}: ${short(p.type)}" + (p.default?.let { " = ${short(it)}" } ?: "")
                } + (returns?.let { ": ${short(it)}" } ?: "")
            }

        /** Text the dropdown inserts; the caret lands inside the parentheses when it ends with `("")` or `()` */
        val insert: String
            get() {
                val callable = if (target == null && aliasOf == null) "amie.$name" else name
                if (kind == Kind.PROPERTY) return callable
                val required = params.filter { it.default == null }
                return when {
                    required.isEmpty() -> "$callable()"
                    required.first().type == STRING -> "$callable(\"\")"
                    else -> "$callable()"
                }
            }

        /** The prelude line delegating to `amie`, or null when the name is not exposed prefix-free */
        fun delegate(): String? {
            val member = target ?: return null
            return when (kind) {
                Kind.PROPERTY -> "val $name get() = amie.$member"
                Kind.FUNCTION -> {
                    val declared = params.joinToString(", ") { p -> "${p.name}: ${p.type}" + (p.default?.let { " = $it" } ?: "") }
                    val passed = params.joinToString(", ") { it.name }
                    "fun $name($declared) = amie.$member($passed)"
                }
            }
        }
    }

    private const val PKG = "org.example.project.components.terminal.window.controller"
    private const val STRING = "String"
    private const val PATH = "java.nio.file.Path"
    private const val LOG_LEVEL = "$PKG.LogLevel"
    private const val INTERACTION_MODEL = "org.example.project.components.terminal.integrations.amiePilot.InteractionModel"
    private const val DATA_SOURCE = "$PKG.sourceFile.DataSource"
    private const val METRICS_REPORT = "$PKG.modules.MetricsReport"
    private const val BUILD_SETTINGS = "$PKG.envBuilder.BuildSettings"
    private const val SETTINGS_ENV = "$PKG.envBuilder.SettingsEnv"
    private const val ORT_SESSIONS = "$PKG.modelLoaders.OrtSessions"
    private const val JAR_INTERPRETER = "$PKG.pluginEngine.JarInterpreter"
    private const val JAR_PACKAGES = "$PKG.pluginEngine.JarPackages"
    private const val MODEL_INTERFACE = "$PKG.ModelCreationalInterface"
    private const val DATABASE = "$PKG.databases.Database"
    private const val DATABASES = "$PKG.databases.Databases"

    private fun fn(name: String, description: String, vararg params: Param, returns: String? = null, target: String? = name, aliasOf: String? = null) =
        Entry(name, Kind.FUNCTION, description, params.toList(), returns, target, aliasOf)

    private fun prop(name: String, type: String, description: String) = Entry(name, Kind.PROPERTY, description, returns = type)

    /** Every member of [ScriptLogger], in its order, plus the prelude-only aliases at the end */
    val entries: List<Entry> = listOf(
        fn("log", "Prints a line in the console at the given level", Param("message", STRING), Param("level", LOG_LEVEL, "$LOG_LEVEL.INFO")),
        fn("info", "Prints an info line in the console", Param("message", STRING)),
        fn("error", "Prints an error line in the console", Param("message", STRING)),
        fn("data", "Prints a data line in the console", Param("message", STRING)),
        fn("initAmie", "Named entities with the bundled BERT NER model: one tag per token (O, B-PER, I-ORG…), printed and returned; a one-off call, initMod() keeps the model open", Param("text", STRING), returns = "List<String>"),
        fn("initMod", "Opens the default interaction model", returns = INTERACTION_MODEL),
        fn("initMod", "The session of the module modelBuilder exported to AMP/modules/<modelName>: the one already open on it, else a new one. A new instance of a package jar when the name ends in .jar (local or fetched from the Amie server). .predict(text) is the label, .scores(text) every label with its score, .inference(text) the raw label:score lines", Param("modelName", STRING), returns = INTERACTION_MODEL),
        prop("packages", JAR_PACKAGES, "Package jar sessions: packages.available(), packages.fetch(\"x.jar\"), packages.print(), packages.closeAll()"),
        fn("saveState", "Stores a JSON text as the signed-in user's saved state <name> on the Amie backend (uploads/<user>/state/<name>.json)", Param("name", STRING), Param("json", STRING)),
        fn("loadState", "The JSON text of the saved state <name> on the Amie backend, null when there is none", Param("name", STRING), returns = "String?"),
        fn("listStates", "Names of the signed-in user's saved states on the Amie backend", returns = "List<String>"),
        fn("importModelOnnx", "A fixed ONNX model with initMod's interface; its session is managed by sessions", Param("it", PATH), returns = "$INTERACTION_MODEL?"),
        fn("importModelOnnx", "A fixed ONNX model with initMod's interface, by file path", Param("path", STRING), returns = "$INTERACTION_MODEL?"),
        prop("sessions", ORT_SESSIONS, "Global ORT sessions: sessions.print(), sessions.cancel(\"name\"), sessions.cancelAll()"),
        fn("ortSessions", "Reads every open ORT session: inputs, outputs, runs and whether it is busy", returns = "List<String>"),
        fn("importModelTensorflow", "Loads a TensorFlow model from a path", Param("it", PATH)),
        fn("forwardContext", "Runs text through the context model; tokens receives the input ids, returns last_hidden_state",
            Param("text", STRING), Param("tokens", "(LongArray) -> Unit", "{}"), returns = "Array<Array<FloatArray>>?"),
        fn("validateMetrics", "Validates a model file on a train and a test file", Param("modelPath", PATH), Param("train", PATH), Param("test", PATH)),
        fn("validateMetrics", "Prints the last stored validation of AMP/modules/<modelName>; labels narrows the table",
            Param("modelName", STRING), Param("labels", "List<String>", "emptyList()"), returns = METRICS_REPORT),
        fn("metricsChart", "Draws ASCII bar charts of the stored validation; labels narrows the chart",
            Param("modelName", STRING), Param("labels", "List<String>", "emptyList()"), returns = METRICS_REPORT),
        fn("metricsHistory", "Draws an ASCII chart of accuracy and macro F1 across builds", Param("modelName", STRING), returns = METRICS_REPORT),
        fn("confusionMatrix", "Draws the confusion matrix of the stored validation as an ASCII grid", Param("modelName", STRING), returns = METRICS_REPORT),
        fn("implementation", "Evaluates Gradle build settings", Param("it", BUILD_SETTINGS)),
        fn("import", "Selects the package environment the script is sourced with (read before the run)", Param("it", SETTINGS_ENV), target = null),
        fn("import", "Gradle-style dependency \"group:module:version\" or a jar file name, sourced before the script compiles", Param("notation", STRING), target = null),
        fn("implementation", "Gradle-style dependency \"group:module:version\" or a jar file name, sourced before the script compiles", Param("notation", STRING)),
        fn("internalEnv", "Prints the env being sourced, what this run sourced and everything in it; returns its packages", returns = "List<String>"),
        prop("jars", JAR_INTERPRETER, "Plugin jars by file name: jars.run(\"plugJar.jar\"), jars.quit(\"plugJar.jar\")"),
        fn("sourceFile", "Rows of the file keyed by row index with their count; a named column keeps only that cell per row",
            Param("it", STRING), Param("column", STRING, "\"\""), returns = "Pair<Int, Map<Int, List<String>>>"),
        fn("source", "Labeled data file with its text/label columns inferred", Param("path", STRING), returns = "$DATA_SOURCE?"),
        fn("relate", "Gives the model a test file to validate on after training on train", Param("train", "$DATA_SOURCE?"), Param("test", STRING), returns = "$DATA_SOURCE?"),
        fn("modelBuilder", "Trains a classifier on the data source and exports it to AMP/modules/<modelName>",
            Param("modelName", STRING), Param("data", "$DATA_SOURCE?"), returns = MODEL_INTERFACE),
        fn("importPlugin", "Runs a plugin jar's static run() or main; a missing jar or a failure is one ERROR line, never an exception", Param("plugin", STRING)),
        fn("database", "A database from AMP/databases.json: .query(sql, params), .execute(sql, params), .print(sql), .scalar(sql), .tables(), .close()", Param("name", STRING, "\"main\""), returns = DATABASE),
        prop("databases", DATABASES, "The configured databases: databases.print(), databases.names(), databases.closeAll()"),
        fn("serve", "Serves the model over HTTP like the Models page's Listen; null keeps serving.json's host/port/path/methods. Returns the predict URL, null when it could not start",
            Param("modelName", STRING), Param("port", "Int?", "null"), Param("host", "String?", "null"), Param("path", "String?", "null"), Param("methods", "List<String>?", "null"), returns = "String?"),
        fn("stopServing", "Stops serving the model and remembers it", Param("modelName", STRING)),
        fn("servingUrl", "The predict URL of a model being served now, null when it is not", Param("modelName", STRING), returns = "String?"),
        fn("modelBuilder", "Trains a classifier on the labeled data the lambda supplies", Param("modelName", STRING), Param("it", "(Any) -> Any"), returns = MODEL_INTERFACE),
        // prelude-only aliases: `import` cannot be a top-level function name in a script
        fn("dependency", "Alias of implementation: adds \"group:module:version\" or a jar file name to the environment", Param("notation", STRING), target = "implementation", aliasOf = "implementation"),
        fn("environment", "Alias of amie.import(SettingsEnv(...)): selects the package environment the script is sourced with", Param("it", SETTINGS_ENV), target = "import", aliasOf = "import"),
    )

    /** The object itself, offered by the dropdown and coloured like the API */
    const val RECEIVER = "amie"

    val receiverEntry = Entry(RECEIVER, Kind.PROPERTY, "The script API object; every name here is also reachable as amie.name", returns = "ScriptLogger", target = null)

    val names: Set<String> = entries.map { it.name }.toSet()
    val functionNames: Set<String> = entries.filter { it.kind == Kind.FUNCTION }.map { it.name }.toSet()
    val propertyNames: Set<String> = entries.filter { it.kind == Kind.PROPERTY }.map { it.name }.toSet()

    /** Entries whose name starts with [prefix] (case-insensitive); an empty prefix lists everything once per name */
    fun complete(prefix: String, afterReceiver: Boolean = false, limit: Int = 8): List<Entry> {
        val candidates = entries.filter { entry ->
            entry.name.startsWith(prefix, ignoreCase = true) && (!afterReceiver || entry.aliasOf == null)
        }
        return candidates.sortedWith(compareBy({ !it.name.startsWith(prefix) }, { it.name.length })).take(limit)
    }

    /**
     * Top-level declarations placed after `val amie = ...` in the assembled script so every API member can be
     * called without the prefix. Overloads each get a delegate; defaults are kept; `amie.` still works.
     */
    fun prelude(): String =
        entries.mapNotNull { it.delegate() }.distinct().joinToString("\n", postfix = "\n")

    /** `java.nio.file.Path` -> `Path`, `org...LogLevel.INFO` -> `LogLevel.INFO`; generic and function types keep their shape */
    private fun short(type: String): String = type.replace(Regex("""\b(?:[a-z_][a-z0-9_]*\.)+"""), "")
}
