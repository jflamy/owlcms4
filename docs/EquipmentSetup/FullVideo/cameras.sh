#!/bin/bash
#
# start-cameras.sh - Start continuous UDP streaming from PTZ cameras
#
# This script starts the FFmpeg processes that capture H.264 video from the
# PTZ cameras and stream it via UDP to OBS systems. These processes run
# continuously and should be started before the replay system.
#
# Usage:
#   ./start-cameras.sh
#
# To run at boot, add to /etc/rc.local or create a systemd service.

set -e

# Auto-detect cameras with H.264 support
detect_h264_cameras() {
    local cameras=()
    for dev in /dev/video*; do
        if [ ! -e "$dev" ]; then
            continue
        fi
        # Only use even-numbered video devices for cameras (e.g. /dev/video2, /dev/video4)
        num=${dev#/dev/video}
        if ! [[ "$num" =~ ^[0-9]+$ ]]; then
            continue
        fi
        if (( num % 2 != 0 )); then
            continue
        fi
        if ffmpeg -hide_banner -f v4l2 -list_formats all -i "$dev" 2>&1 | grep -q "h264"; then
            cameras+=("$dev")
        fi
    done
    echo "${cameras[@]}"
}

# Detect non-H.264 cameras (e.g. webcams that provide MJPEG/YUYV)
detect_non_h264_cameras() {
    local cameras=()
    for dev in /dev/video*; do
        if [ ! -e "$dev" ]; then
            continue
        fi
        # Only use even-numbered video devices for cameras
        num=${dev#/dev/video}
        if ! [[ "$num" =~ ^[0-9]+$ ]]; then
            continue
        fi
        if (( num % 2 != 0 )); then
            continue
        fi
        # If device supports h264, skip it
        if ffmpeg -hide_banner -f v4l2 -list_formats all -i "$dev" 2>&1 | grep -q "h264"; then
            continue
        fi
        cameras+=("$dev")
    done
    echo "${cameras[@]}"
}

# Configuration
# To manually set cameras, uncomment and set DEVICE lines below
# Otherwise, will auto-detect cameras with H.264 support

#CAMERA1_DEVICE="/dev/video2"
PORT_CAMERA1="9001"

#CAMERA2_DEVICE="/dev/video4"
PORT_CAMERA2="9002"

#CAMERA3_DEVICE="/dev/video0"
PORT_CAMERA3="9003"
CAMERA3_FRAMERATE="30"

# Auto-detect cameras independently if not manually configured
detected_cameras=($(detect_h264_cameras))

# Also detect non-H.264 devices and echo both lists for operator visibility
nonh264_devices=($(detect_non_h264_cameras))
if [ ${#detected_cameras[@]} -gt 0 ]; then
    echo "Detected H.264 devices: ${detected_cameras[@]}"
else
    echo "Detected H.264 devices: none"
fi
if [ ${#nonh264_devices[@]} -gt 0 ]; then
    echo "Detected non-H.264 devices: ${nonh264_devices[@]}"
else
    echo "Detected non-H.264 devices: none"
fi

if [ -z "$CAMERA1_DEVICE" ]; then
    if [ ${#detected_cameras[@]} -gt 0 ]; then
        CAMERA1_DEVICE="${detected_cameras[0]}"
        echo "Auto-detected Camera 1: $CAMERA1_DEVICE"
    fi
fi

if [ -z "$CAMERA2_DEVICE" ]; then
    if [ ${#detected_cameras[@]} -gt 1 ]; then
        CAMERA2_DEVICE="${detected_cameras[1]}"
        echo "Auto-detected Camera 2: $CAMERA2_DEVICE"
    fi
fi

# Auto-detect first non-H264 camera for Camera 3 (webcam)
if [ -z "$CAMERA3_DEVICE" ]; then
    nonh264=($(detect_non_h264_cameras))
    if [ ${#nonh264[@]} -gt 0 ]; then
        CAMERA3_DEVICE="${nonh264[0]}"
        echo "Auto-detected Camera 3 (non-H264): $CAMERA3_DEVICE"
    fi
fi

# Common settings
# Use multicast for multiple receivers (3 readers + 2 OBS = 5 clients)
UDP_MULTICAST="239.255.0.1"
VIDEO_SIZE="1280x720"
FRAMERATE="60"
PKT_SIZE="1316"

# Log file and PID directories
# Note: /var/log/camera-streaming must be created with proper permissions:
#   sudo mkdir -p /var/log/camera-streaming /var/run/camera-streaming
#   sudo chown $USER:$USER /var/log/camera-streaming /var/run/camera-streaming

LOG_DIR="/var/log/camera-streaming"
LOG_FILE="$LOG_DIR/camera-streaming.log"

PID_DIR="/var/run/camera-streaming"
PID_CAMERA1="$PID_DIR/camera1.pid"
PID_CAMERA2="$PID_DIR/camera2.pid"
PID_CAMERA3="$PID_DIR/camera3.pid"

mkdir -p "$LOG_DIR" "$PID_DIR"

log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

stop_camera() {
    local pid_file=$1
    local camera_name=$2
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p "$pid" > /dev/null 2>&1; then
            log "Stopping $camera_name (PID: $pid)"
            kill "$pid"
            sleep 1
            if ps -p "$pid" > /dev/null 2>&1; then
                log "Force stopping $camera_name"
                kill -9 "$pid"
            fi
        fi
        rm -f "$pid_file"
    fi
}

start_camera1() {
    log "Starting Camera 1 streaming: $CAMERA1_DEVICE -> UDP:$PORT_CAMERA1"
    
    ffmpeg -f v4l2 \
           -input_format h264 \
           -video_size "$VIDEO_SIZE" \
           -framerate "$FRAMERATE" \
           -use_wallclock_as_timestamps 1 \
           -i "$CAMERA1_DEVICE" \
           -c:v copy \
           -bsf:v dump_extra \
           -fflags +genpts \
           -f mpegts \
           "udp://$UDP_MULTICAST:$PORT_CAMERA1?pkt_size=$PKT_SIZE" \
           >> "$LOG_FILE" 2>&1 &
    
    echo $! > "$PID_CAMERA1"
    log "Camera 1 started with PID: $(cat $PID_CAMERA1)"
}

start_camera2() {
    log "Starting Camera 2 streaming: $CAMERA2_DEVICE -> UDP:$PORT_CAMERA2"
    
    ffmpeg -f v4l2 \
           -input_format h264 \
           -video_size "$VIDEO_SIZE" \
           -framerate "$FRAMERATE" \
           -use_wallclock_as_timestamps 1 \
           -i "$CAMERA2_DEVICE" \
           -c:v copy \
           -bsf:v dump_extra \
           -fflags +genpts \
           -f mpegts \
           "udp://$UDP_MULTICAST:$PORT_CAMERA2?pkt_size=$PKT_SIZE" \
           >> "$LOG_FILE" 2>&1 &
    
    echo $! > "$PID_CAMERA2"
    log "Camera 2 started with PID: $(cat $PID_CAMERA2)"
}

start_camera3() {
    log "Starting Camera 3 streaming: $CAMERA3_DEVICE -> UDP:$PORT_CAMERA3 (encoding to H.264)"
    
    ffmpeg -f v4l2 \
           -video_size "$VIDEO_SIZE" \
           -framerate "$CAMERA3_FRAMERATE" \
           -i "$CAMERA3_DEVICE" \
           -c:v libx264 \
           -preset ultrafast \
           -tune zerolatency \
           -b:v 4M \
           -maxrate 4M \
           -bufsize 2M \
           -g 120 \
           -fflags +genpts \
           -f mpegts \
           "udp://$UDP_MULTICAST:$PORT_CAMERA3?pkt_size=$PKT_SIZE" \
           >> "$LOG_FILE" 2>&1 &
    
    echo $! > "$PID_CAMERA3"
    log "Camera 3 started with PID: $(cat $PID_CAMERA3)"
}

# Handle command line arguments
case "${1:-start}" in
    start)
        log "=== Starting camera streaming ==="
        
        # Stop any existing processes
        stop_camera "$PID_CAMERA1" "Camera 1"
        stop_camera "$PID_CAMERA2" "Camera 2"
        stop_camera "$PID_CAMERA3" "Camera 3"
        
        # Start Camera 1 if configured
        if [ -n "$CAMERA1_DEVICE" ]; then
            if [ ! -e "$CAMERA1_DEVICE" ]; then
                log "ERROR: Camera 1 device $CAMERA1_DEVICE not found"
                exit 1
            fi
            start_camera1
        else
            log "Camera 1 disabled (CAMERA1_DEVICE not set)"
        fi
        
        sleep 2
        
        # Start Camera 2 if configured
        if [ -n "$CAMERA2_DEVICE" ]; then
            if [ ! -e "$CAMERA2_DEVICE" ]; then
                log "ERROR: Camera 2 device $CAMERA2_DEVICE not found"
                exit 1
            fi
            start_camera2
        else
            log "Camera 2 disabled (CAMERA2_DEVICE not set)"
        fi
        
        sleep 2
        
        # Start Camera 3 if configured
        if [ -n "$CAMERA3_DEVICE" ]; then
            if [ ! -e "$CAMERA3_DEVICE" ]; then
                log "ERROR: Camera 3 device $CAMERA3_DEVICE not found"
                exit 1
            fi
            start_camera3
        else
            log "Camera 3 disabled (CAMERA3_DEVICE not set)"
        fi
        
        log "=== Camera streaming started ==="
        [ -n "$CAMERA1_DEVICE" ] && log "Camera 1: udp://@:$PORT_CAMERA1"
        [ -n "$CAMERA2_DEVICE" ] && log "Camera 2: udp://@:$PORT_CAMERA2"
        [ -n "$CAMERA3_DEVICE" ] && log "Camera 3: udp://@:$PORT_CAMERA3"
        ;;
    
    stop)
        log "=== Stopping camera streaming ==="
        stop_camera "$PID_CAMERA1" "Camera 1"
        stop_camera "$PID_CAMERA2" "Camera 2"
        stop_camera "$PID_CAMERA3" "Camera 3"
        log "=== Camera streaming stopped ==="
        ;;
    
    restart)
        "$0" stop
        sleep 2
        "$0" start
        ;;
    
    status)
        echo "Camera Streaming Status:"
        echo "========================"
        
        if [ -f "$PID_CAMERA1" ]; then
            pid=$(cat "$PID_CAMERA1")
            if ps -p "$pid" > /dev/null 2>&1; then
                echo "Camera 1: RUNNING (PID: $pid, UDP:$PORT_CAMERA1)"
            else
                echo "Camera 1: STOPPED (stale PID file)"
            fi
        else
            echo "Camera 1: STOPPED"
        fi
        
        if [ -f "$PID_CAMERA2" ]; then
            pid=$(cat "$PID_CAMERA2")
            if ps -p "$pid" > /dev/null 2>&1; then
                echo "Camera 2: RUNNING (PID: $pid, UDP:$PORT_CAMERA2)"
            else
                echo "Camera 2: STOPPED (stale PID file)"
            fi
        else
            echo "Camera 2: STOPPED"
        fi
        
        if [ -f "$PID_CAMERA3" ]; then
            pid=$(cat "$PID_CAMERA3")
            if ps -p "$pid" > /dev/null 2>&1; then
                echo "Camera 3: RUNNING (PID: $pid, UDP:$PORT_CAMERA3)"
            else
                echo "Camera 3: STOPPED (stale PID file)"
            fi
        else
            echo "Camera 3: STOPPED"
        fi
        
        echo ""
        echo "Log file: $LOG_FILE"
        ;;
    
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac
