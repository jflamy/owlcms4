# OBS Dual-Stream Architecture with Replay System

## System Overview

This document describes a dual-stream OBS setup with integrated replay system for live sports/events broadcasting. The system streams to both YouTube and an LED wall while providing instant replay capabilities.

## Network Topology

![Network Topology Diagram](Overview.svg)

The complete network topology diagram shows:
- **Competition Router**: Core network hub (192.168.1.x)
- **OWLCMS**: Master controller with MQTT broker for replay triggers and timing
- **Tracker**: OBS remote control and remote scoreboards server
- **Video Traffic Switch**: Dedicated switch for video streams (isolated network)
- **RPi 5** (192.168.1.42): Camera capture, replay system, and MQTT subscriber
- **Streaming OBS**: OBS streaming to YouTube (UDP inputs 9001, 9002)
- **LED Wall OBS**: OBS outputting Full HD HDMI to LED wall (HTTP replay inputs and scoreboards from OWLCMS)

## System Components

### Camera Setup
- **2x UVC PTZ Cameras**
  - Output: H.264 compressed @ 1080p60
  - Connection: USB 3.0 to RPi 5
  - Camera 1: `/dev/video0`
  - Camera 2: `/dev/video2`

### Replay System (RPi 5) - 192.168.1.42
**Hardware:**
- Raspberry Pi 5
- SSD storage for recordings
- 2x USB 3.0 ports for cameras

**Functions:**
1. Camera capture (H.264 from USB)
2. **Continuous UDP streaming** to both OBS laptops (FFmpeg #1 per camera)
3. **On-demand recording** from UDP stream (FFmpeg #2 per camera, triggered by MQTT)
4. Replay clipping system
5. MQTT subscriber for OWLCMS commands
6. HTTP server for replay MP4s

### Streaming OBS
**Hardware:**
- Gaming laptop with GTX 1050 (Dell 2017)
- 2x USB 3.0 ports
- Windows OS

**Software:**
- OBS Studio
- NVENC encoding (GPU)
- WebSocket plugin for remote control

**OBS Inputs:**
- Camera 1: `udp://@:9001`
- Camera 2: `udp://@:9002`

**OBS Output:**
- YouTube RTMP stream

### LED Wall OBS
**Hardware:**
- Gaming laptop with GTX 1050 (Dell 2017)
- 2x USB 3.0 ports
- Windows OS

**Software:**
- OBS Studio
- NVENC encoding (GPU)
- WebSocket plugin for remote control

**OBS Inputs:**
- Replay MP4 URLs from RPi 5
- Example: `http://192.168.1.42:8080/replay_001.mp4`

**OBS Output:**
- Full HD HDMI to LED Wall (1920x1080)

### OWLCMS - On-Venue Competition Management
**Functions:**
- Issues MQTT commands for:
  - Recording start/stop triggers
  - Replay clip timing
  - Idle time removal parameters
- Serves HTTP scoreboards to LED Wall OBS and Competition Router (for remote screens)
- Connects to Tracker via WebSocket for OBS remote control

### Tracker - Control Hub
**Functions:**
- OBS remote control via WebSocket API
- Remote scoreboards server
- Connects to OWLCMS via WebSocket
- Routes scoreboard data to Competition Router for internet-accessible displays

## Data Flow

### Video Streams (UDP)
- **Cameras** → RPi 5 via USB 3.0 (H.264 compressed @ 1080p60)
- **RPi 5** → Video Traffic Switch → Both OBS systems (UDP ports 9001, 9002)

### YouTube Upstream
- **Streaming OBS** → Video Traffic Switch → Competition Router → YouTube

### Replay System (HTTP)
- **RPi 5** → Both OBS systems (HTTP-served MP4 replay clips)
- Triggered by MQTT commands from OWLCMS

### Scoreboards (HTTP)
- **OWLCMS** → Competition Router (Local scoreboards to screens)
- **OWLCMS** → LED Wall OBS (LED wall overlays)
- **OWLCMS** → Tracker → Competition Router (Cloud scoreboards for remote access)

### Control (WebSocket & API)
- **OWLCMS** → Tracker (WebSocket connection for competition data)
- **Tracker** → Both OBS systems (API connections for scene control)

### MQTT Control
- **OWLCMS** → RPi 5 (direct MQTT connection)
- Commands include replay triggers, timing parameters, and idle time removal

## FFmpeg Commands

### Design Intent: Zero-Copy Architecture with Separate Streaming and Recording

The FFmpeg commands are designed for **minimal overhead and minimal latency**:

- **No re-encoding**: Cameras output H.264 directly; FFmpeg copies the encoded H.264 without decoding/re-encoding
- **Remuxing only**: FFmpeg only changes container format (muxing to MPEG-TS for streaming, demuxing/remuxing to MP4 for recording)
- **Zero CPU/GPU load**: No encoding/decoding means the RPi 5 CPU/GPU are free for other tasks (replay clipping, HTTP serving)
- **Minimal latency**: Direct copy from camera → network has <100ms delay (vs. 2-5 seconds with encoding)
- **Separate processes for streaming and recording**:
  - **FFmpeg #1** (per camera): Continuous UDP streaming from camera (always running)
  - **FFmpeg #2** (per camera): On-demand recording from UDP stream (started/stopped by MQTT commands)

**Architecture Rationale:**
- OBS requires **continuous video feed** - cannot have interruptions in UDP stream
- Replay system needs **on-demand recording** - starts when athlete prepares, stops after decision
- Reading UDP stream locally is more reliable than dual-reading the same USB camera

---

### Continuous UDP Streaming (Shell Script)

These FFmpeg processes are started by a shell script prior to starting the replay system and run continuously. They provide uninterrupted video feeds to OBS.

**Camera-to-UDP streaming parameters:**
- `-f v4l2`: Video4Linux2 input format (Linux camera API)
- `-input_format h264`: Specify H.264 input (camera outputs H.264-encoded video)
- `-video_size 1920x1080`: Resolution from camera
- `-framerate 60`: Frame rate from camera
- `-i /dev/video0`: Input device (camera)
- `-c:v copy`: Copy H.264-encoded video without decoding/re-encoding (zero CPU/GPU load)
- `-bsf:v dump_extra`: Bitstream filter that repeats H.264 SPS/PPS headers periodically, allowing OBS to start decoding immediately when it connects
- `-f mpegts`: Mux into MPEG Transport Stream container format (minimal overhead, ideal for streaming)
- `udp://192.168.1.255:9001`: UDP broadcast to port 9001
- `?pkt_size=1316`: Optimal UDP packet size for video

#### Camera 1 - UDP Streaming

```bash
# Always running - provides continuous feed to OBS systems
ffmpeg -f v4l2 -input_format h264 -video_size 1920x1080 -framerate 60 -i /dev/video0 \
  -c:v copy -bsf:v dump_extra -f mpegts udp://192.168.1.255:9001?pkt_size=1316
```

#### Camera 2 - UDP Streaming

```bash
# Always running - provides continuous feed to OBS systems
ffmpeg -f v4l2 -input_format h264 -video_size 1920x1080 -framerate 60 -i /dev/video2 \
  -c:v copy -bsf:v dump_extra -f mpegts udp://192.168.1.255:9002?pkt_size=1316
```

---

### On-Demand Recording (Replay System)

These FFmpeg processes are started and stopped by the replay system based on MQTT triggers from OWLCMS. Recording begins when an athlete is called and stops after the decision.

**UDP-to-recording parameters:**
- `-f mpegts`: Demux MPEG Transport Stream input (containing H.264-encoded video)
- `-i udp://127.0.0.1:9001`: Read from local UDP stream
- `-c:v copy`: Copy H.264-encoded video without decoding/re-encoding (remuxing only: MPEG-TS → MP4)
- `-an`: No audio (cameras don't provide audio)

#### Camera 1 - Recording

```bash
# Started/stopped by MQTT commands - records from local UDP stream (H.264 in MPEG-TS)
ffmpeg -f mpegts -i udp://127.0.0.1:9001 \
  -c:v copy -an /recordings/cam1.mp4
```

#### Camera 2 - Recording

```bash
# Started/stopped by MQTT commands - records from local UDP stream (H.264 in MPEG-TS)
ffmpeg -f mpegts -i udp://127.0.0.1:9002 \
  -c:v copy -an /recordings/cam2.mp4
```

---

**Architecture Benefits:**
1. **OBS continuity**: UDP streaming never stops, OBS always has video input
2. **Recording control**: Recording starts when athlete called, stops after decision
3. **Resource efficiency**: Single camera read (FFmpeg #1), recording reads from network stack (FFmpeg #2)
4. **Reliability**: UDP multicast allows multiple consumers without USB contention

## OBS Configuration

### Streaming OBS - YouTube Stream Scenes

Streaming OBS streams to YouTube with automatic scene switching based on competition state. The scene transitions are triggered by OWLCMS events via the display-control system.

#### Scene Flow (Competition Cycle)

| Competition State | Scene | Video Source | Overlay |
|-------------------|-------|--------------|---------|
| Athlete in waiting room | **Attempt Board** | Attempt board from OWLCMS | None |
| Athlete at chalk box | **Side Camera** | Side camera (UDP 9002) | Lower third (athlete name, team, attempt) |
| Athlete on platform | **Front Camera** | Center camera (UDP 9001) | None |
| Decision visible | **Front Camera** | Center camera (UDP 9001) | Lower third (decision result) |
| After decision | **Replay** | Replay MP4 from RPi 5 | None |
| After replay | **Scoreboard** | Scoreboard from OWLCMS | None |

#### Scene Configuration

**Scene 1: Attempt Board**
- Browser source: `http://<owlcms>:8080/displays/attemptBoard?fop=A`
- Used during: Wait time between athletes

**Scene 2: Side Camera + Lower Third**
- Media source: `udp://@:9002` (side camera)
- Browser source overlay: Lower third with athlete info from OWLCMS
- Used during: Athlete preparation at chalk box

**Scene 3: Front Camera**
- Media source: `udp://@:9001` (center camera)
- Used during: Athlete on platform, lift attempt

**Scene 4: Front Camera + Decision**
- Media source: `udp://@:9001` (center camera)
- Browser source overlay: Lower third with decision lights/result
- Used during: Decision display (2-3 seconds)

**Scene 5: Replay**
- Media source: Replay URL from RPi 5 (e.g., `http://192.168.1.42:8080/replay_001.mp4`)
- Updated dynamically by display-control system
- Used during: Instant replay after decision

**Scene 6: Scoreboard**
- Browser source: `http://<owlcms>:8080/displays/scoreboard?fop=A`
- Used during: Between athletes, session breaks

### Streaming OBS - Media Source Setup (UDP Cameras)

1. **Add Media Source**
   - Source → Add → Media Source
   - Uncheck "Local File"
   - Input: `udp://@:9001` (center camera) or `udp://@:9002` (side camera)
   - Network Buffering: 0-100ms (low latency)

2. **Add Browser Source (Scoreboards/Overlays)**
   - Source → Add → Browser Source
   - URL: OWLCMS display URL
   - Width: 1920, Height: 1080

### LED Wall OBS - LED Wall Display

LED Wall OBS provides a simplified Full HD HDMI feed for the venue LED wall, focused on replays and scoreboards.

| Content | Source |
|---------|--------|
| Scoreboard | Browser source from OWLCMS |
| Replay | HTTP MP4 from RPi 5 |

The LED wall typically shows the scoreboard with automatic replay insertion after each lift.