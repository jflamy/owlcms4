#!/bin/bash -
export LC_ALL=C.UTF-8

# Configuration variables - edit these as needed
REMOTE_HOST="jflamy@143.110.208.71"
REMOTE_PORT=8084
REMOTE_DIR="/home/jflamy/isg2025"
DATABASE_FOLDER="database"
LOCAL_SOURCE_ROOT="/c/Dev/git/owlcms-meets/competitions/isg2025/local"
LOCAL_SOURCE_DIR1="${LOCAL_SOURCE_ROOT}/css"
LOCAL_SOURCE_DIR2="${LOCAL_SOURCE_ROOT}/logos"
LOCAL_SOURCE_DIR3="${LOCAL_SOURCE_ROOT}/flags"
LOCAL_SOURCE_DIR4="${LOCAL_SOURCE_ROOT}/img"

# SSH to the remote server for all operations
echo "Connecting to remote server for port cleanup and owlcms download..."

# Set up variables locally
REPO_OWNER="owlcms"
REPO_NAME="owlcms4-prerelease"
FILE_PREFIX="owlcms"
GITHUB_API_URL="https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases/latest"
echo "GitHub API URL: ${GITHUB_API_URL}"
API_RESPONSE=$(curl -s "${GITHUB_API_URL}")
if [ -z "$API_RESPONSE" ]; then
    echo "Error: Failed to fetch release info from GitHub API."
    exit 1
fi

DOWNLOAD_URL=$(echo "$API_RESPONSE" | jq -e -r '.assets[]? | .browser_download_url' 2>/dev/null | grep "${FILE_PREFIX}" | head -1)
if [ $? -ne 0 ] || [ -z "$DOWNLOAD_URL" ]; then
    echo "Error: No downloadable asset found or jq failed to parse the release info."
    exit 1
fi

ssh ${REMOTE_HOST} -q \
    REMOTE_PORT=${REMOTE_PORT} \
    REMOTE_DIR=${REMOTE_DIR} \
    DOWNLOAD_URL="${DOWNLOAD_URL}" \
    FILE_PREFIX="${FILE_PREFIX}" \
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
    echo "Found processes on port (may include threads): $(echo $PIDS | wc -w) entries"
    
    # Collect unique parent PIDs (all threads share the same parent)
    KILL_PIDS=""
    for pid in $PIDS; do
        # Get parent PID for this process/thread
        PARENT_PID=$(ps -o ppid= -p $pid 2>/dev/null | tr -d ' ')
        if [ ! -z "$PARENT_PID" ] && [ "$PARENT_PID" != "1" ]; then
            # Add parent if not already in list
            if ! echo "$KILL_PIDS" | grep -qw "$PARENT_PID"; then
                KILL_PIDS="$KILL_PIDS $PARENT_PID"
            fi
        else
            # No parent or orphan, add the process itself
            if ! echo "$KILL_PIDS" | grep -qw "$pid"; then
                KILL_PIDS="$KILL_PIDS $pid"
            fi
        fi
    done
    
    echo "Unique processes to kill:$KILL_PIDS"
    for pid in $KILL_PIDS; do
        echo "Process $pid:"
        ps -p $pid -o pid,ppid,user,command 2>/dev/null
    done
    
    # Kill all collected PIDs
    for pid in $KILL_PIDS; do
        if kill -0 $pid 2>/dev/null; then
            echo "Killing $pid..."
            kill -TERM $pid 2>/dev/null
        fi
    done
    
    sleep 2
    
    # Force kill any remaining
    for pid in $KILL_PIDS; do
        if kill -0 $pid 2>/dev/null; then
            echo "Force killing $pid..."
            kill -KILL $pid 2>/dev/null
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

echo ""
pwd
rm -f *.zip *.jar
echo "Retrieving latest owlcms release..."
echo "Downloading: $DOWNLOAD_URL"
curl -LJO "$DOWNLOAD_URL"

if [ $? -eq 0 ]; then
    echo "Successfully downloaded latest owlcms release"
    DOWNLOADED_FILE=$(ls -1t $FILE_PREFIX*.jar $FILE_PREFIX*.zip 2>/dev/null | head -1)
    echo "Downloaded file: $DOWNLOADED_FILE"
    if [[ "$DOWNLOADED_FILE" == *.zip ]]; then
        echo "Unzipping $DOWNLOADED_FILE..."
        unzip -q -o "$DOWNLOADED_FILE"
        if [ $? -eq 0 ]; then
            echo "Successfully unzipped $DOWNLOADED_FILE"
#            rm "$DOWNLOADED_FILE"
#            echo "Removed zip file $DOWNLOADED_FILE"
        else
            echo "Error unzipping $DOWNLOADED_FILE"
        fi
    else
        echo "File is not a zip archive, no extraction needed"
    fi
else
    echo "Error downloading owlcms release"
fi
EOSSH

# Copy local files to remote server if local source directory exists
if [ -d "${LOCAL_SOURCE_ROOT}" ]; then
    echo "Copying local files to remote server..."
    ssh ${REMOTE_HOST} -q "mkdir -p ${REMOTE_DIR}/local"
    scp -q -r "${LOCAL_SOURCE_DIR1}" "${LOCAL_SOURCE_DIR2}" "${LOCAL_SOURCE_DIR3}" "${LOCAL_SOURCE_DIR4}" ${REMOTE_HOST}:"${REMOTE_DIR}/local/"
    
    if [ $? -eq 0 ]; then
        echo "Successfully copied local files to remote server"
    else
        echo "Error copying local files to remote server"
    fi
else
    echo "Local source directory ${LOCAL_SOURCE_ROOT} not found, skipping file copy"
fi

# Start owlcms.jar as a background process on remote server
echo ""
echo "Starting owlcms.jar as background process..."
ssh ${REMOTE_HOST} -q \
    REMOTE_PORT=${REMOTE_PORT} \
    REMOTE_DIR=${REMOTE_DIR} \
    DOWNLOAD_URL="${DOWNLOAD_URL}" \
    FILE_PREFIX="${FILE_PREFIX}" \
    'bash -s' <<'EOF'
cd $REMOTE_DIR
echo switching to $(pwd)
export OWLCMS_PORT=$REMOTE_PORT
export OWLCMS_ENABLEEMBEDDEDMQTT=false
export OWLCMS_FEATURESWITCHES=iwfLook
nohup java -cp owlcms*.jar app.owlcms.MainWrapper > owlcms.log 2>&1 &
echo "owlcms started in background with PID: $!"
echo "Logs will be written to owlcms.log"
EOF

echo "Remote operations completed"
