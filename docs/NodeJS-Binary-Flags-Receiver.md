# Binary Flags Receiver for Node.js

Implementation guide for receiving and extracting flag files from OWLCMS via WebSocket binary frames.

## Installation

```bash
npm install ws adm-zip
```

## Binary Frame Format

OWLCMS sends flags as a binary WebSocket frame with this structure:

```text
Bytes 0-3:    Type length (big-endian 32-bit int)
Bytes 4+:     Message type string (UTF-8)
Bytes 4+typeLength+: Binary payload (ZIP file)
```

Example for "flags" type with 100KB ZIP:

```text
[0x00, 0x00, 0x00, 0x05] [f,l,a,g,s] [100KB ZIP bytes...]
```

## Implementation Code

### Complete Handler Function

Copy this into your WebSocket message handler:

```javascript
const AdmZip = require('adm-zip');
const fs = require('fs');

function handleBinaryMessage(buffer) {
  try {
    // Validate minimum frame size
    if (buffer.length < 4) {
      console.error('ERROR: Binary frame too short (< 4 bytes)');
      return;
    }

    // Read type length as big-endian 32-bit integer
    const typeLength = buffer.readUInt32BE(0);

    // Validate frame contains complete type string
    if (buffer.length < 4 + typeLength) {
      console.error(`ERROR: Frame too short for type (need ${4 + typeLength}, got ${buffer.length})`);
      return;
    }

    // Extract message type (UTF-8 string)
    const messageType = buffer.slice(4, 4 + typeLength).toString('utf8');

    // Extract binary payload (everything after type)
    const payload = buffer.slice(4 + typeLength);

    console.log(`[BINARY] Received type="${messageType}", payload=${payload.length} bytes`);

    // Route to handler based on message type
    if (messageType === 'flags') {
      handleFlagsMessage(payload);
    } else if (messageType === 'pictures') {
      handlePicturesMessage(payload);
    } else {
      console.warn(`WARNING: Unknown binary message type "${messageType}"`);
    }
  } catch (error) {
    console.error('ERROR processing binary message:', error.message);
  }
}

function handleFlagsMessage(zipBuffer) {
  try {
    console.log(`[FLAGS] Processing ZIP archive (${zipBuffer.length} bytes)...`);

    // Parse ZIP from buffer
    const zip = new AdmZip(zipBuffer);
    const flagsDir = './local/flags';

    // Ensure target directory exists
    if (!fs.existsSync(flagsDir)) {
      fs.mkdirSync(flagsDir, { recursive: true });
      console.log(`[FLAGS] Created directory: ${flagsDir}`);
    }

    // Extract all files from ZIP
    let extractedCount = 0;
    zip.getEntries().forEach((entry) => {
      if (!entry.isDirectory) {
        const targetPath = `${flagsDir}/${entry.entryName}`;
        const parentDir = targetPath.split('/').slice(0, -1).join('/');

        // Create parent directory if needed
        if (!fs.existsSync(parentDir)) {
          fs.mkdirSync(parentDir, { recursive: true });
        }

        // Write file
        fs.writeFileSync(targetPath, entry.getData());
        console.log(`[FLAGS] Extracted: ${entry.entryName}`);
        extractedCount++;
      }
    });

    console.log(`[FLAGS] ✓ Successfully extracted ${extractedCount} flag files`);
  } catch (error) {
    console.error('[FLAGS] ERROR:', error.message);
  }
}

function handlePicturesMessage(zipBuffer) {
  try {
    console.log(`[PICTURES] Processing ZIP archive (${zipBuffer.length} bytes)...`);

    const zip = new AdmZip(zipBuffer);
    const picturesDir = './local/pictures';

    if (!fs.existsSync(picturesDir)) {
      fs.mkdirSync(picturesDir, { recursive: true });
    }

    let extractedCount = 0;
    zip.getEntries().forEach((entry) => {
      if (!entry.isDirectory) {
        const targetPath = `${picturesDir}/${entry.entryName}`;
        const parentDir = targetPath.split('/').slice(0, -1).join('/');

        if (!fs.existsSync(parentDir)) {
          fs.mkdirSync(parentDir, { recursive: true });
        }

        fs.writeFileSync(targetPath, entry.getData());
        extractedCount++;
      }
    });

    console.log(`[PICTURES] ✓ Successfully extracted ${extractedCount} picture files`);
  } catch (error) {
    console.error('[PICTURES] ERROR:', error.message);
  }
}
```

### Integration with WebSocket Server

```javascript
const WebSocket = require('ws');
const server = new WebSocket.Server({ port: 8080 });

server.on('connection', (ws) => {
  console.log('Client connected');

  ws.on('message', (data, isBinary) => {
    if (isBinary) {
      // Binary frame detected (opcode 0x2)
      handleBinaryMessage(data);
    } else {
      // Text frame detected (opcode 0x1) - JSON
      handleJsonMessage(data.toString());
    }
  });

  ws.on('error', (error) => {
    console.error('WebSocket error:', error);
  });

  ws.on('close', () => {
    console.log('Client disconnected');
  });
});

function handleJsonMessage(jsonString) {
  try {
    const message = JSON.parse(jsonString);
    console.log(`[JSON] Received type="${message.type}"`);
    // Handle JSON messages
  } catch (error) {
    console.error('[JSON] ERROR:', error.message);
  }
}
```

## Usage

1. Save the handler functions to a file (e.g., `binaryHandler.js`)
2. Import and use in your WebSocket message handler
3. Connect your OWLCMS instance to your WebSocket URL
4. When OWLCMS receives a 428 response with `"flags"` in missing array, it will send the binary frame
5. Your handler will automatically extract to `./local/flags`

## Testing

Create a test to verify the implementation:

```javascript
// Test: Create a fake binary frame and test the handler
const testZip = new AdmZip();
testZip.addFile('CAN.svg', Buffer.from('<svg>...</svg>'));
testZip.addFile('USA.png', Buffer.from([...png bytes...]));

const zipBuffer = testZip.toBuffer();

// Build binary frame: type_length + type_string + zip_bytes
const typeString = 'flags';
const frame = Buffer.allocUnsafe(4 + typeString.length + zipBuffer.length);
frame.writeUInt32BE(typeString.length, 0);
frame.write(typeString, 4, 'utf8');
zipBuffer.copy(frame, 4 + typeString.length);

// Test the handler
handleBinaryMessage(frame);
// Should extract files to ./local/flags
```

## Key Points

- **Check `isBinary` parameter**: Always distinguish between text (JSON) and binary frames
- **Big-endian format**: Use `readUInt32BE()` for the type length (network byte order)
- **Validate frame size**: Always verify sufficient bytes exist before reading
- **Directory creation**: Ensure parent directories are created before extracting files
- **Error handling**: Wrap in try-catch, ZIP parsing can fail with corrupted data
- **ZIP extraction**: Use `adm-zip` to parse and extract ZIP archives in-memory

## Troubleshooting

### Binary frame too short

- Frame is corrupted or incomplete
- Check network connection stability

### Frame too short for type

- Type length indicates more bytes than available
- Verify complete frame was received

### Error extracting flags

- ZIP data is corrupted
- Verify OWLCMS is creating valid ZIP archives

### Unknown binary message type

- OWLCMS is sending a different message type
- Check if it might be "pictures", "styles", etc.
- Add handlers for other message types as needed

## Directory Structure

After successfully receiving flags, your directory will look like:

```text
./local/flags/
├── CAN.svg
├── USA.png
├── BRA.svg
└── ... (all flag images)
```

Flags are organized by IOC country code or team name, exactly as sent from OWLCMS.
