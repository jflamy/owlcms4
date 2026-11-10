#!/usr/bin/env bash
# build-owlcms-classpath.sh — compute the classpath that run-jury-scenario.sh --server uses to start its own owlcms.
# Writes playwright/owlcms-cp.txt (not committed).
#
# Run once, and again after a change of Vaadin version or of any other dependency. It is not part of a scenario
# run: the owlcms and shared classes are read from owlcms/target/classes and shared/target/classes, which the IDE
# keeps compiled.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(dirname "$SCRIPT_DIR")"
REPO_DIR="$(dirname "$MODULE_DIR")"
CP_NAME="owlcms-cp.txt"

cd "$REPO_DIR"
# compile: the reactor then resolves the shared module to shared/target/classes instead of an installed jar.
# The file name is relative, so every module of the reactor writes its own; only the owlcms one is kept.
mvn -q -pl owlcms -am compile dependency:build-classpath -Dmdep.includeScope=runtime -Dmdep.outputFile="$CP_NAME"

if [[ ! -s "owlcms/$CP_NAME" ]]; then
    echo "owlcms/$CP_NAME was not written" >&2
    exit 1
fi
mv "owlcms/$CP_NAME" "$MODULE_DIR/$CP_NAME"
rm -f "shared/$CP_NAME" "$CP_NAME"
case ":$(cat "$MODULE_DIR/$CP_NAME"):" in
    *":$REPO_DIR/shared/target/classes:"*) ;;
    *) echo "warning: $CP_NAME does not use shared/target/classes; the shared module would come from a jar" >&2 ;;
esac
echo "wrote $MODULE_DIR/$CP_NAME ($(tr ':' '\n' < "$MODULE_DIR/$CP_NAME" | wc -l | tr -d ' ') entries)"
