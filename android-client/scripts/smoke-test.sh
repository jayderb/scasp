#!/usr/bin/env bash
# Quick offline smoke check: build all modules without tests, then run the Stage 1 core demo.
# Usage (project root): bash scripts/smoke-test.sh      (Windows: scripts\smoke-test.ps1)
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -q -DskipTests package
java -cp core/target/classes com.scasp.app.Main
printf '\nSmoke check completed: all modules compiled and the core demo ran. Run "mvn test" for the JUnit suites.\n'
