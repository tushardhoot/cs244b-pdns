#! /usr/bin/env python3
"""
A simple server in python supporting the rpc call (just to test changes)
"""
from concurrent import futures
import argparse
import logging
import grpc
import domain_lookup_pb2
import domain_lookup_pb2_grpc

logging.getLogger('').setLevel(logging.INFO)
INSECURE_SERVER_PORT = 8980
SECURE_SERVER_PORT = 7892


class DomainLookupServicer(domain_lookup_pb2_grpc.DomainLookupServiceServicer):

    # Contains all known mappings by the server.
    lookup_table = {
        'kevinbaichoo.com': ['104.236.159.189'],
        'walmart.com': ['8.8.8.1']
    }

    def GetDomain(self, request, context):
        logging.info('Received Request:{}'.format(request))
        hostname = request.hostName
        ip_addresses = []

        if hostname in DomainLookupServicer.lookup_table:
            ip_addresses = DomainLookupServicer.lookup_table[hostname]

        return domain_lookup_pb2.DNSRecord(
            hostName=hostname,
            ipAddresses=ip_addresses)


if __name__ == '__main__':

    # Parse CLI arguments.
    parser = argparse.ArgumentParser(
        description='Start a simple server compatible with our backend.')
    parser.add_argument('--server_pem',
                        help='Path to the server pem file used to establish a '
                        'secure channel (otherwise the channel is insecure.')
    parser.add_argument('--server_key',
                        help='Path to the server private key')
    parser.add_argument('--require_auth', default=False, action='store_true',
                        help='Whether we require the client to auth')

    args = parser.parse_args()

    # Both either provided or neither is provided.
    assert((args.server_pem and args.server_key) or (
        not args.server_pem and not args.server_key))

    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    domain_lookup_pb2_grpc.add_DomainLookupServiceServicer_to_server(
        DomainLookupServicer(), server)

    ports = [INSECURE_SERVER_PORT]

    if args.server_pem:
        ports.append(SECURE_SERVER_PORT)

        # Read private key, chain and secure port
        with open(args.server_key, 'rb') as f:
            pk = f.read()
        with open(args.server_pem, 'rb') as f:
            chain = f.read()
        if args.require_auth:
            # Require client to auth with same pk / cert as server.
            creds = grpc.ssl_server_credentials([(pk, chain)], chain, True)
        else:
            creds = grpc.ssl_server_credentials([(pk, chain)])
        server.add_secure_port(
            'localhost:{}'.format(SECURE_SERVER_PORT), creds)

    server.add_insecure_port('localhost:{}'.format(INSECURE_SERVER_PORT))
    server.start()
    logging.info('Server starting on ports {}'.format(ports))
    server.wait_for_termination()
