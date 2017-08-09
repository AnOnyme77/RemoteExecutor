from threading import Thread
from Queue import Queue
import json
from BaseHTTPServer import HTTPServer
from sys import version as python_version
from cgi import parse_header, parse_multipart

if python_version.startswith('3'):
    from urllib.parse import parse_qs
    from http.server import BaseHTTPRequestHandler
else:
    from urlparse import parse_qs
    from BaseHTTPServer import BaseHTTPRequestHandler

# file

outputQueue = Queue()

class Receiver(BaseHTTPRequestHandler):
    def do_GET(self):
        cont = True
        items = []
        while(cont):
            try:
                items.append(str(outputQueue.get(False)))
            except:
                cont = False
        self.send_response(200)
        self.end_headers()
        self.wfile.write(json.dumps(items))

    def log_message(self, format, *args):
        return


class Worker(Thread):
    def __init__(self, queue):
        Thread.__init__(self)
        self.queue = queue

    def run(self):
        print("main")
        self.queue.put("#!#/#%END%#\#!#")


if __name__ == '__main__':
    t = Worker(outputQueue)
    t.daemon = True
    t.start()

    server_address = ('', 9000)
    httpd = HTTPServer(server_address, Receiver)
    httpd.serve_forever()
