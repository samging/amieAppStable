# The AMP folder

Everything the app writes lives in `AMP/`. Started with `./gradlew :desktopApp:run` that is `AMP/` inside the
working directory (`amieApplication/desktopApp/AMP/`). The packaged app (DMG, MSI, DEB) has no useful working
directory, so it keeps AMP in the user's application-data folder: `~/Library/Application Support/amie pilot/AMP` on
macOS, `%APPDATA%\amie pilot\AMP` on Windows, `~/.local/share/amie-pilot/AMP` on Linux. On its first start that
folder is seeded from the `AMP/` shipped inside the app (`backend.json`, `componentSettings.json`, `pluginDir/`,
`env/`, `sample-intents.csv`); the dummy model is written next. `AMIE_AMP_DIR` names another folder for either mode.
Nothing is kept anywhere else on the machine, so copying this folder moves your scripts, models, metrics, plugins
and sign-in to another checkout or machine.

| Path | What it holds | Tracked in git |
|---|---|---|
| `pluginDir/main.kts` | The sandbox script, saved as you type | yes |
| `pluginDir/*.jar` | Package jars, opened by scripts with `initMod("x.jar")`; jars fetched from the Amie server land here | yes |
| `pluginDir/*.jar.source.json` | Where a fetched jar came from: download URL, website page, version, fetch time | yes |
| `pluginDir/collections.json` | The package collections: name and member jars | yes |
| `pluginDir/*.jar.package.json` | Metadata written beside a jar that ships none (or over the jar's own): config files, env, Java, entry class, tests | yes |
| `pluginDir/*.jar.tests.json` | The last test run of that package: each test's input, answer, pass/fail | yes |
| `pluginDir/config/<jar>/` | The configuration files a package reads: its templates are written here, the package resolves them as resources | yes |
| `env/<env>/groups.json` | The model groups of that env: name, models, icon | yes |
| `env/<env>/<group>.sessions.log` | What the swarm of that group was asked and answered | yes |
| `pluginDir/*.jar.sessions.log` | What every session of that package was asked and answered (open, run, error, close lines) | yes |
| `modules/<name>/model.onnx` | An exported model | yes |
| `modules/<name>/module.json` | Its manifest: encoder, dimensions, labels | yes |
| `modules/<name>/metrics.json` | One validation entry per build (tables and charts read this) | yes |
| `modules/<name>/serving.json` | Host, port, path, methods, listening | yes |
| `modules/<name>/incoming.jsonl` | Requests the served model received | yes |
| `modules/<name>/plots/*.png` | The metrics plots (summary, labels, confusion, history), drawn again when `metrics.json` changes | yes |
| `modules/<name>/sessions.log` | What every session of that model was asked and answered (open, run, error, close lines) | yes |
| `env/<name>/env.lock.json` | What a script environment has imported | yes |
| `env/caches/` | Maven downloads | yes |
| `sample-intents.csv` | 45 `text,label` rows over greeting / lights / weather, written on first start for trying New model and Validate | yes |
| `relations.json` | Which test file belongs to which training file (`relate`) | yes |
| `componentSettings.json` | Configured devices | yes |
| `databases.json` | The Postgres connections a script opens with `database("name")`: url, user, password (`$ENV:NAME` reads an environment variable), written with a `main` entry on first use | **no** |
| `backend.json` | Amie backend, website and S3 service addresses | yes |
| `keycloak.json` | Keycloak server, realm, client id and **secret** | **no** |
| `session.json` | The signed-in session (access, refresh and id tokens) | **no** |
| `logs.txt` | Everything the console showed, every level | yes |

## Configuration files

`keycloak.json` is written with defaults on first start:

```json
{
  "baseUrl": "http://localhost:8080",
  "realm": "master",
  "clientId": "amie",
  "clientSecret": "<the amie client's secret>",
  "redirectPort": 48080
}
```

Environment variables `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET` override it.

`backend.json` names the Amie backend, the website and the S3 service that keeps package jars; `AMIE_BACKEND_URL`,
`AMIE_SITE_URL` and `AMIE_S3_URL` override it. With
Keycloak on port 8080, run the backend on another port (`AMIE_BACKEND_PORT=8090`) and set `baseUrl` here.

## Metrics format

`metrics.json` is a list of runs; the last one is the current model:

```json
{
  "runs": [
    {
      "createdAt": "2026-10-14T14:02:11Z",
      "samples": 240, "trainSize": 192, "validationSize": 48,
      "accuracy": 0.917, "macroF1": 0.905,
      "labels": { "lights": { "precision": 0.933, "recall": 0.933, "f1": 0.933, "support": 15 } },
      "confusion": [[14, 1], [1, 14]],
      "warnings": [],
      "validation": "split 20%"
    }
  ]
}
```

`confusion[truth][prediction]` is indexed like the label list in `module.json`.
