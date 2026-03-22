#!/usr/bin/env bash
# Run all tests
cd "$(dirname "$0")/.." && clj -M:test
