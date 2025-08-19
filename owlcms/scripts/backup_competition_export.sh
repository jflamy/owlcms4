#!/bin/bash

# Usage: DIRECTORY=/path/to/dir URL=http://host:port ./backup_competition_export.sh

set -e

# Check required environment variables
if [ -z "$DIRECTORY" ]; then
  echo "Error: DIRECTORY environment variable not set." >&2
  exit 1
fi
if [ -z "$URL" ]; then
  echo "Error: URL environment variable not set." >&2
  exit 1
fi

cd "$DIRECTORY"

BACKUP_DIR="backup"
mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
FILENAME="competition_export_${TIMESTAMP}.json"

curl -sf "$URL/competition/export" -o "$BACKUP_DIR/$FILENAME"

# Remove files older than 14 days in the backup directory
find "$BACKUP_DIR" -type f -name 'competition_export_*.json' -mtime +14 -delete

echo "Backup complete: $BACKUP_DIR/$FILENAME"
