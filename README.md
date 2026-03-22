# Zork — ZIL to Clojure

A faithful translation of the original Zork trilogy from ZIL (Zork Implementation Language)
into Clojure, for fun and education.

## Background

The original Zork games were written in ZIL, Infocom's Lisp-derived language, in the late 1970s
and early 1980s. ZIL compiles to Z-machine bytecode. Since ZIL is itself an s-expression language,
Clojure is a natural translation target.

## Repository Layout

```
src/
  zork.zil/         Original OSS ZIL source (zork1, zork2, zork3)
  zork.clojure/     Clojure rewrite

tests/
  unit/
    zork.zil/       Unit tests for ZIL behaviour reference
    zork.clojure/   Unit tests for Clojure implementation
  integration/
    zork.zil/       Integration tests (ZIL reference)
    zork.clojure/   Integration tests (Clojure implementation)

build/              Shell scripts for building projects
dev/                Developer environment and workflow scripts
docs/               Documentation
```

## Getting Started

### Prerequisites

- Java 11+ (tested on OpenJDK 21)
- Clojure CLI (`clj`) 1.11+

### Running the Clojure REPL

```bash
clj
```

### Running tests

```bash
clj -M:test
```

### A note on the test suite

The integration test assertions are the exact output of the original Zork I
binary (revision 88, serial 840726), verified by hand in DOSBox. They are the
ground truth for this project.

**If a test fails, the code is wrong. The tests are almost certainly not wrong.**

Do not modify an assertion to make it pass. The only valid reason to change one
is evidence from a fresh DOSBox session showing the assertion was transcribed
incorrectly.

## License

This project is licensed under the Apache 2.0 License — see [LICENSE.md](LICENSE.md).

The original ZIL source files in `src/zork.zil/` are the property of their respective
copyright holders and are included under their original open-source licenses.
