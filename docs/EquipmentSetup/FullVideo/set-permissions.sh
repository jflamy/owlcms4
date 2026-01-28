#!/bin/bash
set -e

# set-permissions.sh - create camera streaming system dirs and set ownership
# Usage: sudo ./set-permissions.sh [username]
# If username omitted and script run under sudo, the invoking user will be used.

if [ "$1" != "" ]; then
    OWNER="$1"
else
    if [ -n "$SUDO_USER" ]; then
        OWNER="$SUDO_USER"
    else
        OWNER="$(id -un)"
    fi
fi

DIRS=(/var/log/camera-streaming /var/run/camera-streaming)

for d in "${DIRS[@]}"; do
    echo "Creating $d (if needed)"
    mkdir -p "$d"
    echo "Setting owner to $OWNER"
    chown "$OWNER":"$OWNER" "$d"
    chmod 0755 "$d"
done

echo "Done. Created and set permissions on: ${DIRS[*]}"
