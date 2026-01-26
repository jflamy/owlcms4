#!/bin/bash
#
# test-udp-record.sh - Test recording from UDP multicast stream
#
# This script tests recording from the UDP multicast stream, simulating
# what the replay system does when recording from start-cameras.sh
#
# Usage:
#   ./test-udp-record.sh [duration_seconds] [camera_number]
#   Default duration: 10 seconds
#   Default camera: 1

set -e

# Configuration (must match start-cameras.sh)
UDP_MULTICAST="239.255.0.1"
PORT_CAMERA1="9001"
PORT_CAMERA2="9002"

# Recording parameters
DURATION="${1:-10}"
CAMERA_NUM="${2:-1}"
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
OUTPUT_FILE="test_udp_camera${CAMERA_NUM}_${TIMESTAMP}.mp4"

# Select port based on camera number
if [ "$CAMERA_NUM" = "1" ]; then
    PORT="$PORT_CAMERA1"
elif [ "$CAMERA_NUM" = "2" ]; then
    PORT="$PORT_CAMERA2"
else
    echo "ERROR: Invalid camera number. Use 1 or 2."
    exit 1
fi

UDP_URL="udp://@${UDP_MULTICAST}:${PORT}"

echo "=== Test UDP Multicast Recording ==="
echo "Source: $UDP_URL"
echo "Duration: ${DURATION} seconds"
echo "Output: $OUTPUT_FILE"
echo ""
echo "Recording from multicast stream..."
echo "(Make sure start-cameras.sh is running)"
echo ""

# Record from UDP multicast stream
# This simulates what the replay system does
ffmpeg -f mpegts \
       -i "$UDP_URL" \
       -c:v copy \
       -an \
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
    
    # Show video properties
    echo "Video properties:"
    ffprobe -v error -select_streams v:0 \
            -show_entries stream=width,height,r_frame_rate,codec_name \
            -of csv=p=0 "$OUTPUT_FILE"
    echo ""
    echo "Play with: ffplay $OUTPUT_FILE"
else
    echo "ERROR: Recording failed"
    exit 1
fi
