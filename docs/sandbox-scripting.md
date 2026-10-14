# Sandbox scripting

The sandbox is a Kotlin script editor (`main.kts`) with a console. The script runs inside the app with an
`amie` object bound to it; everything it logs shows in the console on the right and is appended to `AMP/logs.txt`.

## Running a script

1. Type in the editor. The script is saved to `AMP/pluginDir/main.kts` as you type.
2. Press the green **Run** button at the bottom of the left sidebar.
3. While the script runs the button becomes a spinning ring (it fills with the model build's progress when
   `amie.modelBuilder` is working), the status line under the console header reads `Running main.kts · 12s`,
   and pressing Run again only logs "Script is already running".
4. When it ends the status line shows `Finished in 41s` or `Failed after 3s`, and the console gets the same line.

The console chips (INTERNAL, ERROR, DATA, INFO) filter what is shown; **Clear** empties the view, the log file keeps it all.

### Run phases

- **Configuration.** `amie.import("group:module:version")` and `amie.import(SettingsEnv("env", "profile"))` calls are
  read from the script text *before* it compiles, the packages are resolved from Maven Central (cached under
  `AMP/env/caches`) and put on the script's classpath. Each env (`AMP/env/<name>/env.lock.json`) keeps what was ever
  imported into it, like a virtualenv.
- **Execution.** The script is wrapped with `val amie = bindings["logger"] as ScriptLogger` and evaluated with the
  Kotlin JSR-223 engine on a background thread.

## The `amie` API

Defined in `ScriptLogger.kt`. The most used calls:

| Call | What it does |
|---|---|
| `amie.log("...")`, `amie.info`, `amie.error`, `amie.data` | Console lines at the given level |
| `amie.source("train.csv")` | Reads a labeled CSV (text and label columns are inferred from headers like `text`, `label`, `intent`). Looked up as given, then in `AMP/`, then `AMP/pluginDir/` |
| `amie.relate(train, "test.csv")` | Attaches a test file to a training source; the model validates on it instead of a split. Remembered in `AMP/relations.json` |
| `amie.sourceFile("file.csv", column = "")` | Raw rows keyed by index, optionally one column |
| `amie.modelBuilder("name", data)` | Trains and exports an intent classifier (below) |
| `amie.modelBuilder("name") { ... }` | Same, the lambda returns the data |
| `amie.initMod("name")` | Opens an exported model; `.inference("text")` returns `label:probability` sorted best first; `.close()` |
| `amie.initMod()` | The bundled BERT NER model |
| `amie.validateMetrics("name", labels = listOf(...))` | Prints the stored validation table of a model |
| `amie.forwardContext("text") { ids -> }` | Runs the encoder and returns the hidden state `[batch][token][hidden]` |
| `amie.importModelOnnx("path.onnx")` | Any ONNX model behind the `inference / getStatus / close` interface |
| `amie.sessions.print()`, `amie.sessions.cancel("name")`, `amie.sessions.cancelAll()` | The open ORT sessions |
| `amie.jars.run("plugin.jar")`, `amie.jars.quit("plugin.jar")` | Plugin jars from `AMP/pluginDir` or the env |
| `amie.import("group:module:version")` | Add a package to the script's environment |
| `amie.import("my.jar")` | Add a jar file to the environment |
| `amie.internalEnv()` | Print what the environment contains |

## Building a model

```kotlin
val train = amie.source("intents.csv")          // columns: text,label
amie.relate(train, "intents-test.csv")          // optional test file
val model = amie.modelBuilder("intents", train)
model.modelStatus()                             // the stored validation report

val m = amie.initMod("intents")
amie.log(m.inference("turn the lights on").first())
m.close()
```

Accepted data shapes: a `DataSource` from `amie.source`, `List<Pair<text, label>>`, `Map<label, List<text>>`, or
rows of `[text, ..., label]`.

### What modelBuilder does

`buildClassifier` in `jetbrainDLBuilder/JetBuilder.kt`:

1. **checking data**: at least 2 labels, 5 samples per label; imbalance, duplicate and conflicting texts become
   warnings stored with the metrics.
2. **embedding**: every text goes through the BERT encoder; the hidden states are mean-pooled into one vector each.
3. **training**: a two-layer network (`dense[16 relu] -> dense[labels] softmax`) trained with Adam for 150 epochs on
   plain Kotlin arrays (no native TensorFlow needed).
4. **validating**: on the related test file, or on a stratified 20% split; precision, recall, F1 per label.
5. **exporting**: the layers are written by hand as ONNX (opset 13) to `AMP/modules/<name>/model.onnx` with
   `module.json` (encoder, dims, labels) and `metrics.json` (one entry per build).

### Build progress

`ModelBuildProgress` (`jetbrainDLBuilder/BuildProgress.kt`) is a `StateFlow` the builder updates at every phase and
step: text *n* of *N* while embedding, epoch *n* of 150 while training, each validation sample, then export. Phases are
weighted (embedding 60%, training 30%, ...) into one fraction that drives:

- the **Run** button ring in the sandbox sidebar,
- the **status line** under the console header (`modelBuilder 'intents' · training 72/150 · epoch 72 loss 0.3102 accuracy 0.912` with a bar),
- the **BUILDING** card on the Models page.

Every 25 epochs the loss and accuracy are also logged to the console. Finished builds (with their duration and
outcome) stay in `ModelBuildProgress.history`; the status line shows the last one while idle.

## Serving a model

Open the Models page, expand a model, set host, port, path and methods, save, and switch **Listen for incoming data**
on. Then:

```bash
curl -X POST http://127.0.0.1:8765/models/intents/predict -d '{"text":"turn the lights on"}'
curl 'http://127.0.0.1:8765/models/intents/predict?text=turn+the+lights+on'
```

Every request is appended to `AMP/modules/<name>/incoming.jsonl` and counted on the Models page. Models that were
listening when the app closed start again on the next launch.
