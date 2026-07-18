from http.server import HTTPServer, BaseHTTPRequestHandler
import random
import json

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/api/numbers":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"number": random.randint(0, 99)}).encode())
        else:
            self.send_response(404)
            self.end_headers()

if __name__ == "__main__":
    HTTPServer(("0.0.0.0", 8084), Handler).serve_forever()