#!/usr/bin/env bash
# Start a Clojure REPL with dev paths included
cd "$(dirname "$0")/.." && clj -A:dev
