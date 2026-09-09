import argparse
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--port", type=int, default=8766)
    return parser.parse_args()


def main():
    args = parse_args()
    output = args.output.expanduser().resolve()
    output.mkdir(parents=True, exist_ok=True)

    class Handler(BaseHTTPRequestHandler):
        def do_OPTIONS(self):
            self.send_response(204)
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Access-Control-Allow-Methods", "POST, OPTIONS")
            self.send_header("Access-Control-Allow-Headers", "Content-Type, X-Filename")
            self.end_headers()

        def do_POST(self):
            filename = Path(self.headers.get("X-Filename", "download.xlsx")).name
            length = int(self.headers.get("Content-Length", "0"))
            data = self.rfile.read(length)
            target = output / filename
            target.write_bytes(data)
            self.send_response(204)
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            print(f"{target}: {len(data)} bytes", flush=True)

        def log_message(self, format, *args):
            pass

    print(f"Listening on http://127.0.0.1:{args.port}; writing to {output}", flush=True)
    HTTPServer(("127.0.0.1", args.port), Handler).serve_forever()


if __name__ == "__main__":
    main()
