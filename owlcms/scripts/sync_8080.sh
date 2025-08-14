#!/bin/bash -
export LC_ALL=C.UTF-8

# Configuration variables - edit these as needed
REMOTE_HOST="jflamy@143.110.208.71"
REMOTE_PORT=8080
REMOTE_DIR="/home/jflamy/fhq"
DATABASE_FOLDER="database"

rem empty value prevents copy
LOCAL_SOURCE_DIR1=
LOCAL_SOURCE_DIR2="/c/Users/lamyj/git/owlcms4/owlcms/local/logos"
LOCAL_SOURCE_DIR3="/c/Users/lamyj/git/owlcms4/owlcms/local/flags"

# SSH to the remote server for all operations
echo "Connecting to remote server for port cleanup and owlcms download..."


# Set up variables locally
REPO_OWNER="owlcmlsof -
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

echo ""
pwd
rm -f *.zip
echo "Retrieving latest owlcms release..."
echo "Downloading: $DOWNLOAD_URL"
curl -LJO "$DOWNLOAD_URL"

if [ $? -eq 0 ]; then
    echo "Successfully downloaded latest owlcms release"
    DOWNLOADED_FILE=$(ls -1 $FILE_PREFIX*.jar $FILE_PREFIX*.zip 2>/dev/null | tail -1)
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
if [ -d "${LOCAL_SOURCE_DIR1}" ]; then
    echo ""
    echo "Copying local files from ${LOCAL_SOURCE_DIR} to remote server..."
    
    # Use scp to copy files with directory structure preserved
    # First create the local directory on remote server, then copy contents
    ssh ${REMOTE_HOST} "mkdir -p ${REMOTE_DIR}/local"
    scp -r "${LOCAL_SOURCE_DIR1}" "${LOCAL_SOURCE_DIR2}" "${LOCAL_SOURCE_DIR3}" ${REMOTE_HOST}:"${REMOTE_DIR}/local/"
    
    if [ $? -eq 0 ]; then
        echo "Successfully copied local files ${LOCAL_SOURCE_DIR1} to remote server"
    else
        echo "Error copying local files to remote server"
    fi
else
    echo "Local source directory ${LOCAL_SOURCE_DIR} not found, skipping file copy"
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
nohup java -jar owlcms*.jar > owlcms.log 2>&1 &echo "owlcms started in background with PID: $!"
echo "Logs will be written to owlcms.log"
EOF

echo "Remote operations completed"
