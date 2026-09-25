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
| `<VERB? v1 v2>` | `(#{:v1 :v2} prsa)` — verb passed in, not a global |
| `<FSET? obj flag>` | flag keyword in a set in the object map |

## Core Game State

The ZIL parser/action loop uses these globals:
- `PRSA` — current verb (action)
- `PRSO` — direct object
- `PRSI` — indirect object
- `HERE` / `WINNER` — current room / current actor

In Clojure, `HERE`/`WINNER` are atoms (`@here`, `@winner`), but PRSA/PRSO/PRSI
are **passed as arguments**, not globals: verb handlers take object keys, and
`object-action` handlers receive `{:prsa :prso :prsi}`. ZIL PERFORM applies
PRSI's ACTION before PRSO's, so a handler must check `:prso`/`:prsi` itself
when it cares which role its object is playing.

## Development Notes

- The ZIL source in `src/zork.zil/` is reference only — do not modify it
- The Clojure rewrite lives in `src/zork.clojure/`
- Keep tests alongside the Clojure source as we build it out

## On the Test Suite

The integration test assertions in `tests/integration/` are not guesses or
approximations — they are the exact output of the original Zork I binary
(revision 88, serial 840726), verified by running the original story file.

**If a test fails, the code is wrong. The test is almost certainly not wrong.**

Do not modify an integration test assertion to make it pass. Investigate why
the code no longer produces the output the original game produces. The only
legitimate reason to change an assertion is if you can demonstrate with a fresh
run of the original that its output differs from what the test expects — which
would mean the test was transcribed incorrectly in the first place. Cite the
run in the test or commit message.

**Verifying against the original:** use `dfrotz` (preferred over DOSBox) on the
r88/840726 story file from the GOG install, piping commands in:

```bash
printf 'open mailbox\nscore\n' | dfrotz -m -p -w 200 \
  "/mnt/c/Program Files (x86)/GOG Galaxy/Games/Zork/DATA/ZORK1.DAT"
```

`score` prints the move count, which is how turn-cost behavior is checked.
