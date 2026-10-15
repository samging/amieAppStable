# Cookbook: scripts to copy

Every script on this page runs as it is in the sandbox: paste it into `main.kts`, press **Run** (⌘↵), read the
console. The shorter versions are one click away in the app: **Examples** on the strip over the editor puts any
of them in the editor (your own script is kept under `AMP/pluginDir/backup/`). Nothing here needs an account;
the one call that does, `saveState`, says so. They are ordered from the first thing to try to the things you will do once a model exists. The exact
signatures are in the [Scripting API](sandbox-scripting.md); this page is about *what to type*.

One habit that saves time: open a model or a database once at the top of the script and keep it open between
runs (see [Running sessions](sessions.md)). The first run loads, every later run is instant.

## 1. Logging: four levels, four toggles

```kotlin
log("plain line")                       // info: the script's own output
info("same as log")
data("rows: 3, columns: text,label")    // data: what the script produced, kept apart from chatter
error("something is off")               // error: red, and the error toggle turns on by itself when a run fails
log("explicit", LogLevel.INTERNAL)      // internal: what the app itself says (sessions, builds, env); LogLevel is in scope
```

- The console shows **info** only when the app starts; the other three toggles on the console strip have counts,
  so `error 1` means one line is waiting behind a toggle. **all** turns everything on.
- Everything, whatever the toggles, goes to `AMP/logs.txt`.
- Every `inference` of a model is also a **data** line (`dummy "sunny" -> lights 0.671, weather 0.496, …`), so
  `data` is where to look when a model answers.

## 2. Ask a model: initMod and its interface

Every fresh `AMP/` has a model called **dummy** (labels `greeting`, `lights`, `weather`), so this runs before
anything is trained:

```kotlin
val m = initMod("dummy")                      // the model's session: opened now, or the one already open

log(m.predict("turn the lights on"))          // lights            the label (the argmax)
log(m.scores("turn the lights on").toString()) // {lights=0.67, weather=0.50, greeting=0.33}
m.predictions("hello there").forEach { p -> log("${p.label} ${p.score}") }
m.inference("hello there").forEach { log(it) } // the raw lines: label:score, best first
log(m.getStatus().toString())                 // the manifest: encoder, dims, labels, description

// Leave it open while you iterate, or:
// m.close()
```

What `initMod` gives you is an `InteractionModel`:

| Member | Returns | Use it for |
|---|---|---|
| `predict(text)` | `String` | The label. Throws when the model answers nothing |
| `scores(text)` | `Map<String, Float>` | Every label with its score, best first; a confidence check |
| `predictions(text)` | `List<Prediction>` | `label` and `score` as values, in the model's order |
| `inference(text)` | `List<String>` | The raw answer: `label:score` for a classifier, a tag per token for NER, whatever `run(text)` returned for a package |
| `getStatus()` | `Any` | The manifest of a model, the metadata of the NER model, the status map of a package |
| `close()` | | Frees the session; the Models page's Terminate does the same |

The three forms of `initMod`:

| Call | What it opens | How many |
|---|---|---|
| `initMod("name")` | A model exported to `AMP/modules/<name>` | **One per model.** A second call returns the open session |
| `initMod("name.jar")` | A package jar (env, `AMP/pluginDir`, a path, or fetched from the server) | **A new instance every call**; close what you open |
| `initMod()` | The bundled BERT NER model | A plain object you own; close it yourself |

A confidence gate, the most common thing to write around `scores`:

```kotlin
val m = initMod("dummy")
listOf("turn the lights on", "what's the weather", "asdf qwerty").forEach { text ->
    val scores = m.scores(text)
    val (label, score) = scores.entries.first()
    if (score < 0.5f) log("$text -> not sure ($label ${"%.2f".format(score)})")
    else log("$text -> $label ${"%.2f".format(score)}")
}
```

## 3. Train your own model from a csv

`AMP/sample-intents.csv` (45 rows, `text,label`) is written on first start; any csv with a text column and a
label column works (the columns are found by their headers: `text`, `label`, `intent`, …).

```kotlin
val train = source("sample-intents.csv")           // looked up as given, then in AMP/, then AMP/pluginDir/
relate(train, "intents-test.csv")                  // optional: validate on this file instead of a 20% split

val built = modelBuilder("intents", train)         // embeds every text with BERT, trains a small head, exports ONNX
built.modelStatus()                                // the validation table: precision, recall, F1 per label
built.modelChart()                                 // the same as ASCII bars, the build history, the confusion matrix

val m = initMod("intents")
log(m.predict("switch off the kitchen light"))     // lights
```

Later, without rebuilding:

```kotlin
validateMetrics("intents")                         // the stored validation
metricsChart("intents", labels = listOf("lights")) // one label's bars
metricsHistory("intents")                          // accuracy and macro F1 across builds
confusionMatrix("intents")
```

The build's phases and progress show on the console strip; the Models page lists the model with its metrics as
plots once it is exported. What happens inside: [Building a model](sandbox-scripting.md#what-modelbuilder-does).

## 4. Named entities: initAmie and initMod()

Both use the bundled BERT NER model (people, organisations, places, misc). `initAmie` opens and closes the model
for one text, so it is the right call for a one-off; `initMod()` keeps it open for many texts.

```kotlin
val tags = initAmie("Angela Merkel met Tim Cook in Berlin")   // printed, and returned: one tag per token
log(tags.joinToString(" "))                                    // O B-PER I-PER O B-PER I-PER O B-LOC O

val ner = initMod()                                            // the same model, kept open
listOf("Apple opened a store in Paris", "Marie Curie worked in Warsaw").forEach { text ->
    log("$text -> ${ner.inference(text).joinToString(" ")}")
}
ner.close()                                                    // not listed on the Models page, so close it here
```

The tags line up with the tokenizer's tokens: `[CLS]` and `[SEP]` at the ends (always `O`) and one tag per word
piece, so a name split into `Mer` and `##kel` gets `B-PER` and `I-PER`. The token ids of a text come from
`forwardContext(text) { ids -> … }` when you need to line them up yourself.

## 5. Embeddings: forwardContext

`forwardContext` runs the encoder and returns the hidden state `[batch][token][hidden]`. Mean-pool it to get one
vector per text; that is exactly what `modelBuilder` feeds its head with.

```kotlin
fun embed(text: String): FloatArray? {
    val hidden = forwardContext(text) { ids -> log("${ids.size} tokens", LogLevel.INTERNAL) } ?: return null
    val tokens = hidden[0]                                 // [token][hidden]
    val dim = tokens[0].size
    return FloatArray(dim) { i -> tokens.sumOf { it[i].toDouble() }.toFloat() / tokens.size }
}

fun cosine(a: FloatArray, b: FloatArray): Double {
    var dot = 0.0; var na = 0.0; var nb = 0.0
    for (i in a.indices) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
    return dot / (Math.sqrt(na) * Math.sqrt(nb))
}

val lights = embed("turn the lights on")!!
listOf("switch the lamp off", "will it rain tomorrow", "good morning").forEach { text ->
    log("$text -> ${"%.3f".format(cosine(lights, embed(text)!!))}")
}
```

`forwardContext` returns null (with an ERROR line) when the text is blank or the encoder failed.

## 6. Any ONNX model: importModelOnnx

A model file with the same `inference / getStatus / close` interface. A transformer (an `input_ids` input) needs
`tokenizer.json` next to it; labels come from a `config.json` (`id2label`) next to it when there is one; a model
with one float input takes the text as comma-separated numbers.

```kotlin
val m = importModelOnnx("/Users/me/models/sentiment/model.onnx")
if (m == null) {
    error("the model could not be opened; the console says why")
} else {
    log(m.getStatus().toString())                          // inputs, outputs, labels, producer
    log(m.predict("what a great day"))                     // POSITIVE
    m.scores("what a great day").forEach { (label, p) -> log("$label ${"%.3f".format(p)}") }
    m.close()
}
```

Its session appears under ORT SESSIONS (`sessions.print()`), not on the Models page.

## 7. Sessions: see what is open, close what you don't need

```kotlin
sessions.print()                         // every ONNX Runtime session: name, inputs, outputs, runs, busy or not
packages.print()                         // the open package instances
ortSessions().forEach { log(it) }        // the same as sessions.print(), as a list

sessions.cancel("module:dummy")          // terminates that session (its next inference fails)
packages.closeAll()                      // every package instance this or any script opened
sessions.cancelAll()                     // everything, encoders included; the next initMod reloads
```

A model has one session at most, so running a script ten times does not pile up ten copies. Packages are the
opposite: every `initMod("x.jar")` is a new instance, so a script that opens one should close it, or start with
`packages.closeAll()`.

## 8. Packages: jars with run(text) and close()

```kotlin
packages.available().forEach { log("${it.name} ${it.version ?: ""}") }   // what the server offers
packages.fetch("amie-demo-1.0.0.jar")                                     // into AMP/pluginDir; null if not there

val echo = runCatching { initMod("amie-demo-1.0.0.jar") }      // a new instance in its own class loader
    .onFailure { error("package not opened: ${it.message}") }   // not found anywhere, or no class with run(String)/close()
    .getOrNull()
if (echo != null) {
    try {
        echo.inference("hello there").forEach { log(it) }
        log(echo.getStatus().toString())             // id, jar, class, source, url, page, runs
    } finally {
        echo.close()
    }
}
```

A jar that only has a static entry point (`run()` or `main`) is a *plugin*, run once with `importPlugin` or
`jars.run`; nothing is opened, nothing to close:

```kotlin
importPlugin("tools.jar")                    // never throws: a missing jar or a failing plugin is one ERROR line
val result = jars.run("tools.jar")           // the return value of its run(), null on failure (the reason is logged)
if (result == null) error("tools.jar did not run; see the error line above") else log("tools.jar -> $result")
jars.quit("tools.jar")                       // unload, so a rebuilt jar is picked up next time
```

Building a package of your own: [Building a package](sandbox-scripting.md#building-a-package).

## 9. A library from Maven Central

Declare it at the top (the line is read before the script compiles), then import as usual. The Models page's
**Add → From Maven Central…** does the same with a search box.

```kotlin
implementation("com.google.code.gson:gson:2.11.0")
import com.google.gson.GsonBuilder

val gson = GsonBuilder().setPrettyPrinting().create()
val m = initMod("dummy")
val answer = mapOf("text" to "turn the lights on", "scores" to m.scores("turn the lights on"))
log(gson.toJson(answer))
```

The env keeps what was ever imported (`internalEnv()` prints it), so the second run downloads nothing. A
`dependency("…")` and `environment(SettingsEnv("name", "profile"))` select another env.

## 10. Remember something between runs: saveState and loadState

The sandbox has no variables that outlive a run. `saveState` keeps a JSON text under a name **on the Amie backend,
per signed-in user**, so it follows you to another machine and survives a fresh `AMP/`; think "the labels I
settled on", "the last csv I used", "my serving port". A local file does the same thing without signing in.

```kotlin
// On the backend (signed in): one JSON document per name
saveState("intents-config", """{"model": "intents", "threshold": 0.6}""")
listStates().forEach { log(it) }                       // devices, intents-config, …
val config = loadState("intents-config")               // the text, or null when there is none (or not signed in)
log(config ?: "nothing saved yet")

// Locally: a file of your own
val file = java.io.File(System.getProperty("user.home"), "intents-config.json")
file.writeText("""{"threshold": 0.6}""")
log(file.readText())
```

A failure (a guest, an unreachable backend, a bad name) is one ERROR line and `loadState` returns null, so a
script can fall back:

```kotlin
val threshold = loadState("intents-config")?.substringAfter("\"threshold\": ")?.substringBefore("}")?.toFloatOrNull() ?: 0.5f
```

## 11. Postgres: database("main")

The connections live in `AMP/databases.json`, written with one `main` entry the first time anything asks for it.
Edit it (the file is not committed to git):

```json
{
  "main": {
    "url": "jdbc:postgresql://localhost:5432/postgres",
    "user": "postgres",
    "password": "$ENV:PGPASSWORD",
    "description": "the local one"
  },
  "warehouse": { "url": "jdbc:postgresql://db.example.com:5432/analytics", "user": "amie", "password": "$ENV:WAREHOUSE_PW" }
}
```

`$ENV:NAME` is read from the environment variable NAME when the connection opens, so the file holds no secret.
Then, in a script:

```kotlin
databases.print()                                       // every entry: url, user, open or not
val db = database("main")                               // the same object for the same name while the app runs

db.tables().forEach { log(it) }                         // public.messages, …
db.print("select id, text from messages order by id desc limit 10")      // a table in the console (data lines)

val rows = db.query("select text from messages where label is null limit ?", 50)   // ? placeholders, in order
val m = initMod("intents")
rows.forEach { row ->
    val text = row["text"].toString()
    db.execute("update messages set label = ?, score = ? where text = ?", m.predict(text), m.scores(text).values.first(), text)
}
log("labeled ${rows.size} rows; ${db.scalar("select count(*) from messages where label is null")} left")
```

| Call | Returns | Notes |
|---|---|---|
| `query(sql, params…)` | `List<Map<String, Any?>>` | One map per row, columns in the query's order; values as JDBC gives them (`Int`, `String`, `java.sql.Timestamp`, …) |
| `scalar(sql, params…)` | `Any?` | The first column of the first row |
| `execute(sql, params…)` | `Int` | Rows touched by INSERT / UPDATE / DELETE, 0 for DDL |
| `print(sql, params…)` | the rows | Also prints them as a table, at most 200 rows |
| `tables()` | `List<String>` | `schema.table`, system schemas left out |
| `close()` | | The app closes open connections on exit; `databases.closeAll()` closes every one now |

Every failure is one ERROR line (`database 'main': relation "messages" does not exist [42P01] in: select …`) and a
`DatabaseException` the script can catch:

```kotlin
val db = database("main")
try {
    db.execute("create table if not exists predictions (text text, label text, score real, at timestamptz default now())")
    db.execute("insert into predictions (text, label, score) values (?, ?, ?)", "hi", "greeting", 0.91f)
} catch (e: Exception) {
    error("database: ${e.message}")      // a bad host, a wrong password, a typo in the SQL: all land here
}
```

A wrong host or password shows as `database 'main': can't connect to jdbc:postgresql://… as 'postgres': …` on
the first statement, not when `database("main")` is called.

## 12. Serve a model and call it over HTTP

A served model answers `POST {"text": "…"}` (or a plain-text body) and `GET ?text=…` at its predict URL with
`{"model": "intents", "predictions": [{"label": "lights", "score": 0.97}, …]}`. The Models page's **Listen**
does it with a switch; a script does it with `serve`:

```kotlin
val url = serve("dummy", port = 8765)               // http://127.0.0.1:8765/models/dummy/predict; null if it could not start
    ?: throw IllegalStateException("serve did not start; the console says why: port taken, path served already, model can't load")
log("serving at $url")
log(servingUrl("dummy") ?: "not serving")            // the same URL while it listens

// Call it from the script, with the errors handled
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

val http = HttpClient.newHttpClient()
fun ask(text: String): String {
    val request = HttpRequest.newBuilder(URI.create(url))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("""{"text": "$text"}"""))
        .build()
    return try {
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        when (response.statusCode()) {
            200 -> response.body()
            400 -> "no text in the request: ${response.body()}"          // {"error": "send text as {\"text\": ...}"}
            405 -> "method not allowed: ${response.body()}"               // the methods list excludes it
            500 -> "the model failed: ${response.body()}"                 // inference threw; also counted as an error on the page
            else -> "unexpected ${response.statusCode()}: ${response.body()}"
        }
    } catch (e: java.net.ConnectException) {
        "nothing listens at $url (${e.message})"
    } catch (e: Exception) {
        "request failed: ${e.message}"
    }
}

log(ask("turn the lights on"))
log(ask(""))                                        // 400
stopServing("dummy")                                // and it stays off on the next start
```

Every request is appended to `AMP/modules/<name>/incoming.jsonl` and counted on the Models page. Host, port,
path and methods passed to `serve` are saved to `serving.json`, so the page shows them; `serve("dummy")` with no
arguments reuses whatever the page saved. The HTTP side is described in [Serving a model](serving.md).

## 13. When something goes wrong

- A script that throws stops with `Error: …` in red and `failed after 3s` on the console strip; the error level
  turns itself on. The tooltip of the status keeps the last error.
- A model that is not there: `initMod("nope")` throws `Module 'nope' not found in …/AMP/modules`. Wrap it when the
  script should go on:

```kotlin
val m = runCatching { initMod("intents") }.getOrElse { e ->
    error("no 'intents' model yet (${e.message}); building one")
    modelBuilder("intents", source("sample-intents.csv"))
    initMod("intents")
}
```

- A terminated session: an `inference` on a session terminated from the Models page throws
  `model intents (#2) is closed`; call `initMod` again to get a fresh one.
- A second **Run** while a script runs only logs "Script is already running"; there is no stop. A script that
  loops forever is stopped by terminating the session it uses, or by closing the app.
- `importPlugin`, `saveState`, `loadState`, `listStates`, `packages.fetch` and `importModelOnnx` never throw:
  they log one ERROR line and return nothing (`null`, an empty list). Database calls and `initMod` do throw, so
  that the script does not go on with no data; the examples above show the `try`.
