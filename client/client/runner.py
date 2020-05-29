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
import dnswrapper
import dnslib


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
        try:
            # Get the request data
            request_pkt = self.get_data()

            # Extract Query Information
            hostname, record_type = dnswrapper.extract_query(request_pkt)

            # Do RPC lookups
            dns_record_proto = self.grpc_client.request_dns_lookup(hostname)
            logging.debug('Proto response:', dns_record_proto)

            if dns_record_proto:
                # Our gRPC call succeeded, though we might not have
                # found any ip addresses.
                response_pkt = dnswrapper.generate_dns_packet(
                    request_pkt, hostname, dns_record_proto.ipAddresses)
            else:
                # Our gRPC call failed
                response_pkt = dnswrapper.generate_dns_packet(
                    request_pkt, hostname, [])

            # Useful to debug responses.
            # decoded = dnslib.DNSRecord.parse(response_pkt)
            # print(decoded)
            self.send_data(response_pkt)
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
    parser.add_argument("--timeout", default=client.DEFAULT_TIMEOUT_SECONDS,
                        help='The timeout in seconds for DNS lookup requests.')
    args = parser.parse_args()

    grpc_client = client.DnsClient(
        None, args.backend_ip, args.backend_port, args.timeout)
    server_info = ('', args.port)

    # Launch the server.
    with LocalDNSServer(grpc_client, server_info, DNSRequestHandler) as server:
        print('Serving...')
        server.serve_forever()
