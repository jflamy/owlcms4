#!/usr/bin/env bash
# Sends the control panel stop request to a running OWLCMS and records what happens: the HTTP result, whether the
# process is alive, whether the port is still open, and Java thread dumps while it shuts down.
# Works in Git Bash on Windows and in bash on Linux.
#
# usage: capture-stop.sh [port] [token-env-file]
#   port            default 8080
#   token-env-file  file with an OWLCMS_CONTROLPANELTOKEN=... line, default .vscode/.env
#                   at the root of the owlcms4 repository; OWLCMS_CONTROLPANELTOKEN in the
#                   environment wins
# The results go to stop-capture-HHMMSS in the current directory.
set -u

PORT=${1:-8080}
REPO_ROOT=$(cd "$(dirname "$0")/../.." && pwd)
case "$(uname -s)" in
MINGW* | MSYS* | CYGWIN*) WINDOWS=true ;;
*) WINDOWS=false ;;
esac
ENVFILE=${2:-$REPO_ROOT/.vscode/.env}
OUT=$PWD/stop-capture-$(date +%H%M%S)

TOKEN=${OWLCMS_CONTROLPANELTOKEN:-}
if [ -z "$TOKEN" ] && [ -f "$ENVFILE" ]; then
	TOKEN=$(sed -n 's/^OWLCMS_CONTROLPANELTOKEN=//p' "$ENVFILE" | tr -d '\r' | head -1)
fi
if [ -z "$TOKEN" ]; then
	echo "no token: set OWLCMS_CONTROLPANELTOKEN or put it in $ENVFILE; OWLCMS refuses a stop without it"
	exit 1
fi

JCMD=""
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/jcmd" ]; then
	JCMD="$JAVA_HOME/bin/jcmd"
elif command -v jcmd > /dev/null 2>&1; then
	JCMD=$(command -v jcmd)
fi

listener_pid() {
	if $WINDOWS; then
		netstat -ano -p TCP | tr -d '\r' | awk -v p=":$PORT" '$4 == "LISTENING" && substr($2, length($2) - length(p) + 1) == p { print $5; exit }'
	else
		ss -ltnpH "sport = :$PORT" | sed -n 's/.*pid=\([0-9]*\).*/\1/p' | head -1
	fi
}

alive() {
	if $WINDOWS; then
		tasklist //FI "PID eq $1" //NH //FO CSV 2> /dev/null | grep -q "\"$1\""
	else
		[ -e "/proc/$1" ]
	fi
}

process_state() {
	if $WINDOWS; then
		echo running
	else
		sed -n 's/^State:\s*//p' "/proc/$1/status" 2> /dev/null
	fi
}

port_state() {
	if [ -n "$(listener_pid)" ]; then echo listening; else echo released; fi
}

ms() { date +%H:%M:%S.%3N; }
now_ms() { date +%s%3N; }

thread_dump() {
	[ -n "$JCMD" ] || return
	"$JCMD" "$PID" Thread.print > "$1" 2>&1
}

PID=$(listener_pid)
if [ -z "$PID" ]; then
	echo "nothing listening on port $PORT"
	exit 1
fi
mkdir -p "$OUT"
echo "listener PID $PID on port $PORT" | tee "$OUT/timeline.txt"
[ -n "$JCMD" ] || echo "jcmd not found (set JAVA_HOME): no thread dumps" | tee -a "$OUT/timeline.txt"
thread_dump "$OUT/threads-before.txt"

echo "$(ms) POST /controlpanel/stop" | tee -a "$OUT/timeline.txt"
(
	result=$(curl -s -o /dev/null -w '%{http_code} in %{time_total}s' -X POST \
		-H "Authorization: Bearer $TOKEN" --max-time 30 "http://127.0.0.1:$PORT/controlpanel/stop")
	echo "$(ms) HTTP result: $result (curl exit $?)" | tee -a "$OUT/timeline.txt"
) &

DUMPS=" 1 3 5 8 12 20 30 45 "
last=""
start=$(now_ms)
while :; do
	sec=$((($(now_ms) - start) / 1000))
	if ! alive "$PID"; then
		echo "$(ms) PID $PID gone" | tee -a "$OUT/timeline.txt"
		break
	fi
	state="$(process_state "$PID"), port $(port_state)"
	if [ "$state" != "$last" ]; then
		echo "$(ms) $state" | tee -a "$OUT/timeline.txt"
		last=$state
	fi
	case "$DUMPS" in
	*" $sec "*)
		f="$OUT/threads-${sec}s.txt"
		if [ ! -e "$f" ] && [ -n "$JCMD" ]; then
			thread_dump "$f"
			echo "$(ms) thread dump $(basename "$f")" >> "$OUT/timeline.txt"
		fi
		;;
	esac
	if [ "$sec" -ge 60 ]; then
		echo "$(ms) still present after 60s" | tee -a "$OUT/timeline.txt"
		break
	fi
	sleep 0.2
done
wait
echo "results in $OUT"
