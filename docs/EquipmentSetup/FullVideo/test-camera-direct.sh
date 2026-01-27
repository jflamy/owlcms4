#!/bin/bash
#
# test-camera-direct.sh - Test recording directly from camera to MP4
#
# This script tests direct camera recording using the same H.264 parameters
# as the camera streaming setup in start-cameras.sh
#
# Usage:
#   ./test-camera-direct.sh [camera_number] [duration_seconds]
#   camera_number: 1 or 2 (default: 1)
#   duration_seconds: recording duration (default: 10)
#
# Examples:
#   ./test-camera-direct.sh        # Test camera 1 for 10 seconds
#   ./test-camera-direct.sh 2      # Test camera 2 for 10 seconds
#   ./test-camera-direct.sh 1 5    # Test camera 1 for 5 seconds

set -e

# Configuration (matches start-cameras.sh camera parameters)
VIDEO_SIZE="1920x1080"
FRAMERATE="60"
INPUT_FORMAT="h264"

# Auto-detect all cameras with H.264 support
detect_h264_cameras() {
    local cameras=()
    for dev in /dev/video*; do
        if [ -e "$dev" ] && ffmpeg -hide_banner -f v4l2 -list_formats all -i "$dev" 2>&1 | grep -q "h264"; then
            cameras+=("$dev")
        fi
    done
    echo "${cameras[@]}"
}

# Parse arguments
CAMERA_NUM=1
DURATION=10

if [[ "$1" =~ ^[12]$ ]]; then
    CAMERA_NUM=$1
    DURATION="${2:-10}"
else
    DURATION="${1:-10}"
fi

# Detect H.264 cameras
detected_cameras=($(detect_h264_cameras))

if [ ${#detected_cameras[@]} -eq 0 ]; then
    echo "ERROR: No camera with H.264 support found"
    echo "Available cameras:"
    for dev in /dev/video*; do
        [ -e "$dev" ] && echo "  $dev: $(ffmpeg -hide_banner -f v4l2 -list_formats all -i "$dev" 2>&1 | grep -E 'Compressed|Raw' | head -1)"
    done
    exit 1
fi

# Select camera based on camera number
CAMERA_INDEX=$((CAMERA_NUM - 1))
if [ $CAMERA_INDEX -ge ${#detected_cameras[@]} ]; then
    echo "ERROR: Camera $CAMERA_NUM not found"
    echo "Available H.264 cameras:"
    for i in "${!detected_cameras[@]}"; do
        echo "  Camera $((i+1)): ${detected_cameras[$i]}"
    done
    exit 1
fi

CAMERA_DEVICE="${detected_cameras[$CAMERA_INDEX]}"

# Recording parameters
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
OUTPUT_FILE="test_camera${CAMERA_NUM}_${TIMESTAMP}.mp4"

echo "=== Test Direct Camera Recording ==="
echo "Camera $CAMERA_NUM: $CAMERA_DEVICE (auto-detected)"
if [ ${#detected_cameras[@]} -gt 1 ]; then
    echo "Available cameras:"
    for i in "${!detected_cameras[@]}"; do
        echo "  Camera $((i+1)): ${detected_cameras[$i]}"
    done
fi
echo "Format: H.264 @ ${VIDEO_SIZE} ${FRAMERATE}fps"
echo "Duration: ${DURATION} seconds"
echo "Output: $OUTPUT_FILE"
echo ""

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
