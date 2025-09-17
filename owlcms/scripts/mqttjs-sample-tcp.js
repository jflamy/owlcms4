import mqtt from 'mqtt'

// Use the 'tcp://' scheme by default (Paho style). Can be overridden by MQTT_BROKER.
const BROKER = process.env.MQTT_BROKER || 'tcp://127.0.0.1:1883'
const KEEPALIVE = parseInt(process.env.MQTT_KEEPALIVE, 10) || 60
const CLIENT_ID = `owlcms-test-client-${Math.random().toString(16).slice(2,10)}`

console.log(`Connecting to ${BROKER} keepalive= ${KEEPALIVE} s`)

const client = mqtt.connect(BROKER, {
  keepalive: KEEPALIVE,
  clientId: CLIENT_ID,
  reconnectPeriod: 0 // do not auto-reconnect for test clarity
})

client.on('connect', (connack) => {
  console.log('Connected, connack:', connack)

  // publish once then stay subscribed
  const payload = `config ${new Date().toISOString()}`
  client.publish('owlcms/config', payload, { qos: 0 }, (err) => {
    if (err) console.error('Publish error', err)
    else console.log('Published once to owlcms/config:', payload)
  })

  client.subscribe('#', (err, granted) => {
    if (err) console.error('Subscribe error', err)
    else console.log('Subscribed to # (all topics)')
  })
})

client.on('message', (topic, message) => {
  try {
    const msg = message ? message.toString() : ''
    console.log('Received', topic, msg)
  } catch (e) {
    console.log('Received', topic, '<binary>')
  }
})

client.on('packetsend', (packet) => {
  if (packet && packet.cmd) {
    if (packet.cmd === 'pingreq') console.log('packetsend: pingreq')
  }
})

client.on('packetreceive', (packet) => {
  if (packet && packet.cmd) {
    if (packet.cmd === 'pingresp') console.log('packetreceive: pingresp')
  }
})

client.on('close', () => console.log('Connection closed'))
client.on('error', (err) => console.error('Client error', err))

console.log('Client will wait for messages indefinitely; publish was sent once.')

// keep process alive
process.stdin.resume()
