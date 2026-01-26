#!/bin/bash
#
# start-cameras-windows.sh - Start continuous UDP streaming from UVC cameras (Windows/Git Bash)
#
# This script starts FFmpeg processes that capture H.264 video from UVC cameras
# and stream it via UDP to OBS systems. These processes run continuously and
# should be started before the replay system.
#
# No GPU is needed since we're just copying the H.264 stream from the camera
# (the camera does all the encoding).
#
# Usage:
#   ./start-cameras-windows.sh [start|stop|restart|status]
#
# Note: Run this from Git Bash on Windows

set -e

# Configuration
# To disable a camera, comment out its NAME line below

CAMERA1_NAME="UVC Camera"
PORT_CAMERA1="9001"

#CAMERA2_NAME="UVC Camera 2"
PORT_CAMERA2="9002"

# Common settings
# Use multicast for multiple receivers (replay readers + OBS clients)
UDP_MULTICAST="239.255.0.1"
VIDEO_SIZE="1280x720"
FRAMERATE="60"
PKT_SIZE="1316"
INPUT_FORMAT="dshow"

# Log and PID files (in current directory for Windows simplicity)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$SCRIPT_DIR"
LOG_FILE="$LOG_DIR/camera-streaming.log"

PID_DIR="$SCRIPT_DIR"
PID_CAMERA1="$PID_DIR/camera1.pid"
PID_CAMERA2="$PID_DIR/camera2.pid"

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
            kill "$pid" 2>/dev/null || true
            sleep 1
            if ps -p "$pid" > /dev/null 2>&1; then
                log "Force stopping $camera_name"
                kill -9 "$pid" 2>/dev/null || true
            fi
        fi
        rm -f "$pid_file"
    fi
}

start_camera1() {
    log "Starting Camera 1 streaming: $CAMERA1_NAME -> UDP:$PORT_CAMERA1"
    
    # DirectShow doesn't support H.264 passthrough like Linux V4L2
    # Must re-encode - using Intel QuickSync for hardware acceleration
    # 720p60 for manageable bitrate with low latency
    ffmpeg -f "$INPUT_FORMAT" \
           -rtbufsize 100M \
           -video_size "$VIDEO_SIZE" \
           -framerate "$FRAMERATE" \
           -i video="$CAMERA1_NAME" \
           -c:v h264_qsv \
           -preset veryfast \
           -async_depth 1 \
           -look_ahead 0 \
           -g 30 \
           -bf 0 \
           -b:v 6M \
           -maxrate 8M \
           -bufsize 1M \
           -an \
           -f mpegts \
           -muxdelay 0 \
           -muxpreload 0 \
           -flush_packets 1 \
           "udp://$UDP_MULTICAST:$PORT_CAMERA1?pkt_size=$PKT_SIZE" \
           >> "$LOG_FILE" 2>&1 &
    
    echo $! > "$PID_CAMERA1"
    log "Camera 1 started with PID: $(cat $PID_CAMERA1)"
}

start_camera2() {
    log "Starting Camera 2 streaming: $CAMERA2_NAME -> UDP:$PORT_CAMERA2"
    
    # DirectShow doesn't support H.264 passthrough like Linux V4L2
    # Must re-encode - using Intel QuickSync for hardware acceleration
    # 720p60 for manageable bitrate with low latency
    ffmpeg -f "$INPUT_FORMAT" \
           -rtbufsize 100M \
           -video_size "$VIDEO_SIZE" \
           -framerate "$FRAMERATE" \
           -i video="$CAMERA2_NAME" \
           -c:v h264_qsv \
           -preset veryfast \
           -async_depth 1 \
           -look_ahead 0 \
           -g 30 \
           -bf 0 \
           -b:v 6M \
           -maxrate 8M \
           -bufsize 1M \
           -an \
           -f mpegts \
           -muxdelay 0 \
           -muxpreload 0 \
           -flush_packets 1 \
           "udp://$UDP_MULTICAST:$PORT_CAMERA2?pkt_size=$PKT_SIZE" \
           >> "$LOG_FILE" 2>&1 &
    
    echo $! > "$PID_CAMERA2"
    log "Camera 2 started with PID: $(cat $PID_CAMERA2)"
}

# Handle command line arguments
case "${1:-start}" in
    start)
        log "=== Starting camera streaming (Windows) ==="
        
        # Stop any existing processes
        stop_camera "$PID_CAMERA1" "Camera 1"
        stop_camera "$PID_CAMERA2" "Camera 2"
        
        # Start Camera 1 if configured
        if [ -n "$CAMERA1_NAME" ]; then
            start_camera1
        else
            log "Camera 1 disabled (CAMERA1_NAME not set)"
        fi
        
        sleep 2
        
        # Start Camera 2 if configured
        if [ -n "$CAMERA2_NAME" ]; then
            start_camera2
        else
            log "Camera 2 disabled (CAMERA2_NAME not set)"
        fi
        
        log "=== Camera streaming started ==="
        [ -n "$CAMERA1_NAME" ] && log "Camera 1: udp://@$UDP_MULTICAST:$PORT_CAMERA1"
        [ -n "$CAMERA2_NAME" ] && log "Camera 2: udp://@$UDP_MULTICAST:$PORT_CAMERA2"
        echo ""
        echo "Test stream with low-latency playback:"
        [ -n "$CAMERA1_NAME" ] && echo "  ffplay -fflags nobuffer -flags low_delay -probesize 32 -analyzeduration 0 udp://@$UDP_MULTICAST:$PORT_CAMERA1"
        [ -n "$CAMERA2_NAME" ] && echo "  ffplay -fflags nobuffer -flags low_delay -probesize 32 -analyzeduration 0 udp://@$UDP_MULTICAST:$PORT_CAMERA2"
        echo ""
        echo "In OBS, add Media Source with URL: udp://@$UDP_MULTICAST:$PORT_CAMERA1"
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
