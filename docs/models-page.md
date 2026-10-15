# Models page

Route `models`, opened from the second icon in the sidebar, which the chip at the far left of the tab strip, right where
the sidebar meets it, hides and shows (the choice is kept in `AMP/settings.json`, as the sandbox's is). It is a full page (not a side panel) in
`components/terminal/window/controller/ModelsPage.kt`, and it re-reads its state while open. What it showed last
is kept for the life of the app (`ModelsCache`), so coming back to the page shows the models at once; the first
read after start-up is local only (files, sessions, listeners) and the one slow read, the server's jar listing,
is fetched right after and cached for a minute by `JarPackages`. Nothing on the page waits for the network.

The page keeps to one quiet scheme, the same grays as the sandbox, and everything on it is square: no rounded
corners anywhere, in the window frame, the sidebar, the header, the chips, the status marks or the charts. There
are three kinds of button and no borders on any of them: **one filled dark-blue button per place** for the action
that creates, builds, validates, opens or fetches (**New model**, **Build**, **Open session**, **Validate**,
**Add**, **Create**, **Fetch**, **Save**), a **dark-red** one for what ends or removes something (**Stop**,
**Terminate**, **Destroy**), and **bare text** for everything else, with a faint square under the pointer. The
chips that are on or off (methods, run numbers, console scope) are bare text too, lit in the tab strip's gray
when on. A muted green marks what is running. Nothing is vivid: the three tokens the rest of the app shares
(`AddButton`, `SuccessColor`, `DangerColor` in `ui/theme/Color.kt`) are muted too, so every screen calms down
with the page. On-screen words are kept short; the explanations live in the tooltips.

## Finding your way

- **Every button, tab and status word has a tooltip** (`components/ui/viewport/Tooltip.kt`, `AppTooltip`): rest
  the pointer on it for half a second and a square dark box says what it does, or, on a disabled button, what is
  still missing. The status words (`running`, `stopped`, `inferring`, `idle`, `2 open`, `on server`) say what they
  mean for that row; the RAM footer explains each of its numbers.
- **Nothing is reachable only by right-click.** Every row has a **⋯** button (shown while the pointer is over the
  row and on the selected row) and every detail panel header has one at its right end; both open the same square
  menu the right-click opens (`SquareContextMenu`), with the actions that end or remove something in red. Esc or a
  click elsewhere closes it.
- **Slow actions say so on their button**: *Opening…*, *Starting…*, *Stopping…*, *Publishing…*, *Fetching…*,
  *Saving…* (then *saved* for a moment), and the button is disabled until the action ends.
- **Notices are readable**: what an action did, or why it failed, appears in a strip under the tabs with the whole
  text (it wraps and can be selected), a bar at its left edge that is red for a failure, and **Dismiss**.
  Destroying a model asks first in the same kind of strip, with the full consequence spelled out and a red
  **Destroy** button.
- **Forms open in the panel, not above the page.** **New model** and **New collection** take the place of the
  detail panel on the right while they are open, so the list never moves; **Close** brings the selected item
  back. A build in progress is one line at the top of the list with a thin progress bar.
- **Empty states say the next step**: a filter that matches nothing names the query and offers **Clear**; the
  Sessions tab with nothing open says how a session is opened; the server group of the packages list says it is
  still asking the server; a model without a run says what the two Validate buttons do.

## Layout

Under the window header sits a tab strip like the sandbox's, with four tabs and their counts: **Models**,
**Packages** and **Sessions**, and nothing else. Signing in happens in the window header (**Log in**) and
is only needed for **Publish**; everything else works without an account. (The website is not opened from the
page any more; a publish notice names the address that lists what you published.) Each tab is a flat list on the left, a 1dp rule that can be dragged to resize the list, and one bordered
detail panel on the right for the selected item. The tab strip already names the section and counts its items,
so there is no title above the list: its first line is the filter box (its tooltip says what it matches) with
the one action of the list beside it (**New model**; **Add**, which offers a jar from this machine or a new
collection; **Terminate all** on the Sessions tab).

At the foot of the page sits the **RAM** strip: what the process holds of the machine's memory as a bar, the heap
against its `-Xmx` limit, the native memory, and the BERT encoders loaded (the heavy part, shared by every
session). It turns red near the limit.

## Models

The list has one line per module in `AMP/modules`: the name, its last accuracy (or `not validated`), `serving
:port` while it listens, its open sessions, the status word at the right and the ⋯ button; a running one is
underlined in green. Models are grouped by the data they were trained on (the csv name the builder records in
`module.json`; the dummy sits under *written, not trained*, older models under *unknown data*), so the same data
trained twice sits together. Groups with a running model come first; inside a group the running models come
first, then the most accurate. The filter box matches the name, a label, the status, the data and the file a
model was validated on. Clicking a line selects it; the panel on the right shows that model only, with the status
line (listening address, the data it was trained on) under its name and the actions at the right of the header:

| Action | What it does |
|---|---|
| **Open session / Terminate session** | The filled button. Loads the model (encoder and head) as a session, the same as `initMod("name")` in the sandbox, or closes it. A model has one session at most |
| **Listen on :port / Stop** | Starts or stops the HTTP listener (`modules/ModelServer.kt`) with the settings as shown; the label names the port it will use |
| **⚙** | Opens the serving settings under the header: **Host / Port / Path / Methods** and **Save** write `AMP/modules/<name>/serving.json`; a port outside 1–65535 is named and keeps Save disabled; the POST and GET URLs are listed under them with **Copy** and **Copy curl**. The gear stays lit while the block is open |
| **Publish** | Uploads the model as `<name>.onnx`, `<name>.module.json` and `<name>.guide.md` to the Amie backend (`POST /upload`, `progLanguage=onnx`) with the Keycloak token, then asks the website to refresh its listing and says in the notice strip whether `/dashboard?username=<you>` lists the files, naming that address; a guest is sent to the sign-in page. A backend that cannot be reached ("Host is down", "Connection refused") is an address problem in `AMP/backend.json`, not a sign-in one |
| **⋯** | The whole menu: Open session, Listen/Stop, Validate on sample data, Validate on file…, Retrain on `<data>.csv`, Retrain on file…, Rename…, Publish to the website, Remove from the website, Reveal folder, Copy `initMod("name")`, Terminate model, Destroy model… |

**Rename…** opens a strip at the top of the page with the current name in a field; **Rename** is enabled once the
new name is a module name (letters, digits, dots, dashes, underscores) no other model has. The folder under
`AMP/modules` moves, `module.json`, the guide's title and `modules.json` say the new name, the sessions on it are
closed, and a listener comes back under the new name (a serving path that was the old default becomes the new
default). A trained model can be renamed like any other; scripts then open it with `initMod("newname")`.

**Retrain** trains the model again under the same name: on the csv it was trained on as it is now (edit or add
rows to the file, then retrain), or on another csv (**Retrain on file…**, also **Other file…** on the Data tab).
Nothing that opens the model has to change: the head is rebuilt in place, the sessions on the old head are
closed so the next `initMod` opens the new one, a listener comes back on it, the description and the guide stay,
and the Metrics tab keeps every earlier run beside the new one, so the runs show whether the new data helped.
Only one build runs at a time; the line at the top of the list follows it as for a new model.

Under the header the panel has its own tabs, each with a tooltip that says what it holds:

| Tab | What it shows |
|---|---|
| **About** | The description (editable; **Save description** appears under it once it has changed and writes `module.json`), then the **WHERE** and **TRAINING** blocks. Every uppercase block title on these tabs is a line that folds its block away and back (open by default): where the model lives with **Reveal folder**, created / encoder / head / labels, and **TRAINING**: architecture, data, rows, epochs, batch size, learning rate, hidden units, final loss, train accuracy |
| **Guide** | `AMP/modules/<name>/guide.md`, the guide shipped inside the model, rendered as light markdown; **Edit** and **Save** change it, and it goes along when the model is published |
| **Metrics** | One line: the run chips (the latest first, each chip's tooltip names its data, rows, accuracy and time) and **Validate**, which offers **On the sample data** and **On a file…** and adds a run (`modules/ModelValidation.kt`). A run that came from a build has **HEADS COMPARED**: every head the build trained, with accuracy, macro F1, train accuracy and loss on the same validation rows, F1 per label under each, what the diagnostics said of it, and the chosen one marked. The chosen run's charts follow: scores per label, the confusion matrix, then scores by run and, from the sessions log, how sure the model is and how long an answer takes. The charts are PNG files drawn by `modules/MetricsImages.kt` into `AMP/modules/<name>/plots/` whenever the metrics or the log change, in one muted slate hue |
| **Data** | The csv files it was trained and validated on, each with rows per label and text lengths as bars, a folded preview and **Open in viewer**; the trained-on file has **Retrain** (the same name, on the file as it is now) and **Other file…**; then **RAN ON**: what its sessions answered and what the server served |
| **Requirements** | `modules/ModelRequirements.kt`: everything it needs to run, checked on this machine, each line marked met / not met / information (the mark's tooltip says which) |
| **Console** | The console lines that name the model, or with **all models** everything the models, packages and sessions logged; the box's height can be dragged |
| **Sessions** | The sessions it owns, each with **Log** and **Terminate** |

### Icons and groups

Every model has an icon: the chip by default, or one from a small catalog (lightbulb, cloud, chat, home, music,
shield, star, bolt, heart, language, science, work, pets, school, cart, hub, groups) chosen under **ICON** on the
About tab and kept in `module.json`. It shows in the list, on the panel header and in the groups that hold it.

A **group** is a named set of models that are asked together: a swarm. A model's ⋯ menu offers **Add to group
<name>** for every group it is not in and **New group with <model>…**, which opens a strip at the top of the page
asking for the name and the **env** the group belongs to. Groups live in the env, as packages do: each env keeps
its own in `AMP/env/<env>/groups.json`, a script sourced in that env finds them with `group("name")`
(the default env first, then any), and `internalEnv()` lists them with the packages. The list shows the groups in
a **groups** fold above the data folds, each with its icon, how many models it holds (and how many are missing),
its env and **open** or **closed**. Selecting one shows its panel: **Open swarm** / **Terminate swarm**, the
models (a row each; **Detach** takes one out; **Add model** adds one), the icon picker, where it lives and, while
open, the swarm's session with its log. **Remove group** leaves the models alone. A destroyed or renamed model is
taken out of (or renamed in) every group.

Groups are also made by **dragging**: take a model's row and drop it on a group's row to put it in that group, on
another model's row to open the new-group strip with the two of them, or on the zone that appears over the list
while you drag ("drop here for a new group") for a new group with that one. The ghost beside the pointer says what
the drop would do, and the row under it is lit. A click without movement still selects the row.

### New model

**Check data** next to **Build** runs the preprocessor on the chosen file without training and shows its report
under the fields: what it would clean, drop (with the reason) and refuse, and whether a build could start. The
reasons are explained in [Models and metrics](models.md#before-training-the-preprocessor).


**New model** beside the filter box opens the form in the place of the detail panel (it is open on its own when
there are no models): **Name**, **About** (a sentence kept in `module.json`), **Data** (Choose a csv with a text
column and a label column, or **Sample data** for `AMP/sample-intents.csv`, 45 rows over `greeting`, `lights` and
`weather`; the name is filled from the file name) and an optional **Test** file to validate on instead of a 20%
split. As soon as the data file is chosen the form reads it and shows how many samples and which labels it holds.
While **Build** is disabled the line beside it (and its tooltip) says what is still missing: a data file, a name,
or a csv without the two columns. **Build** runs the same `buildClassifier` the sandbox's `modelBuilder` runs, on
a background thread; while it runs one line at the top of the list shows the phase, step and a thin progress bar,
and under it every head scored so far (architecture, accuracy, macro F1, train accuracy, loss, the chosen one
marked once all are compared; the tooltip of a line has its F1 per label), so the comparison is visible while the
next head trains. When it ends the new model is selected. Only one build runs at a time. **Close** brings the
selected model back.

A csv chosen from anywhere else on the machine (**Data**, **Test**, **Validate on file…**, **Retrain on file…**)
is copied into `AMP/` first (a newer copy replaces the one there), so the model finds its data again by its name
later: the Data tab, the list's grouping and a retrain all look the file up in `AMP/`. A file already under `AMP/`
is used where it is.

A fresh checkout has one component already: the **dummy** model (`modules/AmpModules.kt`, `ensureDummyModule`),
a fixed one-layer head over the encoder with the labels `greeting`, `lights`, `weather`. It is written when
`AMP/modules` is empty, so the page always has something to configure and `initMod("dummy")` works before any
training. The sample csv (`ensureSampleData`) has the same labels, so the first thing to try is **Validate on
sample data** on the dummy (its weights are fixed, so expect poor numbers) and then **+ New model** with
**Sample data** to see what a trained model's metrics look like.

## Packages

The list has one line per package jar a script can open, first under the **collections** and then under three
folding groups by place: **In AMP/pluginDir**, **In the env** and **On the server** (the website's `/api/jars`,
else the S3 service's `/api/aws/jars`; folded by default, and a line says so while the server is still being
asked). Each line shows the Java the jar needs and its version, and at the right `idle`, `2 open`, `inferring` or
`on server` (the tooltip says what that means) and the ⋯ button; an open one is underlined in green. The filter
box matches the jar name, version, Java, a collection name, the class a session opens on, and any class or
resource name inside the jar, so a class can be found without knowing which jar holds it. **Add** beside the
filter box offers **Jar from this machine…** (copied into `AMP/pluginDir` and imported into the env), **From
Maven Central…** and **New collection…**.

### What a package needs

A package jar's panel has three blocks the libraries do not: **REQUIREMENTS**, every line marked met, not met or
information: the Java it needs against the Java this app runs, its package class, whether it ships metadata,
its config folder, each config file and environment variable it declares, and what its last test run came to;
**CONFIG**, the folder `AMP/pluginDir/config/<jar>/` with **Reveal folder**, where the package's files are read
from (as resources, through the `amie.package.config` property and a `configure(...)` method; templates for the
missing files are written on the first open) and the declared files with their state; and **TESTS**, the test
runs the jar declares and the last run's answers, each marked pass or fail. **Test run** in the header (also in
the ⋯ menu) opens a fresh session with the configuration in place, runs the declared tests (or one call with
"hello" when there are none), closes it and reports in the notice strip and the block, where every test shows
what it asked (`run("…")`) and the whole answer line by line, with the reason when it failed.

Under the header the jar's **flags** say what it works with at a glance, as coloured tags: its version (blue),
the Java it needs (slate, red when this app is too old for it), its language (violet) and how its last test run
went (green or red), then the jar's own flags from its metadata (`flags: [{"text": "stable", "color": "green"}]`)
and the ones you add with **Add flag** (also in the ⋯ menu): a word and one of six colours (slate, green, amber,
red, violet, blue), kept in the sidecar metadata beside the jar and removable from the ⋯ menu. The same flags
show on the jar's row in the list. A jar with no metadata
gets **Write metadata template** in its ⋯ menu, which writes `AMP/pluginDir/<jar>.package.json` to fill in. The
format is in [Sandbox scripting](sandbox-scripting.md#what-a-jar-needs-metadata-configuration-and-test-runs).

### From Maven Central

**Add → From Maven Central…** opens a form in the place of the detail panel with one box. Type a name (`gson`,
`okhttp`), a class name, `group:artifact` for the versions of one artifact, or paste a full
`group:artifact:version` as in a Gradle dependency; Maven Central is asked as you type and the hits are ordered
here, the artifact named like what you typed and the well-known groups first (the server's own order buries
them). Every hit is a row with
its newest version and publication date and its own **Add**, which resolves that version and its dependencies
into the env (`AMP/env/<env>/env.lock.json`, files under `AMP/env/caches`), exactly what
`implementation("group:artifact:version")` in a script does. A pasted coordinate is the first row. A thin bar
shows while the library is being resolved, and the form closes itself when it is done: the library then sits
under **From Maven Central** in the list, selected, with the same SOURCE, BUILD and CONTENTS sections as any jar
(the coordinate is in SOURCE), a **Remove** button that takes it out of the env (the files stay cached), and a
menu that copies the `implementation("…")` line. A script can import its classes from the next run without
declaring it. These are libraries, not packages: nothing opens them with `initMod`.

### Collections

A collection is a folder of jars that belong together, kept in `AMP/pluginDir/collections.json`
(`modules/PackageCollections.kt`), typically the jars that run on one Java: "jvm21" with every plugin that fits.
**Add → New collection…** opens the form in the place of the detail panel: a name, the jars to put in (toggles),
and a **By Java** shortcut per Java version found among the jars (`jvm17`, `jvm21`, …) that names the collection
and picks every jar that runs on that Java or older. Each collection is a fold at the top of the list with its
jars as rows; moving the pointer over a row shows **Detach**, **Add jar** lists the jars that are not in it yet,
and **Delete folder** removes the collection but keeps the jars. A jar can be in several collections, and its panel header says which.

The detail panel describes the selected jar without running anything (`pluginEngine/JarInspector.kt`, read once
per file version). Its header has the status line (status, `java 17+`, `kotlin` or `java`, version), **Fetch**
(filled) for a jar still on the server or **Publish** for a jar on this machine (uploads it to the website under
your account; the server refuses a jar without `run(String): List` and `close()`, with the reasons in the
notice), and the ⋯ menu with the same plus **Open its page on the website**, **Reveal file** and Copy
`initMod("name.jar")`. Under it the `initMod("name.jar")` call, then folding
sections, closed until opened:

| Section | What it shows |
|---|---|
| **SOURCE** | where it is, the website page, the exact download URL, when it was fetched and published (from `<jar>.source.json`), the file |
| **BUILD** | what built it (the manifest's `Created-By`, `Build-Jdk-Spec`), the class file version and the Java it needs, whether this app's Java can load it, language, size |
| **CONTENTS** | the class a session opens on (the one with `run(String)` and `close()`) and the `Main-Class`; a filter box; **CLASSES** grouped by package and **RESOURCES** grouped by folder, each group folding; the entry class and main class are marked; clicking a line copies its full name |

Then **SESSIONS**: the sessions open on it, each with **Log** and **Terminate**. There are no Run or Quit
buttons: a package is a bottle, not something the page controls. A script opens its own instance with
`initMod("name.jar")`, calls `inference(text)` and `close()`, and a jar that is only on the server is fetched into
`AMP/pluginDir` on first open (see [Packages](sandbox-scripting.md#packages)).

## Sessions

(What a session is and how to use one from a script is in [Running sessions](sessions.md); this is the tab.)

The tab lists everything that is open as flat rows named after the owner (`dummy  #3`, `echo-1.0.jar  #1`) with
what it is (intent model, package, ONNX session), idle or inferring, how many runs it has done and for how long
it has been open; with more than a few, a filter box matches owner, kind or `#id`, and **Terminate all** sits
at the right of that first line. **Log** unfolds what the
session was asked and answered: one line per `inference` call (`"turn the lights on" -> lights:0.97,
weather:0.02 (12 ms)`), plus open, close and error lines, with the time of each. The same lines are appended to
the owner's file, `AMP/modules/<name>/sessions.log` for a model and `AMP/pluginDir/<jar>.sessions.log` for a
package, so the history of a terminated session is not lost (`modules/SessionLog.kt`). **Terminate** ends a
session: intent model sessions (from **Open session** or `initMod("name")`; `modules/ModelSessions.kt`), package
sessions (`pluginEngine/JarPackages.kt`; terminating calls the package's `close()` and drops its class loader)
and ONNX Runtime sessions opened on a bare file with `importModelOnnx` (`modelLoaders/OrtSessions.kt`;
terminating cancels in-flight runs; these keep no log). The ORT session inside a model session is not listed
twice; it goes with the model session. **Terminate all** ends all of them. Right-click a row, or its ⋯ button,
for Terminate, Show log, Copy log and Copy `#id`.

