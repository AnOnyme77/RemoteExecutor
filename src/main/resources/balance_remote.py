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

# fctCode

queue = Queue()
outputQueue = Queue()

class Receiver(BaseHTTPRequestHandler):
    def parse_POST(self):
        ctype, pdict = parse_header(self.headers['content-type'])
        if ctype == 'multipart/form-data':
            postvars = parse_multipart(self.rfile, pdict)
        elif ctype == 'application/x-www-form-urlencoded':
            length = int(self.headers['content-length'])
            postvars = parse_qs(
                self.rfile.read(length),
                keep_blank_values=1)
        else:
            postvars = {}
        return postvars

    def do_POST(self):
        args = self.parse_POST()
        queue.put(args["line"][0])
        self.send_response(200)

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
    def __init__(self):
        Thread.__init__(self)

    def run(self):
        while True:
            elem = queue.get()
            outputQueue.put(# fctCall)


if __name__ == '__main__':

    for i in range(20):
        t = Worker()
        t.daemon = True
        t.start()

    server_address = ('', 9001)
    httpd = HTTPServer(server_address, Receiver)
    httpd.serve_forever()

