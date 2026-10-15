package org.example.project.components.terminal.window.controller

/**
 * The scripts behind the sandbox's Examples menu: complete, they run as they are, and none of them needs an
 * account or a server. Every one is compiled by the unit tests (CookbookTest), so they cannot go stale.
 */
object SandboxExamples {

    class Example(val title: String, val group: String, val code: String)

    val all: List<Example> = listOf(
        Example("Hello: log at every level", "First steps", """
            |// Run this (⌘↵ / Ctrl+Enter). The console opens by itself and shows the info lines;
            |// the data, error and internal toggles on its strip count what they hide.
            |log("hello from main.kts")
            |info("the same level as log")
            |data("what the script produced, kept apart from chatter")
            |error("how a problem looks (the error toggle turns on when a run fails)")
            |log("what the app itself says", LogLevel.INTERNAL)
            |""".trimMargin()),

        Example("Ask the dummy model", "First steps", """
            |// Every AMP folder has a model called dummy (labels greeting, lights, weather),
            |// so this works before anything is trained. One session per model: run it twice, it stays open.
            |val m = initMod("dummy")
            |
            |log(m.predict("turn the lights on"))                   // the label (the argmax)
            |log(m.scores("turn the lights on").toString())         // every label with its score, best first
            |m.inference("hello there").forEach { log(it) }         // the raw label:score lines
            |log(m.getStatus().toString())                          // encoder, dims, labels, description
            |
            |// Open Models > Sessions to see it; m.close() or Terminate there closes it.
            |""".trimMargin()),

        Example("A confidence gate", "First steps", """
            |// Say "not sure" instead of guessing when the best score is low.
            |val m = initMod("dummy")
            |val threshold = 0.5f
            |listOf("turn the lights on", "what's the weather", "asdf qwerty").forEach { text ->
            |    val scores = m.scores(text)
            |    val (label, score) = scores.entries.first()
            |    if (score < threshold) log("${'$'}text -> not sure (${'$'}label ${'$'}{"%.2f".format(score)})")
            |    else log("${'$'}text -> ${'$'}label ${'$'}{"%.2f".format(score)}")
            |}
            |""".trimMargin()),

        Example("Train a model from a csv", "Models", """
            |// AMP/sample-intents.csv (45 rows: text,label) is written on first start; any csv with a
            |// text column and a label column works. The build's progress shows on the console strip.
            |val train = source("sample-intents.csv")
            |val built = modelBuilder("intents", train)     // embeds with BERT, trains a small head, exports ONNX
            |built.modelStatus()                             // precision, recall, F1 per label
            |built.modelChart()                              // bars, build history, confusion matrix
            |
            |val m = initMod("intents")
            |log(m.predict("switch off the kitchen light"))  // lights
            |""".trimMargin()),

        Example("Train with your own heads", "Models", """
            |// modelBuilder cleans the rows first (the console says what it dropped and why), then tries every head
            |// in the options on the same rows and keeps the best. Without options it tries linear, dense[16],
            |// dense[32] and dense[64, 16]; here are three of your own, picked by accuracy.
            |val train = source("sample-intents.csv")
            |val options = buildOptions(
            |    candidates = listOf(
            |        architecture(hidden = listOf(32, 16), epochs = 200, learningRate = 0.005),
            |        architecture(hidden = emptyList(), epochs = 100),          // a linear head
            |        architecture(hidden = listOf(64), activation = "Tanh"),
            |    ),
            |    pickBy = "accuracy",
            |)
            |val built = modelBuilder("intents-custom", train, options)
            |built.modelStatus()                             // the chosen head's validation table
            |""".trimMargin()),

        Example("Read a model's metrics", "Models", """
            |// The stored validation of a model, without rebuilding it. "dummy" has numbers after
            |// Models > Metrics > Validate on the sample data; "intents" after the training example.
            |val name = "dummy"
            |validateMetrics(name)                           // the table
            |metricsChart(name)                              // bars per label
            |metricsHistory(name)                            // accuracy and macro F1 across builds
            |confusionMatrix(name)
            |""".trimMargin()),

        Example("Named entities (NER)", "Models", """
            |// The bundled BERT NER model tags people, organisations and places, one tag per token.
            |// initAmie is a one-off (opens and closes); initMod() keeps the model open for many texts.
            |val tags = initAmie("Angela Merkel met Tim Cook in Berlin")
            |log(tags.joinToString(" "))
            |
            |val ner = initMod()
            |listOf("Apple opened a store in Paris", "Marie Curie worked in Warsaw").forEach { text ->
            |    log("${'$'}text -> ${'$'}{ner.inference(text).joinToString(" ")}")
            |}
            |ner.close()                                     // not on the Models page, so close it here
            |""".trimMargin()),

        Example("Embeddings and similarity", "Models", """
            |// forwardContext runs the encoder: [batch][token][hidden]. Mean-pool it to one vector per text.
            |fun embed(text: String): FloatArray? {
            |    val hidden = forwardContext(text) ?: return null
            |    val tokens = hidden[0]
            |    return FloatArray(tokens[0].size) { i -> tokens.sumOf { it[i].toDouble() }.toFloat() / tokens.size }
            |}
            |fun cosine(a: FloatArray, b: FloatArray): Double {
            |    var dot = 0.0; var na = 0.0; var nb = 0.0
            |    for (i in a.indices) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
            |    return dot / (Math.sqrt(na) * Math.sqrt(nb))
            |}
            |
            |val lights = embed("turn the lights on")!!
            |listOf("switch the lamp off", "will it rain tomorrow", "good morning").forEach { text ->
            |    log("${'$'}text -> ${'$'}{"%.3f".format(cosine(lights, embed(text)!!))}")
            |}
            |""".trimMargin()),

        Example("A group of models, asked together", "Models", """
            |// A group is a named set of models kept in the env (AMP/env/<env>/groups.json). group("name")
            |// opens them as one swarm: predict is the best score across them, vote the label most give.
            |// The Models page makes groups too (a model's ⋯ menu > New group with…); this one holds the dummy.
            |groups.create("house", listOf("dummy"))
            |groups.print()
            |val swarm = group("house")
            |log(swarm.predict("turn the lights on"))                 // the label with the best score across the models
            |log(swarm.vote("turn the lights on"))                    // the label most models answer
            |swarm.each("turn the lights on").forEach { (model, label) -> log("${'$'}model says ${'$'}label") }
            |swarm.scores("turn the lights on").forEach { (label, score) -> log("${'$'}label ${'$'}score") }
            |internalEnv()                                            // the env's packages and its groups
            |swarm.close()                                            // closes the sessions it opened
            |""".trimMargin()),

        Example("Sessions: what is open", "Housekeeping", """
            |// Models have one session each; packages get a new instance per initMod("x.jar").
            |sessions.print()                                // every ONNX Runtime session
            |packages.print()                                // open package instances
            |
            |// packages.closeAll()                          // every package instance
            |// sessions.cancel("module:dummy")              // one session by name
            |// sessions.cancelAll()                         // everything, encoders included
            |""".trimMargin()),

        Example("Postgres: query and update", "Data", """
            |// Connections live in AMP/databases.json, written with a "main" entry the first time this runs:
            |// edit its url, user and password (${'$'}ENV:PGPASSWORD reads an environment variable), then run again.
            |databases.print()
            |val db = database("main")
            |try {
            |    db.tables().forEach { log(it) }
            |    db.execute("create table if not exists predictions (text text, label text, score real, at timestamptz default now())")
            |    val m = initMod("dummy")
            |    listOf("turn the lights on", "good morning").forEach { text ->
            |        db.execute("insert into predictions (text, label, score) values (?, ?, ?)", text, m.predict(text), m.scores(text).values.first())
            |    }
            |    db.print("select text, label, score, at from predictions order by at desc limit 10")
            |    log("rows: ${'$'}{db.scalar("select count(*) from predictions")}")
            |} catch (e: Exception) {
            |    error("database: ${'$'}{e.message}")       // wrong host or password, a typo in the SQL: all land here
            |}
            |""".trimMargin()),

        Example("Serve a model and call it", "Serving", """
            |// Serves dummy over HTTP (like Listen on the Models page), asks it from this script, stops it.
            |import java.net.URI
            |import java.net.http.HttpClient
            |import java.net.http.HttpRequest
            |import java.net.http.HttpResponse
            |
            |val url = serve("dummy", port = 8765)
            |    ?: throw IllegalStateException("serve did not start; the console says why (port taken, model can't load)")
            |log("serving at ${'$'}url")
            |
            |val http = HttpClient.newHttpClient()
            |fun ask(text: String): String {
            |    val request = HttpRequest.newBuilder(URI.create(url))
            |        .header("Content-Type", "application/json")
            |        .POST(HttpRequest.BodyPublishers.ofString(""${'"'}{"text": "${'$'}text"}""${'"'}))
            |        .build()
            |    return try {
            |        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            |        when (response.statusCode()) {
            |            200 -> response.body()
            |            400 -> "no text sent: ${'$'}{response.body()}"
            |            405 -> "method not allowed: ${'$'}{response.body()}"
            |            500 -> "the model failed: ${'$'}{response.body()}"
            |            else -> "unexpected ${'$'}{response.statusCode()}: ${'$'}{response.body()}"
            |        }
            |    } catch (e: java.net.ConnectException) {
            |        "nothing listens at ${'$'}url"
            |    }
            |}
            |log(ask("turn the lights on"))
            |log(ask(""))                                    // 400
            |stopServing("dummy")
            |""".trimMargin()),

        Example("When something goes wrong", "Housekeeping", """
            |// initMod and database calls throw, so a script does not go on without data; wrap what may be missing.
            |val m = runCatching { initMod("intents") }.getOrElse { e ->
            |    error("no 'intents' model yet (${'$'}{e.message}); building one from the sample data")
            |    modelBuilder("intents", source("sample-intents.csv"))
            |    initMod("intents")
            |}
            |log(m.predict("good morning"))
            |
            |// importPlugin, packages.fetch, importModelOnnx, saveState/loadState never throw:
            |// they log one ERROR line and return nothing.
            |importPlugin("not-there.jar")
            |val onnx = importModelOnnx("/nowhere/model.onnx")
            |log(if (onnx == null) "no model at that path, as expected" else "opened")
            |""".trimMargin()),
    )

    val groups: List<String> get() = all.map { it.group }.distinct()
}
