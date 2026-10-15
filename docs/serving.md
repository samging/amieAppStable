# Serving a model

Every exported model can answer HTTP requests from inside the app.

1. Open **Models**, expand the model.
2. Set **Host** (default `127.0.0.1`), **Port**, **Path** (default `/models/<name>`) and the **Methods** (POST, GET),
   then **Save**. This writes `AMP/modules/<name>/serving.json`.
3. Switch **Listen for incoming data** on. The card turns green and shows `listening · up 3m`.

```bash
curl -X POST http://127.0.0.1:8765/models/intents -d '{"text":"turn the lights on"}'
curl 'http://127.0.0.1:8765/models/intents?text=turn+the+lights+on'
```

The POST and GET URLs are shown on the card and can be selected and copied.

- Every request is appended to `AMP/modules/<name>/incoming.jsonl` and counted as **incoming rows**.
- **Requests**, **errors**, **avg latency**, **max latency** and **last request** on the card are kept per
  listener from the moment it starts.
- Changes to host, port or path apply after switching the listener off and on.
- Models that were listening when the app closed start listening again on the next launch.
- **Cancel all** under ORT SESSIONS terminates in-flight inference runs and closes the sessions.

## The HTTP contract

| Request | Answer |
|---|---|
| `POST <path>/predict` with `{"text": "…"}` or a plain-text body | `200` `{"model": "intents", "predictions": [{"label": "lights", "score": 0.97}, …]}`, best first |
| `GET <path>/predict?text=…` | the same |
| no text (empty body, no `text` key, no `?text=`) | `400` `{"error": "send text as {\"text\": \"...\"}, a plain-text body or ?text="}` |
| a method not in the model's **Methods** list | `405` `{"error": "PUT not allowed, use [POST, GET]"}` |
| the model threw while answering | `500` `{"error": "…"}`, and the page's **errors** count goes up |
| nothing listens on the port | the connection is refused (no HTTP answer at all) |

Every answer is `application/json`. The path is `/models/<name>/predict` unless **Path** was changed. `/predict`
is always appended, so a Path of `/intents` serves `/intents/predict`.

## From a script

```kotlin
val url = serve("intents", port = 8765)          // null when it could not start; the console says why
log(url ?: "not started")
stopServing("intents")
```

`serve("name")` with no other argument reuses what the page saved in `serving.json`; a port, host, path or
methods given here are saved there too. The full example with the request code and every error handled is in
the [Cookbook](cookbook.md#12-serve-a-model-and-call-it-over-http). Three reasons a start fails, each one ERROR
line in the console: the model cannot be loaded, the port is already taken by another program, or the path is
already served by another model on that port.
