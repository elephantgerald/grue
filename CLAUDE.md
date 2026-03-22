# CLAUDE.md — Zork ZIL→Clojure Project

## Project Overview

Translating the original Zork trilogy from ZIL (Zork Implementation Language) to Clojure.
ZIL is a Lisp dialect, making Clojure a natural and fun translation target.

## Repository Layout

- `src/zork.zil/` — original OSS ZIL source for Zork 1, 2, and 3 (read-only reference)
- `src/zork.clojure/` — our Clojure rewrite
- `tests/` — unit and integration tests for both implementations
- `build/` — shell scripts for building
- `dev/` — developer environment scripts
- `docs/` — documentation

## Tech Stack

- Language: Clojure 1.12
- Runtime: Java 21 (OpenJDK/Temurin)
- Build tool: Clojure CLI (`clj` / `deps.edn`)
- Platform: WSL2 on Windows

## Key ZIL Concepts and Their Clojure Mappings

| ZIL | Clojure |
|-----|---------|
| `<CONSTANT name val>` | `(def name val)` |
| `<GLOBAL name val>` | `(def name (atom val))` |
| `<ROUTINE name (args) body>` | `(defn name [args] body)` |
| `<OBJECT name (props...)>` | map or record |
| `<COND (test body) ...>` | `(cond ...)` |
| `<SET var val>` | `(let [var val] ...)` |
| `<SETG var val>` | `(reset! var val)` |
| `<TELL "str" CR>` | `(println "str")` |
| `<REPEAT () body>` | `(loop [] ... (recur))` |
| `<VERB? v1 v2>` | `(#{:v1 :v2} @prsa)` |
| `<FSET? obj flag>` | flag keyword in a set in the object map |

## Core Game State

The ZIL parser/action loop uses these globals:
- `PRSA` — current verb (action)
- `PRSO` — direct object
- `PRSI` — indirect object
- `HERE` / `WINNER` — current room / current actor

## Development Notes

- The ZIL source in `src/zork.zil/` is reference only — do not modify it
- The Clojure rewrite lives in `src/zork.clojure/`
- Keep tests alongside the Clojure source as we build it out
