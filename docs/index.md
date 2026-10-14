# Amie desktop app

Amie is a desktop workbench for small intent models. You write a Kotlin script in the **sandbox**, it trains a
classifier on top of a BERT encoder, the result lands in the `AMP/` folder next to the app, and from there you can
read its metrics, draw them as charts in the console, serve the model over HTTP, and open package jars from the Amie server.
Devices connected to the Amie backend get their own dashboard.

This site is plain Markdown in the repository's `docs/` folder, built with [MkDocs](https://www.mkdocs.org/)
(`mkdocs.yml` at the repository root). Read it on GitHub, or serve it:

```bash
mkdocs serve -a 127.0.0.1:8000      # live-reloading site at http://127.0.0.1:8000
mkdocs build                        # static site in build/docs-site/
```

## Where to go

| I want to… | Read |
|---|---|
| Build and run the app for the first time | [Getting started](getting-started.md) |
| Sign in once and stay signed in | [Signing in](sign-in.md) |
| Write and run a script, read the console | [The sandbox](sandbox.md) |
| Know every function a script can call | [Scripting API](sandbox-scripting.md) |
| Train a model and look at its metrics | [Models and metrics](models.md) |
| Put a model behind an HTTP endpoint | [Serving a model](serving.md) |
| Manage devices on the dashboard | [Devices](devices.md) |
| Know what the app writes to disk | [The AMP folder](amp-folder.md) |
| Fix something that does not work | [Troubleshooting](troubleshooting.md) |

## The app in one minute

1. Start Keycloak (optional, see [Signing in](sign-in.md)) and run `./gradlew :desktopApp:run`.
2. On the welcome screen press **Continue**. The first time you are asked to sign in, create an account, or continue
   as a guest. From then on the app remembers you and skips the welcome screen while the session is valid.
3. In the sandbox, type a script and press the green **Run** button:

```kotlin
val train = source("intents.csv")          // columns: text,label
val model = modelBuilder("intents", train)
metricsChart("intents")                    // F1 per label as bars
metricsHistory("intents")                  // accuracy and macro F1 over every build
```

4. Open **Models** (second sidebar icon): every model is a component to configure, serve, open and publish, and every open session can be terminated there.
