#!/usr/bin/env bash

cd "$(dirname "$0")/.."

set -euxo pipefail

mkdir -p classes

clojure -J-Dclojure.compiler.direct-linking=true \
  -e "(compile 'incrementalizer.cli)"

clojure -A:uberjar

zip -d target/incrementalizer.jar "*.clj"
