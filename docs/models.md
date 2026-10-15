# Models and metrics

A model is an intent classifier: a small dense network trained on BERT embeddings of your texts. Everything about
a model lives in `AMP/modules/<name>/`, so building, inspecting and serving one needs no account; only publishing
does. A fresh checkout starts with a fixed **dummy** model (labels `greeting`, `lights`, `weather`) that is written
when `AMP/modules` is empty, so `initMod("dummy")` and the Models page work before any training.

## Building one

From the Models page: **New model**, pick the csv (or **Sample data**, `AMP/sample-intents.csv`), Build. From
a script:

```kotlin
val train = source("intents.csv")        // columns: text,label (header names like text/label/intent are detected)
relate(train, "intents-test.csv")        // optional: validate on this file instead of a 20% split
val model = modelBuilder("intents", train)
model.modelStatus()                      // the validation table
model.modelChart()                       // bars, history and confusion matrix
```

`modelBuilder` runs the **preprocessor** over the rows, embeds every text once, trains **every candidate head**
on the same rows, keeps the best one by macro F1 on the validation rows, and exports it:

| File | Content |
|---|---|
| `model.onnx` | The trained layers, written as ONNX (opset 13) |
| `module.json` | Encoder path, input and output sizes, pooling, the label list, the data file it was trained on |
| `metrics.json` | **One entry per build**: accuracy, macro F1, per-label precision / recall / F1 / support, the confusion matrix, warnings, what it was validated on |
| `serving.json` | Host, port, path, methods and whether it listens |
| `incoming.jsonl` | Every request the served model received |

Accepted data shapes: a `DataSource` from `source(...)`, `List<Pair<text, label>>`, `Map<label, List<text>>`, or
rows of `[text, …, label]`.

### Before training: the preprocessor

Nothing is embedded before the rows are cleaned and measured. The preprocessor writes what it did to the console
(the **info** lines that begin with `modelBuilder:`), keeps the same lines with the model (`metrics.json`,
shown under **About → TRAINING** on the Models page), and stops the build when the set cannot give a usable
classifier. What it does, in order:

1. **Cleans every row.** Control characters go, runs of whitespace become one space, text and label are trimmed.
2. **Unifies label spellings.** `Lights` and `lights` are one label; the more frequent spelling wins and the console
   says `label 'Lights' read as 'lights'`.
3. **Drops rows that cannot be learned from**, each with its reason: an empty text, an empty label, a text with no
   letters or digits (`???`), a single character.
4. **Drops repeats and conflicts.** The same text twice under one label keeps one copy (`repeated row`). The same
   text under two labels keeps the label with the majority (`same text under another label, kept as 'lights'`);
   with no majority both rows go, because neither label can be right.
5. **Measures what is left**: rows per label, distinct texts per label, words per text and the spread of lengths.

A build stops, with the sentence below in red and the model not written, when:

| The console says | Why training would be pointless | What to do |
|---|---|---|
| `no labeled rows at all` | Nothing came out of the data shape | Return rows of `[text, label]`, pairs, or a map; check the csv's header names |
| `almost every row has its own label (… labels for … rows): the text and label columns are probably swapped` | A classifier learns a few labels from many texts, not the reverse | Swap the columns, or name them `text` and `label` |
| `label '…' is … characters long, which looks like a text` | Same as above, seen from the label's side | Same |
| `a classifier needs at least 2 labels` | With one label there is nothing to decide | Add rows of another label |
| `label 'x' has 3 rows after cleaning, at least 5 are needed` | Too few to train on **and** hold some out to validate | Add rows of that label, or drop the label |
| `label 'x' has only 1 distinct text; the rest differ only in case or punctuation` | The encoder sees one sentence; the head would memorise it, not the label | Write different sentences for that label |
| `nothing is left after cleaning` | Every row was empty, repeated or in conflict | Look at the dropped lines above it |
| `embeddings are nearly identical` (after embedding) | The encoder cannot tell the texts apart, so no head can tell the labels apart | Use texts that differ in more than a word |

Softer problems become **warnings**, stored with the metrics and printed, and training goes on: labels
imbalanced more than 10×, texts of under two words on average, texts longer than 1000 characters (the encoder
reads the first 512 tokens), many texts that differ only in case or punctuation, every text of the same length.

`buildOptions(preprocess = false)` skips the cleaning; only the hard checks of the old builder run then.

The dropped rows are written to `AMP/modules/<name>/dropped.csv` (`text,label,reason`), so they can be fixed in the
source file. A related test file is cleaned the same way, and a test row whose text also appears in the training
rows is left out of the validation (the console says how many), because it would be scored on what was memorised.

**Check before building.** `checkData(source("intents.csv"))` in a script runs the preprocessor alone and prints
the same report, ending with whether a build could start; on the Models page the **New model** form has
**Check data** next to Build, which shows the report under the fields.

### After validating: what the scores say

Once the chosen head is validated, a few more lines are added to the warnings (printed, and kept with the build):

| The console says | What it means |
|---|---|
| `the validation has only 7 rows; its scores are rough` | Each row moves accuracy by a seventh; add rows or a test file before trusting a number |
| `labels with fewer than 2 validation rows: weather` | That label's precision and recall come from one row |
| `the head fits the training rows much better than the validation rows (0.99 vs 0.80): it memorises` | Overfitting: more varied rows, or a smaller head (`architecture(hidden = emptyList())`), would generalise better |
| `accuracy 0.62 is barely above always answering 'lights' (0.58)` | The head learned little beyond the majority label; the texts may not separate the labels |
| `label 'weather' is weak (F1 0.40); it is mostly taken for 'lights' (4 of 6 rows)` | The confusion matrix's worst row in a sentence; those two labels need more distinct examples |

Two more options help with awkward data: `buildOptions(balanceClasses = true)` weights the loss so every label
counts the same, which lifts a rare label the head would otherwise learn last; `buildOptions(seed = 7)` changes the
split, the initial weights and the shuffles (the same data and seed always give the same model, so a comparison
between two runs is a comparison of the data or the options, not of luck).

### Which head: the architecture search

On top of the encoder's 768 numbers per text sits a small head. By default four are trained on the same
training rows and scored on the same validation rows, and the best macro F1 wins (ties: the better accuracy,
then the smaller head):

| Candidate | Head |
|---|---|
| `linear` | `dense[768 -> labels] softmax` |
| `dense[16]` | one hidden layer of 16 relu units |
| `dense[32]` | one of 32 |
| `dense[64, 16]` | two hidden layers |

As each head finishes the console shows its accuracy, macro F1, train accuracy, loss and parameter count, then
its F1, precision and recall per label, then what the diagnostics make of it (memorising, barely above the
majority label, a weak label and what it is taken for), and the Models page lists it under the build line at once.
Once every head is in, one line ranks them (`compared 4 heads by macro F1: dense[16] 0.950 · dense[32] 0.912 …`)
and `chosen: dense[16], the best macro F1 on the validation rows (next: dense[32] 0.912)` says which is kept.
The chosen head is then validated once more on every validation row (the VALIDATING phase), so what is stored,
charted and exported is that head's own run. The comparison is kept with the build in `metrics.json`
(`candidates`, with `perLabelF1` and `insights` per head; **HEADS COMPARED** on the Metrics tab) and in one line
of the manifest (`search`, shown as **heads tried** under About → TRAINING). Each candidate costs its epochs; on a
few hundred rows the whole search takes seconds.

### Training again

`modelBuilder("intents", train)` with the name of a model that exists trains it again in place, which is how a
model follows its data: add or fix rows in the csv and build under the same name. The sessions open on the old
head are closed (the next `initMod` opens the new one), a listener comes back on the new head, the description
and the guide are kept unless the build brings its own, and `metrics.json` keeps every earlier run, so the runs
show whether the new rows helped. The Models page's **Retrain** does the same.

### Your own head

```kotlin
val options = buildOptions(
    candidates = listOf(
        architecture(hidden = listOf(32, 16), epochs = 200, learningRate = 0.005),
        architecture(hidden = emptyList(), epochs = 100),              // a linear head
        architecture(hidden = listOf(64), activation = "Tanh"),
    ),
    pickBy = "accuracy",            // or "macroF1" (the default)
    validationFraction = 0.25,       // how much is held out when there is no related test file
)
val built = modelBuilder("intents", train, options)
```

`architecture(...)` takes the hidden layer widths (an empty list is a linear head), `epochs`, `batchSize`,
`learningRate` and the hidden activation (`Relu` or `Tanh`); a wrong value is refused when the line runs, not
after the embedding. One candidate means no comparison, just that head. The Models page's **New model** uses the
defaults.

## Validating without training

A run can be added to `metrics.json` without rebuilding: on the Models page, **Validate on file** or **Validate
on sample data** runs the model on every row of a csv and stores accuracy, F1 per label and the confusion
matrix as one more run, with the file name as the validation set (`modules/ModelValidation.kt`). This is how the
dummy model, which is written rather than trained, gets metrics at all.

## Reading the metrics

Metrics are plain local files, so anyone using the app can read them, from the console or from the Models page.

### In the console, as tables

```kotlin
validateMetrics("intents")                          // all labels
validateMetrics("intents", listOf("lights", "music")) // some labels
```

```
metrics 'intents' @ 2026-10-14T14:02:11Z: 240 samples (192 train / 48 validation on split 20%)
accuracy 0.917  macro F1 0.905
label   precision  recall  f1     support
lights  0.933      0.933   0.933  15
music   0.875      0.933   0.903  15
```

### In the console, as ASCII charts

```kotlin
metricsChart("intents")             // accuracy, macro F1 and the F1 of every label as bars
metricsChart("intents", listOf("lights"))
metricsHistory("intents")           // accuracy and macro F1 across every build, as a line chart
confusionMatrix("intents")          // the last validation's confusion matrix as a shaded grid
```

`metricsChart` draws one bar per row with precision, recall and support beside it:

```
metrics 'intents' · build 3 of 3 · 48 validation samples on split 20%
F1 per label (bar), precision p, recall r, support n
accuracy ███████████████████████████░░░ 0.917
macro F1 ███████████████████████████░░░ 0.905
lights   ████████████████████████████░░ 0.933  p 0.933  r 0.933  n 15
music    ███████████████████████████░░░ 0.903  p 0.875  r 0.933  n 15
weather  ██████████████████████████░░░░ 0.880  p 0.917  r 0.846  n 18
```

`metricsHistory` plots every build on one canvas (● accuracy, ○ macro F1, ◆ where they meet) with the build
numbers under the columns and a dated line per build, so you can see whether a new data file helped.

```
history 'intents' · 3 builds
1.00 ┤            ◆
     ┤      ●
     ┤      ○
0.50 ┤●
     ┤○
0.00 ┤
     └─────────────
      1     2     3
      ● accuracy   ○ macro F1   ◆ both
```

`confusionMatrix` prints truth in rows and prediction in columns; the shade of each cell is its share of the row
and ✓ marks the diagonal.

Each of these returns the report, so they chain: `metricsChart("intents").history().confusion()`. A model that
was never built logs an error telling you to run `modelBuilder` first.

### On the Models page

The **Models** page (second sidebar icon) draws the same numbers as charts on the model's **Metrics** tab: the
scores per label and the confusion matrix of every validation run, the scores across the runs, and from the
sessions how sure the model is and how long an answer takes, read from `metrics.json` and `sessions.log`. The
ASCII charts stay in the console. The page also builds a model from a csv (**New model**: name, a sentence about
it, data file, optional test file), and every model is a component with **Open session**, **Listen**, the serving
settings behind the gear, and **Publish** (Keycloak sign-in); every open session can be terminated there and its
log (what it was asked and answered) read. Details in [Models page](models-page.md).

## Using a model from a script

```kotlin
val m = initMod("intents")
log(m.predict("turn the lights on"))             // "lights"
log(m.inference("turn the lights on").first())   // "lights:0.97"
m.close()
```

`initMod()` with no name opens the bundled NER model; `importModelOnnx("path.onnx")` opens any ONNX model behind
the same `inference / getStatus / close` interface. Every `initMod("name")` is a session the Models page lists
and can terminate; what it was asked and answered is logged per session (behind **Log** on the page and in
`AMP/modules/<name>/sessions.log`). ORT sessions are also printed with `sessions.print()`.

## Publishing

Signed in with Keycloak, expand a model on the Models page and press **Publish**: the model goes up to your package folder on the Amie backend (`POST /upload`,
`progLanguage=onnx`) as `<name>.onnx`, `<name>.module.json` and `<name>.guide.md`, so two models never overwrite
each other, and the website's dashboard lists them. A guest pressing it is sent to the sign-in page. Requires the backend from
[Amie backend](backend-integration.md), which stores the files in the Amie S3 bucket. Package jars go the other way: a
script opens one the server lists with `initMod("name.jar")` (see [Packages](sandbox-scripting.md#packages)).
