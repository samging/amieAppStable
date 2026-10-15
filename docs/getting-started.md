# Getting started

## What you need

| Requirement | Why | Notes |
|---|---|---|
| JDK 21 | The desktop module pins a Java 21 toolchain | Gradle's foojay resolver downloads one if it is missing |
| Gradle wrapper | `./gradlew` from the repository root | Gradle 9.3, configuration cache on |
| BERT models | `modelBuilder` and `initMod` embed text with the `bge` encoder; `initMod()` runs the NER model | The NER folder (`model.onnx`, `tokenizer.json`) with `bge/` inside it: `/Users/samuel/Documents/onnx/bert` for a Gradle run, or `AMIE_MODELS_DIR` (`BundledModels.kt`). The packaged app ships them in `app/resources/models/bert/` |
| Keycloak (optional) | Sign-in, the Models page's backend card, publishing | Docker on `http://localhost:8080`; without it choose **Continue as guest** |
| Amie backend (optional) | Devices, packages, **Publish** | Address in `AMP/backend.json`; see [Amie backend](backend-integration.md) |
| Android SDK (optional) | Only for the Android app | `sdk.dir=...` in `local.properties` |

## Build and run

All commands run from the repository root.

```bash
./gradlew :desktopApp:run                 # run the desktop app
./gradlew :desktopApp:hotRun --auto       # hot reload while editing
./gradlew :desktopApp:compileKotlin       # compile only (fast check)
./gradlew :desktopApp:packageDistributionForCurrentOS   # DMG / MSI / DEB under amieApplication/desktopApp/build/compose/binaries/main/, BERT models included
```

In IntelliJ or Android Studio open the repository root and use the run configurations in the toolbar.

## Running the tests

```bash
./gradlew :desktopApp:test
```

The desktop module's tests need no services and never touch your `AMP/` folder: the test task points `AMIE_AMP_DIR`
at `build/test-amp` and the backend, site and Keycloak addresses at fake hosts, and the HTTP clients under test
get a mock engine. They cover the editor (folding, formatting), the prediction helpers, the Postgres driver and
config, every script of the cookbook and the Examples menu (compiled through the sandbox's assembler), the
Keycloak client (password grant, refresh, logout, what each failure says, issuer, signature and expiry checks on
the token), the backend client (profile, upload, publishing, the SSO handoff, saved states, error reports, an
unreachable backend) and a model session over a Mockito model. The services themselves are tested in the Amie
repository (`README.md` there, "Testing"): unit tests for the backend and the S3 service, and
`scripts/e2e-auth-publish.py`, which runs sign-in, registration, publishing and SSO against the running stack.

## First start

1. The window stays blank for a moment while the app checks `AMP/session.json`. When a valid session comes back
   you land on the **dashboard** straight away; otherwise the **Welcome** screen opens.
2. Press **Start**. No account is needed: the sandbox, the models and the metrics work on this machine. The small
   link under it, *Sign in or create an account*, only matters for publishing and devices; see [Signing in](sign-in.md).
3. You arrive in the **sandbox** with `main.kts` open. Open **Examples** on the strip over the editor and pick
   *Ask the dummy model*, then press **Run**. Everything the app writes goes into `AMP/` next to its working
   directory (`amieApplication/desktopApp/AMP/` when started with Gradle); see [The AMP folder](amp-folder.md).

## The three screens

- **Sandbox** (console icon in the left sidebar): the editor with **Run** over it, and the output console.
  [The sandbox](sandbox.md).
- **Models** (second sidebar icon): every intent model with its metrics, data, requirements, console and
  sessions; the package jars and their collections; and every open session. Open to everyone; only
  **Publish** needs a sign-in. [Models page](models-page.md), [Models and metrics](models.md).
- **Dashboard** (first sidebar icon): device cards with configure, manage, connect and disconnect screens.
  [Devices](devices.md).

The window header shows who is signed in with a **Log out** action; a guest sees **Log in** instead.
