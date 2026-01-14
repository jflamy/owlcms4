#!/usr/bin/env bash

# Update translation4.csv from Google Sheets
# This script downloads the latest translations from the public Google Sheet
# and saves them to the i18n resources directory

set -e  # Exit on error

# Google Sheet ID
SHEET_ID="1ZRfYHCARnPCnUEVZYo3Y_7qJGS9z7NRVg-Se7z3lHtE"

# Export URL
EXPORT_URL="https://docs.google.com/spreadsheets/d/${SHEET_ID}/export?format=csv"

# Target file (relative to script location)
# Script is in owlcms4/owlcms/scripts, need to go up to owlcms4
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
TARGET_FILE="${PROJECT_ROOT}/shared/src/main/resources/i18n/translation4.csv"

echo "Downloading translations from Google Sheets..."
echo "Source: ${EXPORT_URL}"
echo "Target: ${TARGET_FILE}"

# Create backup of existing file
if [ -f "$TARGET_FILE" ]; then
    BACKUP_FILE="${TARGET_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
    echo "Creating backup: ${BACKUP_FILE}"
    cp "$TARGET_FILE" "$BACKUP_FILE"
fi

# Download the CSV file
if curl -L -f "${EXPORT_URL}" -o "$TARGET_FILE"; then
    echo "✓ Translations updated successfully"
    echo "  File size: $(wc -c < "$TARGET_FILE") bytes"
    echo "  Lines: $(wc -l < "$TARGET_FILE") lines"
else
    echo "✗ Failed to download translations"
    # Restore backup if download failed
    if [ -f "$BACKUP_FILE" ]; then
        echo "Restoring backup..."
        mv "$BACKUP_FILE" "$TARGET_FILE"
    fi
    exit 1
fi

echo "Done!"
