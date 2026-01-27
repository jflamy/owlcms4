#!/bin/bash
# Test PTZ movement sequence: 45° left, home, 45° right, home, 90° left, home, 90° right, home

DEVICE="/dev/video2"
DELAY=2          # seconds between movements
TILT_45=162000   # approx 45° tilt (adjust if needed)
TILT_HOME=0

echo "PTZ Movement Test Sequence"
echo "Device: $DEVICE"
echo "Delay between moves: ${DELAY}s"
echo ""

move_camera() {
    local pan=$1
    local description=$2
    echo "Moving to: $description (pan=$pan)"
    v4l2-ctl --device=$DEVICE --set-ctrl=pan_absolute=$pan
    sleep $DELAY
}

move_tilt() {
    local tilt=$1
    local description=$2
    echo "Tilting to: $description (tilt=$tilt)"
    v4l2-ctl --device=$DEVICE --set-ctrl=tilt_absolute=$tilt
    sleep $DELAY
}

move_pan_tilt() {
    local pan=$1
    local tilt=$2
    local description=$3
    echo "Moving pan+tilt to: $description (pan=$pan, tilt=$tilt)"
    v4l2-ctl --device=$DEVICE --set-ctrl=pan_absolute=$pan,tilt_absolute=$tilt
    sleep $DELAY
}

# Verify device exists
if [ ! -e "$DEVICE" ]; then
    echo "Error: Device $DEVICE not found"
    exit 1
fi

echo "Starting sequence..."
echo ""

move_pan_tilt 0       $TILT_HOME "HOME (Start)"
move_pan_tilt -60000  $TILT_45   "45° LEFT + 45° UP"
move_pan_tilt 0       $TILT_HOME "HOME"
move_pan_tilt 162000  $TILT_45   "45° RIGHT + 45° UP"
move_pan_tilt 0       $TILT_HOME "HOME"
move_camera -216000 "90° LEFT"
move_camera 324000  "90° RIGHT"
move_pan_tilt 0       $TILT_HOME "HOME (Final)"

# Re-assert home after a short settle delay in case the last move overshoots
sleep 1
move_pan_tilt 0       $TILT_HOME "HOME (Recheck)"

echo ""
echo "Sequence complete!"
v4l2-ctl --device=$DEVICE --get-ctrl=pan_absolute
