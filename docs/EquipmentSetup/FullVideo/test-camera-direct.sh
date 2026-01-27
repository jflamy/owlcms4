#!/bin/bash
#
# test-camera-direct.sh - Test recording directly from camera to MP4
#
# This script tests direct camera recording using the same H.264 parameters
# as the camera streaming setup in start-cameras.sh
#
# Usage:
#   ./test-camera-direct.sh [duration_seconds]
#   Default duration: 10 seconds

set -e

# Configuration (matches start-cameras.sh camera parameters)
CAMERA_DEVICE="/dev/video0"
VIDEO_SIZE="1920x1080"
FRAMERATE="60"
INPUT_FORMAT="h264"

# Recording parameters
DURATION="${1:-10}"
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
OUTPUT_FILE="test_camera_direct_${TIMESTAMP}.mp4"

echo "=== Test Direct Camera Recording ==="
echo "Camera: $CAMERA_DEVICE"
echo "Format: H.264 @ ${VIDEO_SIZE} ${FRAMERATE}fps"
echo "Duration: ${DURATION} seconds"
echo "Output: $OUTPUT_FILE"
echo ""

# Check if camera exists
if [ ! -e "$CAMERA_DEVICE" ]; then
    echo "ERROR: Camera device $CAMERA_DEVICE not found"
    exit 1
fi

echo "Starting recording..."

# Record directly from camera using same parameters as start-cameras.sh
# Note: Adding timestamp and sync fixes for camera H.264 streams that may have
# problematic timestamps causing MP4 muxer assertion failures
ffmpeg -f v4l2 \
       -input_format "$INPUT_FORMAT" \
       -video_size "$VIDEO_SIZE" \
       -framerate "$FRAMERATE" \
       -use_wallclock_as_timestamps 1 \
       -i "$CAMERA_DEVICE" \
       -c:v copy \
       -an \
       -fflags +genpts \
       -vsync cfr \
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
else
    echo ""
    echo "ERROR: Recording failed"
    exit 1
fi
