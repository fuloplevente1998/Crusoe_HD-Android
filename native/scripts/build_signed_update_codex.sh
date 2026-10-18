#!/usr/bin/env bash
# Compatibility entry point; the same build works locally and on GitHub.
set -euo pipefail
exec bash "$(dirname "$0")/build_update.sh" "$@"
