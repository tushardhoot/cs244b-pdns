#! /usr/bin/env python3
"""
Runs and spins up a local DNS servers to intercept traffic from the
local system, and pass it to our backend.
"""
import argparse
import logging
import socketserver
import traceback
import client


logging.getLogger('').setLevel(logging.INFO)
DEFAULT_SERVER_IP = '127.0.0.1'
DEFAULT_SERVER_PORT = '8980'


class DNSRequestHandler(socketserver.BaseRequestHandler):
    """The RequestClass created each DNS request to process it."""

    def __init__(self, grpc_client, request, client_address, server):
        self.grpc_client = grpc_client
        socketserver.BaseRequestHandler.__init__(
            self, request, client_address, server)

    def get_data(self):
        return self.request[0].strip()

    def send_data(self, data):
        return self.request[1].sendto(data, self.client_address)

    def handle(self):
        print('Implement handling the DNS request...')
        try:
            #  TODO(kbaichoo): implement the handling of the DNS record /
            #  sending the correct records.
            dns_record_proto = self.grpc_client.request_dns_lookup(
                'walmart.com')
            str_results = str(dns_record_proto)
            print(str_results)
            self.send_data(str.encode(str_results))
        except Exception:
            traceback.print_exc()



class LocalDNSServer(socketserver.ThreadingMixIn, socketserver.UDPServer):
    def __init__(self, backend_client, server_address, RequestHandlerClass):
        # Overwrite the init class to accept the client.
        self.grpc_client = backend_client
        socketserver.TCPServer.__init__(
            self, server_address, RequestHandlerClass)

    def finish_request(self, request, client_address):
        # Init our RequestHandler
        self.RequestHandlerClass(
            self.grpc_client, request, client_address, self)


if __name__ == '__main__':
    # Parse CLI arguments.
    parser = argparse.ArgumentParser(description='Starts up the DNS client.')

    # Arguments for the P2P Backend Node
    parser.add_argument('--backend_port', default=DEFAULT_SERVER_PORT,
                        type=int,
                        help='The port your DNS Server is running on.')
    parser.add_argument('--backend_ip', default=DEFAULT_SERVER_IP,
                        help='The ip your DNS Server is running on.')

    # Arguments for the local DNS service
    parser.add_argument('--port', default=53, type=int,
                        help='The port to listen on for the local '
                        'UDP DNS Server.')
    args = parser.parse_args()

    grpc_client = client.DnsClient(None, args.backend_ip, args.backend_port)
    server_info = ('', args.port)

    # Launch the server.
    with LocalDNSServer(grpc_client, server_info, DNSRequestHandler) as server:
        print('Serving...')
        server.serve_forever()
