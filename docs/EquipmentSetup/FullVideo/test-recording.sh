#!/bin/bash
#
# test-recording.sh - Test recording from UDP multicast stream
#
# This script tests the FFmpeg #2 (on-demand recording) configuration
# using the same parameters as linux5 in config.toml
#
# Usage:
#   ./test-recording.sh [duration_seconds]
#   Default duration: 10 seconds

set -e

# Configuration (matches linux5 in config.toml)
UDP_SOURCE="udp://239.255.0.1:9001"
FORMAT="mpegts"
OUTPUT_PARAMS="-c:v copy -an"

# Note: Resolution (1280x720) and framerate (60fps) are embedded in the MPEG-TS stream
# and don't need to be specified when reading from UDP

# Recording parameters
DURATION="${1:-10}"
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
OUTPUT_FILE="test_recording_${TIMESTAMP}.mp4"

echo "=== Test Recording from UDP Stream ==="
echo "Source: $UDP_SOURCE"
echo "Duration: ${DURATION} seconds"
echo "Output: $OUTPUT_FILE"
echo ""
echo "Starting recording..."

# Record using the same parameters as linux5 in config.toml
ffmpeg -f "$FORMAT" \
       -i "$UDP_SOURCE" \
       $OUTPUT_PARAMS \
       -t "$DURATION" \
       "$OUTPUT_FILE"

if [ $? -eq 0 ]; then
    echo ""
    echo "=== Recording Complete ==="
    echo "File: $OUTPUT_FILE"
    echo ""
    
    # Show file info
    ls -lh "$OUTPUT_FILE"
    echo ""
    
    # Probe the file
    ffprobe -v error -show_format -show_streams "$OUTPUT_FILE" 2>&1 | grep -E "(codec_name|width|height|duration|bit_rate)"
    
    echo ""
    echo "To play the recording:"
    echo "  ffplay $OUTPUT_FILE"
else
    echo ""
    echo "ERROR: Recording failed"
    exit 1
fi
