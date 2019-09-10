#!/usr/bin/env bash

cd "$(dirname "$0")/.."

set -euxo pipefail

clojure -m incrementalizer.cli "$@"
