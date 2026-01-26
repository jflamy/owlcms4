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

# Configuration
# To disable a camera, comment out its DEVICE line below

CAMERA1_DEVICE="/dev/video0"
PORT_CAMERA1="9001"

#CAMERA2_DEVICE="/dev/video2"
PORT_CAMERA2="9002"

# Common settings
# Use multicast for multiple receivers (3 readers + 2 OBS = 5 clients)
UDP_MULTICAST="239.255.0.1"
VIDEO_SIZE="1920x1080"
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
           -i "$CAMERA1_DEVICE" \
           -c:v copy \
           -bsf:v dump_extra \
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
           -i "$CAMERA2_DEVICE" \
           -c:v copy \
           -bsf:v dump_extra \
           -f mpegts \
           "udp://$UDP_MULTICAST:$PORT_CAMERA2?pkt_size=$PKT_SIZE" \
           >> "$LOG_FILE" 2>&1 &
    
    echo $! > "$PID_CAMERA2"
    log "Camera 2 started with PID: $(cat $PID_CAMERA2)"
}

# Handle command line arguments
case "${1:-start}" in
    start)
        log "=== Starting camera streaming ==="
        
        # Stop any existing processes
        stop_camera "$PID_CAMERA1" "Camera 1"
        stop_camera "$PID_CAMERA2" "Camera 2"
        
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
        
        log "=== Camera streaming started ==="
        [ -n "$CAMERA1_DEVICE" ] && log "Camera 1: udp://@:$PORT_CAMERA1"
        [ -n "$CAMERA2_DEVICE" ] && log "Camera 2: udp://@:$PORT_CAMERA2"
        ;;
    
    stop)
        log "=== Stopping camera streaming ==="
        stop_camera "$PID_CAMERA1" "Camera 1"
        stop_camera "$PID_CAMERA2" "Camera 2"
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
        
        echo ""
        echo "Log file: $LOG_FILE"
        ;;
    
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac
