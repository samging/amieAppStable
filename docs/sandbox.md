# The sandbox

The sandbox is the first screen after sign-in: a Kotlin script editor on the left, the output console on the right,
and the sidebar. It keeps to the app's quiet scheme: two thin strips (one over the editor, one over the console),
no borders, one filled button (**Run**), and tooltips on everything instead of words on the screen. The rule
between the editor and the console can be dragged to share the width; the split is kept while the app runs.

Both side panes can be put away with the two icon chips left of **Run**. The navigation sidebar is hidden or
shown by its chip, and that choice is kept in `AMP/settings.json`. The console follows the output instead: it
starts hidden, since there is nothing to show, and opens by itself at the first line a run logs (one the level
toggles let through). Its chip hides it at any time, output or not; what it holds is kept, it stays hidden for
the rest of that run, and the first line of the next run (which empties it first) brings it back. With the console hidden the editor takes the whole width and the run's status (`ready`,
`running 12s`, `finished in 41s`, `failed after 3s` with the error in its tooltip) moves up next to the file tab.

## The editor

Indentation is drawn: a thin vertical line stands at every four leading spaces, so the lines of a block are joined
down its left edge, across its empty lines too; the guide of the block the caret is in is lit. The editor keeps
the indentation for you: **Enter** starts the new line at the current line's depth, one level deeper after a `{`,
and between `{` and `}` it puts the closing brace on its own line below the caret. **Tab** inserts four spaces,
or indents every line of a selection; **Shift+Tab** moves them back. **Format** on the strip (or ⌘⇧F, Ctrl+Shift+F
elsewhere) re-indents the whole script by its braces and parentheses, continuing lines (`.map`, `?.let`, `?:`)
one level in, and leaves the inside of strings and comments alone; the caret stays where it was in its line.

- There is one script and it is always `AMP/pluginDir/main.kts`, so the strip over the editor shows no file tab.
  It says instead which **env** the script is sourced in (`default` unless the text says
  `environment(SettingsEnv("name"))`), with **saved** beside it for a moment after each write, and the green
  **Run** at its right end (**Running…** while the script runs).
- The file is saved as you type (a third of a second after the last keystroke). You can edit
  `AMP/pluginDir/main.kts` with anything else too; the editor loads it at start.
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

**Examples**, on the strip over the editor, is the fastest way in: a menu of complete scripts (ask the dummy
model, a confidence gate, train from a csv, read metrics, named entities, embeddings, sessions, Postgres, serve a
model and call it, what to do when something goes wrong). Picking one puts it in the editor, ready for **Run**;
the script that was there is kept under `AMP/pluginDir/backup/` and **Bring back my previous script** restores
it. None of them needs an account or a server. The longer versions, with the explanations, are in the
[Cookbook](cookbook.md).

## Running

1. Press **Run** over the editor or press **⌘↵** (Ctrl+Enter elsewhere) inside it; they are the same. A second
   press while it runs only logs "Script is already running".
2. The console strip reads `running 12s` with a moving bar under it and the Run button reads *Running…*. While
   `modelBuilder` works the strip shows the phase and step (`modelBuilder 'x' embedding 40/120 · 43%`).
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

When the app starts only **info** is on, so the console shows the script's own `log(...)` lines and the start and
end of each run. The counts on the other toggles still move: `error 1` after a failed run means one error line is
waiting behind the toggle, and the strip's status says `failed after 3s` with the error in its tooltip either way.

## Keyboard summary

| Keys | Action |
|---|---|
| ⌘↵ (Ctrl+Enter) | Run main.kts |
| Enter | New line at the current indentation; deeper after `{` |
| Tab / Shift+Tab | Indent or dedent the line, or every line of the selection |
| ⌘⇧F (Ctrl+Shift+F) | Format: re-indent the whole script |
| ⌘- (Ctrl+-) | Fold the block the caret is in; with Shift, every block |
| ⌘= (Ctrl+=) | Open the folded block at the caret; with Shift, every block |
| Typing a name | Opens the completion dropdown |
| Up / Down | Move in the dropdown |
| Enter / Tab | Insert the selected function |
| Esc | Close the dropdown |
| Ctrl+Space | Open the dropdown |
