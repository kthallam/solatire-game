#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
output=$(mktemp -d)
trap 'rm -rf "$output"' EXIT
javac -d "$output" app/src/main/java/com/quietspider/app/SpiderGame.java tests/SpiderGameTest.java
java -cp "$output" com.quietspider.app.SpiderGameTest
