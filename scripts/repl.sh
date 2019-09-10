#!/usr/bin/env bash

set -euxo pipefail

clj -A:user -C:test -R:test -r
