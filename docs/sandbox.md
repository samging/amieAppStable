# The sandbox

The sandbox is the first screen after sign-in: a Kotlin script editor on the left, the output console on the right,
and the sidebar. It keeps to the app's quiet scheme: two thin strips (one over the editor, one over the console),
no borders, one filled button (**Run**), and tooltips on everything instead of words on the screen. The rule
between the editor and the console can be dragged to share the width; the split is kept while the app runs.

## The editor

- The strip over it shows the one file as a tab, `main.kts`, with **saved** beside it for a moment after each
  write, and **Run** at its right end (**Running…** while the script runs).
- The file is `AMP/pluginDir/main.kts`. It is saved as you type (a third of a second after the last keystroke).
- **Highlighting** in a calm palette: keywords a steel blue, strings sand, numbers gray-blue, comments gray, and
  the sandbox API functions (`source`, `modelBuilder`, `metricsChart`, …) in the one accent, a muted teal, so you
  can see at a glance what talks to the app.
- **Current line**: the line the caret is on sits in a faint box that runs across the gutter and the text, and its
  number in the gutter is bright.
- **Folding**: every `fun`, `class`, `object` or `interface` whose line ends with `{` has a small arrow in the
  gutter. Click it, or press **⌘-** (Ctrl+- elsewhere) with the caret anywhere in the block, and the block folds
  down to its first line: `fun greet(name: String) { … }`. Click the arrow or the `…`, or press **⌘=** (Ctrl+=),
  to open it again; with **Shift** the two shortcuts fold or open every block. Typing into a folded block, or
  moving the caret into it, opens it, so nothing ever happens out of sight. Folding changes nothing in the file:
  saving and **Run** always see the whole script, and a fold stays in place while you edit other lines.
- **Completion dropdown**: start typing a name and a list of matching API functions opens under the caret with the
  signature and a one-line description. **Up / Down** move, **Enter** or **Tab** insert the call and put the caret
  between the parentheses, **Esc** closes it, **Ctrl+Space** opens it on demand.
- The API is available **without a prefix**. `log("hi")`, `source("x.csv")` and `modelBuilder("name", data)` are the
  same as `amie.log(...)`, `amie.source(...)` and `amie.modelBuilder(...)`; both spellings work in the same script.
  Only `amie.import` has distinct names without the prefix: `implementation("group:module:version")` (or
  `dependency(...)`) for a package and `environment(SettingsEnv("name", "profile"))` for an environment, because
  `import(...)` without `amie.` reads as a Kotlin import statement. **Tab** in the editor inserts four spaces.

## Running

1. Press **Run** over the editor, press **⌘↵** (Ctrl+Enter elsewhere) inside it, or press the play button at the
   bottom of the sidebar; they are the same. A second press while it runs only logs "Script is already running".
2. The console strip reads `running 12s` with a moving bar under it; the sidebar's play button becomes a square
   that fills with the build's progress. While `modelBuilder` works the strip shows the phase and step
   (`modelBuilder 'x' embedding 40/120 · 43%`).
3. The strip ends on `finished in 41s` or `failed after 3s` (the error in its tooltip, and in the console); the
   console gets the same line.

What happens underneath: the script's dependency calls are read **before** it compiles and the packages are put on
the classpath (configuration phase); then the script runs with the Kotlin JSR-223 engine on a background thread
(execution phase). Details in [Scripting API](sandbox-scripting.md).

## Sessions: what stays open after a run

A script ends; what it opened does not. `initMod("name")` opens a **session** of a model (one per model, reused by
the next run), `initMod("name.jar")` a session of a package (a fresh instance per call), and both stay open until
they are closed in a script or terminated on the Models page, **Sessions** tab, where each one shows its runs and a
log of what it was asked and answered. So the usual loop is: open the model at the top of the script, ask it,
read the console, edit, Run again (instant, the model is already loaded), and close it when you are done.
All of it, with the pitfalls, is in [Running sessions](sessions.md).

## The output console

By default a line is just its message, in monospace so metric tables and ASCII charts keep their alignment, and
coloured by its level: an error in the muted red, a line the app itself wrote in a dimmer gray, the script's own
lines in the plain text colour. The console follows the newest line until you scroll up; scrolling back to the end
re-attaches it. While it is empty it says how to run the script.

| Level | Shown as | Used for |
|---|---|---|
| **internal** | dim gray text | Lines the app itself writes: build phases, metrics tables, charts, session messages |
| **error** | muted red text | Failures, Keycloak and backend errors, script exceptions |
| **data** | plain text | Data your script logs with `data(...)`, file dumps |
| **info** | plain text | `log(...)` and `info(...)` from the script, run start and end |

The strip over the console carries, at its right end, the four levels as toggles with their counts (`internal 12`,
`error 0`, …; click one to hide or show its lines, **all** turns them all on again), the **columns** switch that
adds the time, the level and the source (`main.kts`, `sandbox`, `internal`) in fixed columns before every line, as
the log file has them, and **Clear**, which empties the view. Nothing is lost: everything the console ever showed
is appended to `AMP/logs.txt`. When the console is narrow the toggles move under the status instead of squeezing it.

## Keyboard summary

| Keys | Action |
|---|---|
| ⌘↵ (Ctrl+Enter) | Run main.kts |
| ⌘- (Ctrl+-) | Fold the block the caret is in; with Shift, every block |
| ⌘= (Ctrl+=) | Open the folded block at the caret; with Shift, every block |
| Typing a name | Opens the completion dropdown |
| Up / Down | Move in the dropdown |
| Enter / Tab | Insert the selected function |
| Esc | Close the dropdown |
| Ctrl+Space | Open the dropdown |
