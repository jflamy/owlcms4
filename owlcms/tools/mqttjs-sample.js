// Simple mqtt.js WebSocket client sample for testing keepalive
// Usage:
//   npm install mqtt
//   node mqttjs-sample.js

const mqtt = require('mqtt');

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
  // subscribe to a test topic
  client.subscribe('owlcms/test', { qos: 0 }, function (err) {
    if (err) console.error('Subscribe error', err);
    else console.log('Subscribed to owlcms/test');
  });

  // publish a test message every 20s so you can observe traffic
  setInterval(() => {
    const msg = 'ping ' + new Date().toISOString();
    client.publish('owlcms/test', msg, { qos: 0 }, (e) => {
      if (e) console.error('Publish error', e);
      else console.log('Published:', msg);
    });
  }, 20000);
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
