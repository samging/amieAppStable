# Models page

Route `models`, opened from the second icon in the sandbox sidebar. It is a full page (not a side panel) in
`components/terminal/window/controller/ModelsPage.kt`, and it re-reads everything every 1.5 seconds while open.

Everything on it is read from local files and in-process counters, so it is open to guests too: the header says
"signed in as …" or "guest · read from AMP/modules". Only the **AMIE BACKEND** card and the **Publish** button need a
Keycloak sign-in; a guest sees a **Sign in** button in their place. How to read the metrics, in the console and as
charts, is in [Models and metrics](models.md).

## Overview tiles

| Tile | Source |
|---|---|
| App uptime | JVM `RuntimeMXBean.uptime` |
| Models | exported modules, how many are listening |
| Requests | sum of served requests, with the error count |
| ORT sessions | open ONNX Runtime sessions, total runs and time spent inside the native runtime |
| Heap | used / max heap with a bar that turns red above 85% |
| CPU | process CPU load, system load and core count (`com.sun.management.OperatingSystemMXBean`) |
| Threads | live JVM threads and non-heap memory |

Collected by `modules/SystemStats.kt`.

## Per model

Each intent model card shows: listening state and uptime, requests, errors, average and maximum inference latency,
last request time, open sessions, session runs, time in the native runtime and average run, incoming rows recorded,
file size, accuracy, macro F1 and the number of builds. Expanding the card adds labels, encoder, validation details,
creation time, the last build's outcome, and the serving settings:

- **Host / Port / Path / Methods** and **Save** write `AMP/modules/<name>/serving.json`.
- **Listen for incoming data** starts or stops the HTTP listener (`modules/ModelServer.kt`). The POST and GET
  URLs are shown and selectable.

Serving statistics (uptime, requests, errors, latency) are kept per listener in memory from the moment it starts;
session statistics come from `modelLoaders/OrtSessions.kt`, which times every run.

## Building

While `modelBuilder` runs in the sandbox, a **BUILDING** card shows the model's phase, step and progress bar,
from the same `ModelBuildProgress` state the sandbox uses (see [sandbox-scripting.md](sandbox-scripting.md)).

## Jars and sessions

**JAR PLUGINS** lists jars in `AMP/pluginDir` and the active env, with Run / Quit and an **+ Add jar** picker that
copies the file in and imports it into the env. **ORT SESSIONS** lists every open session with its runs, native
time and how long it has been open; Cancel terminates in-flight runs and closes it.
