# Scripting API

Complete scripts for every call below, ready to paste, are in the [Cookbook](cookbook.md).

The sandbox is a Kotlin script editor (`main.kts`) with a console (how to use the editor, the completion dropdown and
the console filters is in [The sandbox](sandbox.md)). The script runs inside the app with the sandbox API bound to
it; everything it logs shows in the console on the right and is appended to `AMP/logs.txt`.

Every function below can be called **directly** (`source("x.csv")`) or through the `amie` object
(`amie.source("x.csv")`). The only difference is `amie.import`: without the prefix write
`implementation("group:module:version")` or `dependency(...)` for a package and `environment(SettingsEnv(...))` for
an environment, since a bare `import(...)` reads as a Kotlin import.

## Running a script

1. Type in the editor. The script is saved to `AMP/pluginDir/main.kts` as you type.
2. Press **Run** over the editor or **⌘↵** (Ctrl+Enter elsewhere) inside it.
3. While the script runs the console strip reads `running 12s` (with the model build's phase and progress when
   `modelBuilder` is working) and the Run button reads *Running…*; pressing Run again only logs "Script is
   already running".
4. When it ends the strip shows `finished in 41s` or `failed after 3s`, and the console gets the same line.

The level toggles on the console strip (internal, error, data, info) filter what is shown; only **info** is on at
start, the counts on the others say what is hidden. **Clear** empties the view, the log file keeps it all.

### Run phases

- **Configuration.** `implementation("group:module:version")` / `amie.import("...")` and
  `environment(SettingsEnv("env", "profile"))` / `amie.import(SettingsEnv(...))` calls are read from the script text *before* it compiles, the packages are resolved from Maven Central (cached under
  `AMP/env/caches`) and put on the script's classpath. Each env (`AMP/env/<name>/env.lock.json`) keeps what was ever
  imported into it, like a virtualenv.
- **Execution.** The script's own `import` lines are moved to the top (a trailing `//` comment on one is fine),
  `LogLevel` is imported for you, then `val amie = bindings["logger"] as ScriptLogger`
  and a prelude of top-level functions delegating to it (`fun source(path: String) = amie.source(path)`, …) are
  inserted before the rest of the code, and the whole is evaluated with the Kotlin JSR-223 engine on a background
  thread. The prelude is generated from one list (`SandboxApi.kt`), which also feeds the editor's completion dropdown.

## The API

Defined in `ScriptLogger.kt`. The most used calls (prefix `amie.` optional):

| Call | What it does |
|---|---|
| `log("...")`, `info`, `error`, `data` | Console lines at the given level: **info** for the script's own output (the only level on at start), **data** for what it produced (every model answer is one), **error** (turns itself on when a run fails), **internal** for the app's own lines |
| `source("train.csv")` | Reads a labeled CSV (text and label columns are inferred from headers like `text`, `label`, `intent`). Looked up as given, then in `AMP/`, then `AMP/pluginDir/` |
| `relate(train, "test.csv")` | Attaches a test file to a training source; the model validates on it instead of a split. Remembered in `AMP/relations.json` |
| `sourceFile("file.csv", column = "")` | Raw rows keyed by index, optionally one column |
| `modelBuilder("name", data)` | Trains and exports an intent classifier (below) |
| `modelBuilder("name") { ... }` | Same, the lambda returns the data |
| `modelBuilder("name", data, buildOptions(...))` | The same build with your own options: `architecture(hidden, epochs, batchSize, learningRate, activation)` candidates, `pickBy` (`macroF1` or `accuracy`), `preprocess`, `validationFraction`, `seed`, `balanceClasses`. See [Models and metrics](models.md#your-own-head) |
| `checkData(data)` | The preprocessor alone, without training: what it would clean, drop and refuse, and whether a build could start (returns true then) |
| `initMod("name")` | The session of an exported model: the one already open, else a new one (listed on the Models page, terminable there); `.predict("text")` returns the predicted label, `.scores("text")` every label with its score (best first), `.inference("text")` the raw `label:probability` lines sorted best first; `.close()`. `initMod("dummy")` works on a fresh checkout. How sessions live and die: [Running sessions](sessions.md) |
| `initMod()` | The bundled BERT NER model, kept open: `.inference("text")` is one tag per token |
| `group("name")` | The swarm of a model group of the env: the models asked together as one session (the one already open on the group, else a new one, listed on the Models page as a *model group* session). `.predict("text")` is the label with the best score across the models, `.scores("text")` each label's best score, `.vote("text")` the label most models give, `.each("text")` every model's own label, `.ask("text")` every model's raw lines; `.close()` closes the sessions it opened. Groups are made on the Models page (a model's ⋯ menu) or with `groups` |
| `groups` | The model groups of the envs: `groups.print()`, `groups.names()`, `groups.create("name", listOf("a", "b"))` (in the env the script is sourced in), `groups.add("name", "model")`, `groups.remove("name", "model")`, `groups.delete("name")`, `groups.closeAll()`. A group belongs to an env: it is kept in `AMP/env/<env>/groups.json`, `group("name")` looks in the script's env first, and `internalEnv()` lists them with the packages |
| `initAmie("text")` | Named entities in one text with the same NER model, opened and closed for the call: the tags are printed and returned. For many texts use `initMod()` |
| `initMod("name.jar")` | A new instance of a package jar behind the same interface: `.inference("text")` calls its `run(text)`, `.close()` its `close()`. Looked up in the env, `AMP/pluginDir`, as a path, then fetched from the Amie server (below) |
| `validateMetrics("name", labels = listOf(...))` | Prints the stored validation table of a model |
| `metricsChart("name", labels = listOf(...))` | ASCII bars: accuracy, macro F1 and the F1 of each label with precision, recall and support |
| `metricsHistory("name")` | ASCII line chart of accuracy and macro F1 across every build |
| `confusionMatrix("name")` | The last validation's confusion matrix as a shaded ASCII grid |
| `forwardContext("text") { ids -> }` | Runs the encoder and returns the hidden state `[batch][token][hidden]` |
| `importModelOnnx("path.onnx")` | Any ONNX model behind the `inference / getStatus / close` interface |
| `sessions.print()`, `sessions.cancel("name")`, `sessions.cancelAll()` | The open ORT sessions |
| `packages.available()`, `packages.fetch("x.jar")`, `packages.print()`, `packages.closeAll()` | The package jars the server offers, a download into `AMP/pluginDir`, the open package sessions |
| `saveState("name", json)`, `loadState("name")`, `listStates()` | A JSON text kept per user on the Amie backend (signed in): store it under a name, read it back (null when absent), list the names. The sandbox has no state that outlives a run; this is how a script remembers a choice across runs and machines |
| `database("main")` | A Postgres database from `AMP/databases.json`: `.query(sql, params…)` rows as maps, `.scalar`, `.execute` for changes, `.print(sql)` a table in the console, `.tables()`, `.close()`. Failures are one ERROR line and a `DatabaseException` |
| `databases.print()`, `databases.names()`, `databases.closeAll()` | The configured databases |
| `serve("name", port = 8765)` | Serves the model over HTTP like the Models page's Listen; returns the predict URL or null. `stopServing("name")`, `servingUrl("name")` |
| `jars.run("plugin.jar")`, `jars.quit("plugin.jar")` | Runs a static `run()` or `main` of a plugin jar once; not a package session. `run` returns what the method returned, null on failure (the reason is an ERROR line) |
| `importPlugin("plugin.jar")` | The same run, found in the env, `AMP/pluginDir`, as a path or on the server; never throws: a missing jar, a jar without an entry point or a plugin that fails is one ERROR line and the script goes on |
| `implementation("group:module:version")`, `amie.import("...")` | Add a package to the script's environment (the Models page's **Add → From Maven Central…** does the same with a search) |
| `implementation("my.jar")`, `amie.import("my.jar")` | Add a jar file to the environment |
| `environment(SettingsEnv("name", "profile"))`, `amie.import(SettingsEnv(...))` | Select the script environment (must be written literally) |
| `internalEnv()` | Print what the environment contains |

## Reading a prediction

Every model session, whatever opened it (`initMod("name")`, `initMod("x.jar")`, `importModelOnnx(...)`), answers
through the same three calls on top of `inference`:

| Call | Returns | Example |
|---|---|---|
| `predict("text")` | The predicted label: the best answer of `inference` without its score (the argmax of a classifier). Throws when the model answers nothing | `"lights"` |
| `scores("text")` | Every label with its score, best first, as a `Map<String, Float>`. Answers without a score are left out | `{lights=0.67, weather=0.50, greeting=0.33}` |
| `predictions("text")` | The raw answers parsed into `Prediction(label, score)`, in the model's order; `score` is null when the answer had none | `[lights:0.67, weather:0.50, …]` |
| `inference("text")` | The raw lines a model answers: `label:score` for a classifier, one tag per token for the NER model, whatever `run(text)` returned for a package | `["lights:0.67", "weather:0.50", …]` |

```kotlin
val m = initMod("intents")
log(m.predict("turn the lights on"))                        // lights
val scores = m.scores("turn the lights on")                 // {lights=0.97, weather=0.02, greeting=0.01}
log("confidence ${scores.values.first()}")                  // 0.97
if (scores.values.first() < 0.5f) log("not sure", LogLevel.ERROR)
m.predictions("turn the lights on").forEach { p -> log("${p.label} ${p.score}") }
```

Each call is logged as a DATA line in the console in the shape it was asked: `predict` logs the text and the one
label with its score (`intents "turn the lights on" -> lights 0.970`), `scores` and `inference` the text and every
label with its score. Each of the three calls runs the model once, so ask once and keep the result instead of
calling `predict` and `scores` on the same text. A package's answers (`echo: hello`) have no score: `predict` returns the first line
whole and `scores` is empty.

## Building a model

```kotlin
val train = source("intents.csv")          // columns: text,label
relate(train, "intents-test.csv")          // optional test file
val model = modelBuilder("intents", train)
model.modelStatus()                        // the stored validation table
model.modelChart()                         // bars, build history and confusion matrix

val m = initMod("intents")
log(m.predict("turn the lights on"))                 // lights
log(m.scores("turn the lights on").toString())       // {lights=0.97, weather=0.02, greeting=0.01}
m.close()
```

The charts are described with examples in [Models and metrics](models.md#reading-the-metrics).

## Packages

A package is a jar with one public class that has `run(text: String): List<String>` and `close()`, the contract the
website checks on upload (implementing the backend's `AmiePackage` interface is the easiest way to get there).
Packages are bottles: every `initMod("name.jar")` makes a fresh instance in its own class loader, so two sessions
on the same jar never share state, and `close()` releases that instance and its loader. The jar is looked up in the
active env, then `AMP/pluginDir`, then as a path; when it is nowhere local it is fetched from the Amie server (the
website's `/api/jar-download`, else the S3 service's `/api/aws/jars/<name>`; addresses in `AMP/backend.json`) and
stored in `AMP/pluginDir`, so the next open is local. Next to it, `AMP/pluginDir/<name>.source.json` records where
it came from: the exact download URL, the website page that lists and describes it (`<site>/downloads`), version,
size, publish date and fetch time. `getStatus()` of a package session carries the `url` and `page`, and the
Models page shows them behind the **i** toggle of the package.

```kotlin
val echo = initMod("echo-1.0.jar")        // fetched from the server when it is not local
echo.inference("hello there").forEach { log(it) }
log(echo.getStatus().toString())          // jar, class, source, runs
echo.close()

packages.available().forEach { log("${it.name} ${it.size}") }   // what the server lists
packages.print()                                                   // open package sessions
```

The package class is the one the jar's metadata names (below) when it satisfies the contract, else the manifest's
`Main-Class` when it does, else a class implementing an interface named `AmiePackage`, else the first class in the
jar that does. `run` executes on the calling thread with
the jar's class loader as context loader, and what it returns is shown one line per element. Open package sessions
also appear under **Sessions** on the [Models page](models-page.md).

### What a jar needs: metadata, configuration and test runs

A jar can say what it requires in `META-INF/amie/package.json` (or `amie-package.json` at its root). For a jar
that ships none, `AMP/pluginDir/<jar>.package.json` beside it says the same (**⋯ > Write metadata template** on
the Models page writes one to fill in); when both exist, the sidecar's fields override the jar's. The shape:

```json
{
  "name": "weather", "version": "1.2", "description": "answers with the forecast for a place",
  "java": 21,
  "entry": "com.example.Weather",
  "config": [
    {"file": "weather.properties", "required": true, "description": "the API key", "template": "apiKey=\n"}
  ],
  "env": [{"name": "WEATHER_PROXY", "required": false, "description": "an HTTP proxy, if one is needed"}],
  "tests": [
    {"name": "a known place", "input": "Paris", "contains": "Paris"},
    {"name": "answers in one line", "input": "Berlin", "lines": 1},
    {"name": "exact", "input": "ping", "expect": "pong"}
  ],
  "notes": "free text for whoever runs it",
  "flags": [{"text": "stable", "color": "green"}, {"text": "v2 api", "color": "blue"}]
}
```

`flags` are coloured tags the Models page shows on the jar's row and panel beside its version, Java, language and
test flags; the colour is one of slate, green, amber, red, violet, blue. Flags added on the page go to the
sidecar and add to the jar's own.

The runtime reads it before the jar is opened (`pluginEngine/PackageMeta.kt`, `JarPackages.open`):

- **Configuration lives in `AMP/pluginDir/config/<jar without .jar>/`.** The folder is made on the first open,
  and every declared `config` file that is missing is written from its `template`, so the person finds a file to
  fill in instead of a stack trace. A required file with no template that is missing is an error line in the
  console (the jar still opens). The package finds the folder three ways: it is the **first root of the jar's
  class loader**, so `getClass().getClassLoader().getResource("weather.properties")` (or `getResourceAsStream`)
  resolves there, ahead of a default copy inside the jar; the system properties **`amie.package.config`** (the
  folder) and `amie.package.name` (the jar) are set while it is opened, with `amie.package.config.<jar>` kept
  for the jar; and a public **`configure(File)`**, `configure(Path)`, `configure(String)` or
  `configure(Map<String, String>)` method of the package class, when it has one, is called with the folder right
  after the constructor (the map holds `dir` and each file in the folder by name). `jars.run("x.jar")` loads a
  jar the same way.
- **`java`** is checked against the Java this app runs: a jar that needs a newer one is refused with the reason.
- **`entry`** picks the package class when the jar has more than one candidate.
- **`tests`** are the jar's own test runs: `packages.test("x.jar")` in a script, or **Test run** on the Models
  page, opens a fresh session as `initMod` would (configuration included), sends each `input` through `run`,
  checks the answer against `expect` (the whole answer), `contains` (some line) and `lines` (how many), closes
  the session, prints the report and keeps it as `AMP/pluginDir/<jar>.tests.json`. A test with none of the three
  passes when `run` does not throw. With no tests declared the run is one call with `"hello"` that must answer.

```kotlin
packages.meta("weather.jar")?.let { log("needs Java ${it.java}, files ${it.config.map { c -> c.file }}") }
val report = packages.test("weather.jar")          // the console gets one line per test
log(if (report.ok) "all ${report.passed} passed" else "${report.failed} failed")
```

The Models page shows all of it on the jar's panel: **REQUIREMENTS** (Java, package class, metadata, config
folder, every config file and variable, the last test run, each marked met or not), **CONFIG** (the folder with
**Reveal folder**, the files and their state) and **TESTS** (what is declared, and the last run's answers).

### Session logs

(How to work with sessions day to day, keep them warm between runs and close them is in [Running sessions](sessions.md).)

Every session of a model or a package keeps a log of what it was asked and answered: an `open` line, one `run`
line per `inference` call with the text, the first answers and the time it took, an `error` line when the call
threw, and a `close` line with the number of runs. The Models page shows it behind **Log** on the session, and the
same lines are appended to `AMP/modules/<name>/sessions.log` for a model and `AMP/pluginDir/<jar>.sessions.log`
for a package (`modules/SessionLog.kt`), so a terminated session's history stays readable:

```
2026-10-14T17:40:02.118Z dummy #3 open  opened as session #3
2026-10-14T17:40:05.402Z dummy #3 run   "turn the lights on" -> lights:0.61, weather:0.22, greeting:0.17 (14 ms)
2026-10-14T17:41:10.009Z dummy #3 close closed after 1 runs
```

### Building a package

The **Packages** tab of the Models page reads a jar's classes and resources, the Java it needs (from the class
file version) and what built it (the manifest), so a jar can be checked before a script opens it. The shape of a
build that produces one:

```kotlin
plugins { kotlin("jvm") version "2.1.0" }
repositories { mavenCentral() }
kotlin { jvmToolchain(17) }
tasks.jar {
    archiveFileName = "echo-1.0.jar"
    manifest { attributes("Main-Class" to "org.example.Echo") }
}
```

```kotlin
package org.example

class Echo {
    fun run(text: String): List<String> = listOf("echo: $text")
    fun close() {}
}
```

The app runs on Java 21, so a jar compiled for Java 21 or older loads; the page says so next to the class file version.

Accepted data shapes: a `DataSource` from `source`, `List<Pair<text, label>>`, `Map<label, List<text>>`, or
rows of `[text, ..., label]`.

### What modelBuilder does

`buildClassifier` in `jetbrainDLBuilder/JetBuilder.kt`:

1. **checking data**: the preprocessor (`jetbrainDLBuilder/Preprocessor.kt`) cleans the rows, unifies label
   spellings, drops empty, junk, repeated and conflicting rows with a reason each, measures the set and refuses one
   a classifier cannot come out of; every line goes to the console and `metrics.json`. The reasons are listed in
   [Models and metrics](models.md#before-training-the-preprocessor).
2. **embedding**: every text goes through the BERT encoder once; the hidden states are mean-pooled into one vector each.
3. **training**: every candidate head (`Architecture.kt`; by default linear, `dense[16]`, `dense[32]`, `dense[64, 16]`,
   or what `buildOptions` names) is trained with Adam on plain Kotlin arrays (`DenseNet.kt`, any number of hidden
   layers, relu or tanh) on the same rows, scored on the same validation rows, and the best by macro F1 (or
   accuracy) is kept; the comparison is stored with the build.
4. **validating**: the chosen head on the related test file, or on a stratified 20% split; precision, recall, F1 per label.
5. **exporting**: the layers are written by hand as ONNX (opset 13) to `AMP/modules/<name>/model.onnx` with
   `module.json` (encoder, dims, labels) and `metrics.json` (one entry per build).

### Build progress

`ModelBuildProgress` (`jetbrainDLBuilder/BuildProgress.kt`) is a `StateFlow` the builder updates at every phase and
step: text *n* of *N* while embedding, epoch *n* of 150 while training, each validation sample, then export. Phases are
weighted (embedding 60%, training 30%, ...) into one fraction that drives:

- the **console strip** (`running 12s · modelBuilder 'intents' training 72/150 · epoch 72 loss 0.3102 accuracy 0.912 · 48%`, with a thin bar under it),
- the build line at the top of the Models page's list, with its thin bar.

Every 25 epochs the loss and accuracy are also logged to the console. Finished builds (with their duration and
outcome) stay in `ModelBuildProgress.history`; the console strip's tooltip names the last one while idle.

## Saved states

Signed in with Keycloak, a script can keep small JSON documents on the Amie backend, which stores them as
`uploads/<user>/state/<name>.json` next to your packages (your devices live there as `devices`). The calls block
like a package fetch and print one line each; a failure (a guest, an unreachable backend, a bad name) is one ERROR
line and `loadState` then returns null. A name matches `[A-Za-z0-9._-]{1,64}`, a document is at most 1 MiB.

```kotlin
saveState("labels", """{"intents": ["greet", "bye"]}""")   // PUT /state/labels
listStates().forEach { log(it) }                                 // devices, labels, ...
val labels = loadState("labels")                                 // the JSON text, null when there is none
```

## Serving a model

Open the Models page, expand a model, set host, port, path and methods, save, and switch **Listen for incoming data**
on. Then:

```bash
curl -X POST http://127.0.0.1:8765/models/intents/predict -d '{"text":"turn the lights on"}'
curl 'http://127.0.0.1:8765/models/intents/predict?text=turn+the+lights+on'
```

Every request is appended to `AMP/modules/<name>/incoming.jsonl` and counted on the Models page. Models that were
listening when the app closed start again on the next launch.
