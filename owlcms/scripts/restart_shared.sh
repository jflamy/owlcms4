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
    echo "Found processes on port: $PIDS"
    
    # For each port-owning process, find its parent (MainWrapper) and kill that instead
    for pid in $PIDS; do
        echo "Process details for $pid:"
        ps -p $pid -o pid,ppid,user,command 2>/dev/null
        
        # Get parent PID
        PPID=$(ps -o ppid= -p $pid 2>/dev/null | tr -d ' ')
        if [ ! -z "$PPID" ] && [ "$PPID" != "1" ]; then
            PARENT_CMD=$(ps -o command= -p $PPID 2>/dev/null)
            if echo "$PARENT_CMD" | grep -q "MainWrapper"; then
                echo "Found MainWrapper parent process: $PPID"
                echo "Killing MainWrapper (parent) instead of child..."
                kill -TERM $PPID 2>/dev/null
                sleep 2
                if kill -0 $PPID 2>/dev/null; then
                    echo "MainWrapper $PPID still running, force killing..."
                    kill -KILL $PPID 2>/dev/null
                else
                    echo "MainWrapper $PPID terminated successfully"
                fi
                continue
            fi
        fi
        
        # Fallback: kill the port-owning process directly
        echo "Killing process $pid directly..."
        kill -TERM $pid 2>/dev/null
        sleep 2
        if kill -0 $pid 2>/dev/null; then
            echo "Process $pid still running, force killing..."
            kill -KILL $pid 2>/dev/null
        else
            echo "Process $pid terminated successfully"
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
nohup env OWLCMS_PORT=$OWLCMS_PORT OWLCMS_ENABLEEMBEDDEDMQTT=false java -cp owlcms*.jar app.owlcms.MainWrapper > owlcms.log 2>&1 &
echo "owlcms started in background with PID: $! on port $OWLCMS_PORT"
echo "Logs will be written to owlcms.log"
EOSSH

echo "Remote operations completed"
