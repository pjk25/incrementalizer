#!/usr/bin/env bash

set -euxo pipefail

clojure -A:lint:lint/fix -d user
