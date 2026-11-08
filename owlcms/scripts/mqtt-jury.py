#!/usr/bin/env python3
"""Send an owlcms jury decision over MQTT. Test tool, no external dependencies.

Publishes on the jury keypad topics listened to by MQTTMonitor:
  owlcms/jurybox/juryMember/decision/<platform>  payload: "<member 1-5> good|bad"
  owlcms/jurybox/decision/<platform>             payload: "good|bad"   (jury console)

Examples:
  ./mqtt-jury.py 3 good          # jury member 3 votes good lift on platform A
  ./mqtt-jury.py 1 bad -p B      # jury member 1 votes bad lift on platform B
  ./mqtt-jury.py --console good  # jury console "Good Lift" button
  ./mqtt-jury.py --random        # random good/bad from every jury member (default 4 members, see -n)
"""
import argparse
import os
import random
import socket
import sys

DEFAULT_HOST = "127.0.0.1"
DEFAULT_PORT = 1883


def encode_string(s):
    b = s.encode("utf-8")
    return len(b).to_bytes(2, "big") + b


def encode_remaining_length(n):
    out = bytearray()
    while True:
        byte = n % 128
        n //= 128
        if n > 0:
            byte |= 0x80
        out.append(byte)
        if n == 0:
            return bytes(out)


def mqtt_publish(host, port, messages, username=None, password=None):
    client_id = f"mqtt-jury-test-{os.getpid()}"
    flags = 0x02  # clean session
    if password is not None:
        flags |= 0x40
    if username is not None:
        flags |= 0x80
    variable = encode_string("MQTT") + b"\x04" + bytes([flags]) + (60).to_bytes(2, "big")
    payload_bytes = encode_string(client_id)
    if username is not None:
        payload_bytes += encode_string(username)
    if password is not None:
        payload_bytes += encode_string(password)
    connect = b"\x10" + encode_remaining_length(len(variable) + len(payload_bytes)) + variable + payload_bytes

    publishes = []
    for topic, payload in messages:
        publish_variable = encode_string(topic)
        publish_payload = payload.encode("utf-8")
        publishes.append(b"\x30" + encode_remaining_length(len(publish_variable) + len(publish_payload))
                         + publish_variable + publish_payload)

    with socket.create_connection((host, port), timeout=5) as sock:
        sock.sendall(connect)
        connack = sock.recv(4)
        if len(connack) < 4 or connack[0] >> 4 != 2 or connack[3] != 0:
            code = connack[3] if len(connack) >= 4 else "no response"
            raise SystemExit(f"MQTT connection refused by broker (CONNACK code {code})")
        for publish in publishes:
            sock.sendall(publish)
        sock.sendall(b"\xe0\x00")  # DISCONNECT


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("words", nargs="*", metavar="[MEMBER] DECISION",
                        help="member number (1-5) and good|bad; only good|bad with --console; nothing with --random")
    parser.add_argument("--console", action="store_true",
                        help="send the jury console decision instead of a member decision")
    parser.add_argument("--random", action="store_true",
                        help="send a random good/bad for every jury member")
    parser.add_argument("-n", "--size", type=int, default=4, help="jury size used by --random, default 4")
    parser.add_argument("-p", "--platform", default="A", help="platform (FOP) name, default A")
    parser.add_argument("-H", "--host", default=DEFAULT_HOST, help=f"MQTT broker host, default {DEFAULT_HOST}")
    parser.add_argument("-P", "--port", type=int, default=DEFAULT_PORT, help=f"MQTT broker port, default {DEFAULT_PORT}")
    parser.add_argument("-u", "--username", default=None, help="MQTT username (if configured in owlcms)")
    parser.add_argument("-w", "--password", default=None,
                        help="MQTT password (if configured in owlcms); may also come from MQTT_PASSWORD env var")
    args = parser.parse_args()

    member_topic = f"owlcms/jurybox/juryMember/decision/{args.platform}"
    words = args.words
    if args.random:
        if words:
            parser.error("--random takes no member or decision")
        messages = [(member_topic, f"{n} {random.choice(['good', 'bad'])}") for n in range(1, args.size + 1)]
    elif args.console:
        if len(words) != 1 or words[0] not in ("good", "bad"):
            parser.error("--console requires exactly one argument: good or bad")
        messages = [(f"owlcms/jurybox/decision/{args.platform}", words[0])]
    else:
        if len(words) != 2 or not words[0].isdigit() or not 1 <= int(words[0]) <= 5 or words[1] not in ("good", "bad"):
            parser.error("expected: MEMBER(1-5) good|bad")
        messages = [(member_topic, f"{words[0]} {words[1]}")]

    password = args.password or os.environ.get("MQTT_PASSWORD")
    try:
        mqtt_publish(args.host, args.port, messages, args.username, password)
    except (OSError, SystemExit) as e:
        print(f"error: {e}", file=sys.stderr)
        sys.exit(1)
    for topic, payload in messages:
        print(f"published {topic} : {payload}")


if __name__ == "__main__":
    main()
