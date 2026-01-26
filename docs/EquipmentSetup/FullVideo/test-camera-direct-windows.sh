#!/bin/bash
#
# test-camera-direct-windows.sh - Test recording directly from camera to MP4 (Windows/Git Bash)
#
# This script tests direct camera recording using H.264 from a UVC camera on Windows
#
# Usage:
#   ./test-camera-direct-windows.sh [duration_seconds]
#   Default duration: 10 seconds

set -e

# Configuration
CAMERA_NAME="UVC Camera"
VIDEO_SIZE="1280x720"
FRAMERATE="60"
INPUT_FORMAT="dshow"

# Recording parameters
DURATION="${1:-10}"
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
OUTPUT_FILE="test_camera_direct_${TIMESTAMP}.mp4"

echo "=== Test Direct Camera Recording (Windows) ==="
echo "Camera: $CAMERA_NAME"
echo "Format: H.264 @ ${VIDEO_SIZE} ${FRAMERATE}fps"
echo "Duration: ${DURATION} seconds"
echo "Output: $OUTPUT_FILE"
echo ""
echo "Starting recording..."

# Record directly from camera using DirectShow
ffmpeg -f "$INPUT_FORMAT" \
       -video_size "$VIDEO_SIZE" \
       -framerate "$FRAMERATE" \
       -i video="$CAMERA_NAME" \
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
    
    # Probe the file
    echo "Video properties:"
    ffprobe -v error -show_format -show_streams "$OUTPUT_FILE" 2>&1 | grep -E "(codec_name|width|height|duration|bit_rate|r_frame_rate)"
    
    echo ""
    echo "To play the recording:"
    echo "  ffplay $OUTPUT_FILE"
    echo ""
    echo "To list available cameras:"
    echo "  ffmpeg -list_devices true -f dshow -i dummy"
else
    echo ""
    echo "ERROR: Recording failed"
    exit 1
fi
