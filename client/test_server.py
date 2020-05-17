#! /usr/bin/env python3
"""
Test Server in python supporting the rpc call (just to test changes)
"""
from concurrent import futures
import logging
import grpc
import domain_lookup_pb2
import domain_lookup_pb2_grpc

logging.getLogger('').setLevel(logging.INFO)
SERVER_PORT = 8980


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
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    domain_lookup_pb2_grpc.add_DomainLookupServiceServicer_to_server(
        DomainLookupServicer(), server)
    server.add_insecure_port('localhost:{}'.format(SERVER_PORT))
    server.start()
    logging.info('Server starting on port {}'.format(SERVER_PORT))
    server.wait_for_termination()
