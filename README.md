# amie pilot

A Kotlin Multiplatform workspace with a Compose Desktop app (`amieApplication/desktopApp`), an Android app
(`amieApplication/androidApp`) and shared code (`amieApplication/shared`). The desktop app is the main
product: a scripting sandbox that trains small intent classifiers on top of a BERT encoder, serves them over
HTTP, runs plugin jars, and manages connected devices. Sign-in goes through Keycloak.

Documentation is a Markdown site in [`docs/`](docs/index.md) built with [MkDocs](https://www.mkdocs.org/)
(`mkdocs.yml`): read it on GitHub, or run `mkdocs serve` and open http://127.0.0.1:8000. Pages:

| Topic | File |
|---|---|
| Overview and the app in one minute | [docs/index.md](docs/index.md) |
| Prerequisites, build, run, first start | [docs/getting-started.md](docs/getting-started.md) |
| Signing in once and staying signed in | [docs/sign-in.md](docs/sign-in.md) |
| The editor (completion, highlighting), running, the console and its filters | [docs/sandbox.md](docs/sandbox.md) |
| Every function a script can call, build phases | [docs/sandbox-scripting.md](docs/sandbox-scripting.md) |
| Building models, metrics tables and ASCII charts, publishing | [docs/models.md](docs/models.md) |
| Serving a model over HTTP | [docs/serving.md](docs/serving.md) |
| Device dashboard | [docs/devices.md](docs/devices.md) |
| What the app writes to `AMP/` | [docs/amp-folder.md](docs/amp-folder.md) |
| Keycloak realm and client setup | [docs/authentication.md](docs/authentication.md) |
| Amie backend and website: addresses, bearer tokens, publishing | [docs/backend-integration.md](docs/backend-integration.md) |
| Models page tiles | [docs/models-page.md](docs/models-page.md) |
| Troubleshooting | [docs/troubleshooting.md](docs/troubleshooting.md) |

## Prerequisites

- **JDK 21.** The desktop module pins a Java 21 toolchain (`amieApplication/desktopApp/build.gradle.kts`);
  Gradle's foojay resolver downloads one if it is missing.
- **Gradle wrapper.** Use `./gradlew` from the repository root. Gradle 9.3 with the configuration cache on.
- **Android SDK** only for the Android app. Put its path in `local.properties` (`sdk.dir=...`).
- **ONNX encoder** for building and running intent models: a BERT-style model folder with `model.onnx` and
  `tokenizer.json`. The default path is `/Users/samuel/Documents/onnx/bert/bge`
  (`DEFAULT_ENCODER` in `jetbrainDLBuilder/JetBuilder.kt`); the bundled NER model is expected in
  `/Users/samuel/Documents/onnx/bert`.
- **Keycloak** in Docker on `http://localhost:8080` for sign-in (see [docs/authentication.md](docs/authentication.md)).
  Without it the app still runs: the login page offers "Continue as guest".
- **Amie backend** (optional): the Spring Boot `packageRepository` from the Amie repository, used by the device
  and package screens and by **Publish** on the Models page. Its address and the website's are in
  `AMP/backend.json` (`AMIE_BACKEND_URL`, `AMIE_SITE_URL` override); see
  [docs/backend-integration.md](docs/backend-integration.md).

## Build and run

All commands run from the repository root.

```bash
# Desktop app
./gradlew :desktopApp:run                 # standard run
./gradlew :desktopApp:hotRun --auto       # hot reload while editing

# Compile only (fast check)
./gradlew :desktopApp:compileKotlin

# Native installer (DMG on macOS, MSI on Windows, DEB on Linux)
./gradlew :desktopApp:packageDistributionForCurrentOS
# output: amieApplication/desktopApp/build/compose/binaries/main/

# Android
./gradlew :androidApp:assembleDebug

# Tests
./gradlew :shared:jvmTest
./gradlew :shared:testAndroidHostTest
```

In IntelliJ or Android Studio, open the repository root and use the run configurations in the toolbar.

The desktop app stores everything it writes under `AMP/` in its working directory. With `./gradlew :desktopApp:run`
that is `amieApplication/desktopApp/AMP/`:

| Path | What it holds |
|---|---|
| `AMP/pluginDir/main.kts` | The script shown in the sandbox editor (saved as you type) |
| `AMP/pluginDir/*.jar` | Plugin jars |
| `AMP/modules/<name>/` | Exported models: `model.onnx`, `module.json`, `metrics.json`, `serving.json`, `incoming.jsonl` |
| `AMP/env/` | Script package environments and the Maven download cache |
| `AMP/keycloak.json` | Keycloak server, realm, client id and secret (git-ignored) |
| `AMP/session.json` | The signed-in session (offline token) so a restart signs you back in (git-ignored) |
| `AMP/componentSettings.json` | Configured devices |
| `AMP/logs.txt` | Everything the console showed |

## Using the app

1. **Welcome** → **Continue**. The first time you land on the login page; sign in with a Keycloak account, create
   one, or continue as a guest. The session is kept, so later starts go straight on.
2. **Sandbox** (the console icon in the left sidebar). Write a Kotlin script in `main.kts` and press the green
   **Run** button at the bottom of the sidebar. While it runs the button turns into a progress ring and the status
   line under the console header shows what is happening; `modelBuilder(...)` reports its phases there. The API
   is called without a prefix (`source`, `modelBuilder`, `metricsChart`, …) and the editor completes it.
   Details in [docs/sandbox.md](docs/sandbox.md) and [docs/sandbox-scripting.md](docs/sandbox-scripting.md).
3. **Models** (the second sidebar icon). A full page with the app's resource usage and every model's uptime,
   requests, latency and validation, plus serving settings, plugin jars and ORT sessions. Open to guests; only the
   backend card and Publish need a sign-in. Details in [docs/models.md](docs/models.md).
4. **Dashboard** (`home1/`): device cards with configure, manage, connect and disconnect screens; **+** adds a device.

## Project layout

```
amieApplication/
  desktopApp/src/main/kotlin/org/example/project/
    main.kt                         window, chrome, entry point
    auth/                           Keycloak client, JWT verification, session store, config
    components/render/templates/    pages: Welcome, Login, Register, AppBaseTemplate (navigation)
    components/ui/viewport/         window header, sidebar, device panels
    components/terminal/window/controller/
      CodingSandbox.kt              editor + console + Run button + run status bar
      ModelsPage.kt                 model statistics route
      ScriptLogger.kt               the API scripts call (also bound as `amie`)
      SandboxApi.kt                 the API list: prelude for prefix-free calls and editor completion
      TerminalWindow.kt             console, log levels, log file
      jetbrainDLBuilder/            modelBuilder: data checks, DenseNet training, BuildProgress
      modules/                      exported modules, metrics and their ASCII charts, ModelServer, SystemStats
      modelLoaders/                 ORT sessions and model loaders
      envBuilder/                   script package environments (Maven resolution)
      pluginEngine/                 plugin jar runner
      sourceFile/                   CSV data sources
    components/terminal/integrations/amiePilot/   BERT encoder, NER model, embeddings
  androidApp/                       Android entry point
  shared/                           common code and Compose resources
docs/                               this project's documentation
```

## Troubleshooting

- **"Keycloak is not reachable"** on the login page: check `docker ps`. The Keycloak container restarts in a loop
  when its Postgres container is down; `docker start <postgres container>` fixes it.
- **"Client 'amie' rejected"**: the client is confidential; put its secret in `AMP/keycloak.json` or
  `KEYCLOAK_CLIENT_SECRET`.
- **modelBuilder fails to embed**: the encoder folder is missing; see Prerequisites.
- **"The backend rejected the Keycloak token (401)"** on the Models page: the backend was started without the
  Keycloak verifier settings, or its `KEYCLOAK_ISSUER` doesn't match the realm's frontend URL. See
  [docs/backend-integration.md](docs/backend-integration.md).
- **"Connection refused: ensure the Amie backend is running"**: the address in `AMP/backend.json` has nothing
  listening. With Keycloak on 8080, run the backend with `AMIE_BACKEND_PORT=8090` and set `baseUrl` accordingly.
- **Port in use when signing in with the browser**: change `redirectPort` in `AMP/keycloak.json` and add
  `http://localhost:<port>/*` to the client's redirect URIs in Keycloak.
