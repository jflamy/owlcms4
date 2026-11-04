#!/bin/bash
# Restart the owlcms_69 server with the masters fixture, as the "load JSON owlcms reset" launch does, without the
# debugger: stop this worktree's server, clear the logs, start MainWrapper with the launch's VM options and
# environment (.vscode/.env.mac plus the launch env), wait until it answers.
#
#   compliance/phase7/restart-server.sh [fixture.json]
#
# The classpath is resolved once with Maven (offline, -pl owlcms -am so the shared module is in the reactor) and
# cached in owlcms/target/scenario-classpath.txt; it is rebuilt when a pom.xml is newer. The IDE keeps
# owlcms/target/classes and shared/target/classes compiled.

set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
APP="$ROOT/owlcms"
FIXTURE="${1:-$APP/src/test/resources/testDatabases/oldVeryOldMasters_2026.json}"
PORT=8080
JAVA="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/jbr_jcef-25.0.3-osx-aarch64-b496.62/Contents/Home}/bin/java"
CP_FILE="$APP/target/scenario-classpath.txt"
OUT="$APP/target/scenario-server.out"

# 1. classpath
if [[ ! -s "$CP_FILE" || "$ROOT/pom.xml" -nt "$CP_FILE" || "$APP/pom.xml" -nt "$CP_FILE" || "$ROOT/shared/pom.xml" -nt "$CP_FILE" ]]; then
	echo "resolving classpath"
	DEPS="$APP/target/scenario-deps.txt"
	# the compile phase lets the reactor resolve shared to its target/classes; the IDE's classes are not recompiled
	(cd "$ROOT" && mvn -o -q -pl owlcms -am compile dependency:build-classpath -Dmaven.main.skip=true \
		-DexcludeArtifactIds=shared -Dmdep.outputFile="$DEPS" -Dmdep.includeScope=runtime) \
		|| { echo "classpath resolution failed"; exit 1; }
	echo "$APP/target/classes:$ROOT/shared/target/classes:$(cat "$DEPS")" > "$CP_FILE"
fi

# 2. stop this script's server: the wrapper and its child, either of which can outlive the other
PIDS=$(pgrep -f "java.*$APP/target/classes.*app\.owlcms\.Main")
if [[ -n "$PIDS" ]]; then
	echo "stopping $PIDS"
	kill $PIDS
	for _ in $(seq 1 30); do
		pgrep -f "java.*$APP/target/classes.*app\.owlcms\.Main" > /dev/null || break
		sleep 1
	done
	pkill -9 -f "java.*$APP/target/classes.*app\.owlcms\.Main" 2> /dev/null
fi
if lsof -iTCP:$PORT -sTCP:LISTEN > /dev/null 2>&1; then
	echo "port $PORT is still in use by another process; stop it first"
	exit 1
fi

# 3. the launch's preLaunchTask
(cd "$APP" && find logs -maxdepth 2 \( -path 'logs/*.log' -o -path 'logs/audit/*.log' \) -type f -delete 2> /dev/null)

# 4. start, with the launch's environment
cd "$APP" || exit 1
set -a
[[ -f "$ROOT/.vscode/.env.mac" ]] && source "$ROOT/.vscode/.env.mac"
OWLCMS_MEMORYMODE=false
OWLCMS_ENABLEEVENTFORWARDING=false
OWLCMS_LOADJSON="$FIXTURE"
set +a
nohup "$JAVA" -ea -DcssEdit=true -XX:+AllowEnhancedClassRedefinition -XX:HotswapAgent=fatjar \
	--add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
	--enable-native-access=ALL-UNNAMED -Dlogback.configurationFile=logback.xml \
	-cp "$(cat "$CP_FILE")" app.owlcms.MainWrapper > "$OUT" 2>&1 &
echo "started pid $!, output in $OUT"

# 5. ready when the page answers and startup is complete (the port opens before the import and startup finish)
for _ in $(seq 1 120); do
	code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 "http://localhost:$PORT/")
	if [[ "$code" == "200" ]] && grep -q "application.started" logs/audit/competition_plain_*.log 2> /dev/null; then
		echo "ready: $(grep -h 'Importing' logs/owlcms.log 2> /dev/null | tail -1 | cut -c1-140)"
		exit 0
	fi
	sleep 1
done
echo "not ready after 120 s; see $OUT"
exit 1
