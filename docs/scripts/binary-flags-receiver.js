// Quick Reference: OWLCMS Binary Flags Receiver
// Copy this function directly into your Node.js WebSocket handler

const AdmZip = require('adm-zip');
const fs = require('fs');

// Add this to your WebSocket message handler
ws.on('message', (data, isBinary) => {
  if (isBinary) {
    handleBinaryMessage(data);
  } else {
    handleJsonMessage(data.toString());
  }
});

// ============================================================
// COPY FROM HERE
// ============================================================

function handleBinaryMessage(buffer) {
  try {
    if (buffer.length < 4) {
      console.error('ERROR: Binary frame too short (< 4 bytes)');
      return;
    }

    const typeLength = buffer.readUInt32BE(0);

    if (buffer.length < 4 + typeLength) {
      console.error(`ERROR: Frame too short for type (need ${4 + typeLength}, got ${buffer.length})`);
      return;
    }

    const messageType = buffer.slice(4, 4 + typeLength).toString('utf8');
    const payload = buffer.slice(4 + typeLength);

    console.log(`[BINARY] Received type="${messageType}", payload=${payload.length} bytes`);

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

    const zip = new AdmZip(zipBuffer);
    const flagsDir = './local/flags';

    if (!fs.existsSync(flagsDir)) {
      fs.mkdirSync(flagsDir, { recursive: true });
      console.log(`[FLAGS] Created directory: ${flagsDir}`);
    }

    let extractedCount = 0;
    zip.getEntries().forEach((entry) => {
      if (!entry.isDirectory) {
        const targetPath = `${flagsDir}/${entry.entryName}`;
        const parentDir = targetPath.split('/').slice(0, -1).join('/');

        if (!fs.existsSync(parentDir)) {
          fs.mkdirSync(parentDir, { recursive: true });
        }

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

// ============================================================
// COPY TO HERE
// ============================================================

function handleJsonMessage(jsonString) {
  try {
    const message = JSON.parse(jsonString);
    console.log(`[JSON] Received type="${message.type}"`);
    // Your existing JSON handling code here
  } catch (error) {
    console.error('[JSON] ERROR:', error.message);
  }
}
