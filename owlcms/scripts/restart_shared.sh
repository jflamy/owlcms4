#!/bin/bash -
export LC_ALL=C.UTF-8

# Usage: ./restart_shared.sh <remote_host> <remote_port> <remote_dir>
REMOTE_HOST="$1"
REMOTE_PORT="$2"
REMOTE_DIR="$3"

if [ -z "$REMOTE_HOST" ] || [ -z "$REMOTE_PORT" ] || [ -z "$REMOTE_DIR" ]; then
    echo "Usage: $0 <remote_host> <remote_port> <remote_dir>"
    exit 1
fi

# SSH to the remote server for all operations
echo "Connecting to remote server for port cleanup and owlcms restart..."

ssh ${REMOTE_HOST} -q \
    REMOTE_PORT=${REMOTE_PORT} \
    REMOTE_DIR=${REMOTE_DIR} \
    OWLCMS_PORT=${REMOTE_PORT} \
    'bash -s' <<'EOSSH'
cd $REMOTE_DIR
pwd
echo "Checking for processes on port $REMOTE_PORT..."
echo "Using lsof to find processes on port: $REMOTE_PORT"
echo "Running: lsof -ti:$REMOTE_PORT 2>/dev/null"
PIDS=$(lsof -ti:$REMOTE_PORT 2>/dev/null)
echo "lsof result: $PIDS"

if [ -z "$PIDS" ]; then
    echo "No processes found running on port $REMOTE_PORT"
else
    echo "Found processes to kill: $PIDS"
    echo "Process details:"
    for pid in $PIDS; do
        ps -p $pid -o pid,user,command 2>/dev/null
    done

    echo "Killing processes..."
    for pid in $PIDS; do
        if [ ! -z "$pid" ] && [ "$pid" != "-" ]; then
            echo "Attempting to kill process $pid"
            kill -TERM $pid 2>/dev/null
            sleep 2

            # Check if process is still running, force kill if necessary
            if kill -0 $pid 2>/dev/null; then
                echo "Process $pid still running, force killing..."
                kill -KILL $pid 2>/dev/null
            else
                echo "Process $pid terminated successfully"
            fi
        fi
    done

    # Verify processes are killed
    sleep 1
    REMAINING=$(lsof -ti:$REMOTE_PORT 2>/dev/null)
    if [ -z "$REMAINING" ]; then
        echo "All processes on port $REMOTE_PORT have been successfully killed"
    else
        echo "Warning: Some processes may still be running on port $REMOTE_PORT"
    fi
fi

# Start owlcms.jar as a background process on remote server
echo ""
echo "Starting owlcms.jar as background process on port $OWLCMS_PORT..."
nohup env OWLCMS_PORT=$OWLCMS_PORT OWLCMS_ENABLEEMBEDDEDMQTT=false java -jar owlcms*.jar > owlcms.log 2>&1 &
echo "owlcms started in background with PID: $! on port $OWLCMS_PORT"
echo "Logs will be written to owlcms.log"
EOSSH

echo "Remote operations completed"
