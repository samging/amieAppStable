# dummy

A fixed three-label head with made-up weights over the bge encoder, so the page has a model to configure, serve and validate and a script can try initMod("dummy") before training anything. Its scores are not meaningful; build a model from sample-intents.csv to see real ones.

## Labels

- greeting
- lights
- weather

## From a script

```
val model = initMod("dummy")
model.inference("your sentence")   // ["label:score", ...], best first
model.close()
```

A model has one session: initMod gives the open one when there is one. Terminate it on the Models page or close() it.

## Over HTTP

Listen on the Models page, then POST {"text": "..."} or GET ?text=... to its /predict address; Configure shows the address and a curl call.

## Validating

Validate on a csv with a text and a label column (the labels above). Every validation is a run on the Metrics tab.

## How it works

The text goes through the bge encoder (768 numbers, mean pooled), then through this model's head (dense[768 -> 3] sigmoid over mean-pooled bge), which gives one score per label.

## Files

- module.json: name, labels, encoder, how it was trained
- model.onnx: the head
- metrics.json: every validation run
- guide.md: this guide
