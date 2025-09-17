// Simple mqtt.js WebSocket client sample for testing keepalive
// Usage:
//   npm install mqtt
//   node mqttjs-sample.js

import mqtt from 'mqtt';

const options = {
  // For MQTT over WebSocket, specify the ws:// URL
  protocol: 'ws',
  hostname: '127.0.0.1',
  port: 8080,
  path: '/mqtt',
  // Keep alive in seconds
  keepalive: 60,
  // Client id for easier debugging
  clientId: 'owlcms-test-client-' + Math.random().toString(16).substr(2, 8),
  // Reconnect behaviour
  reconnectPeriod: 2000,
  connectTimeout: 30 * 1000
};

// Build full URL for mqtt.connect
const url = `ws://${options.hostname}:${options.port}${options.path}`;

console.log('Connecting to', url, 'keepalive=', options.keepalive, 's');

const client = mqtt.connect(url, options);

client.on('connect', function (connack) {
  console.log('Connected, connack:', connack ? connack : '<no connack>');
  // subscribe to all topics to observe broker replies
  client.subscribe('#', { qos: 0 }, function (err) {
    if (err) console.error('Subscribe error', err);
    else console.log('Subscribed to # (all topics)');
  });

  // publish a single test message once after connecting
  const msg = 'config ' + new Date().toISOString();
  client.publish('owlcms/config', msg, { qos: 0 }, (e) => {
    if (e) console.error('Publish error', e);
    else console.log('Published once to owlcms/config:', msg);
  });
});

client.on('message', function (topic, message) {
  console.log('Received', topic, message.toString());
});

client.on('close', function () {
  console.log('Connection closed');
});

client.on('reconnect', function () {
  console.log('Reconnecting...');
});

client.on('offline', function () {
  console.log('Client offline');
});

client.on('error', function (err) {
  console.error('Client error', err && err.message ? err.message : err);
});

// Log MQTT packets (packetsend/packetreceive) so keepalive PINGs are visible
client.on && client.on('packetsend', (packet) => {
  if (packet && packet.cmd) {
    if (packet.cmd === 'pingreq' || packet.cmd === 'pingresp') {
      console.log('packetsend:', packet.cmd);
    }
  }
});
client.on && client.on('packetreceive', (packet) => {
  if (packet && packet.cmd) {
    if (packet.cmd === 'pingreq' || packet.cmd === 'pingresp') {
      console.log('packetreceive:', packet.cmd);
    }
  }
});

console.log('Client will wait for messages indefinitely; publish was sent once.');

// mqtt.js emits "packetsend" and "packetreceive" events only on the internal stream
// but we can tap into outgoing/incoming packets via the client stream for debugging (advanced)
try {
  const stream = client.stream; // native websocket stream
  if (stream && stream.on) {
    stream.on('data', (d) => {
      // raw bytes received from socket; not parsed here
    });
  }
} catch (e) {
  // ignore
}

// Keep the process alive
process.stdin.resume();
