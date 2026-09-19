#!/usr/bin/env python3
"""Receive one JSON export from the VS Code browser scraper and exit."""
from __future__ import annotations

import argparse
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("output", type=Path)
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()

    output = args.output.expanduser().resolve()
    output.parent.mkdir(parents=True, exist_ok=True)

    class Handler(BaseHTTPRequestHandler):
        def do_POST(self) -> None:
            length = int(self.headers.get("Content-Length", "0"))
            output.write_bytes(self.rfile.read(length))
            self.send_response(204)
            self.end_headers()

        def log_message(self, format: str, *values: object) -> None:
            return

    server = HTTPServer(("127.0.0.1", args.port), Handler)
    print(f"Waiting for browser records on http://127.0.0.1:{args.port}")
    server.handle_request()
    print(f"Wrote {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
