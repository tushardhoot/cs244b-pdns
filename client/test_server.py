#! /usr/bin/env python3
"""
Test Server in python supporting the rpc call (just to test changes)
"""
from concurrent import futures
import logging
import time
import grpc
import domain_lookup_pb2
import domain_lookup_pb2_grpc

logging.getLogger('').setLevel(logging.INFO)
SERVER_PORT = 8980


class DomainLookupServicer(domain_lookup_pb2_grpc.DomainLookupServiceServicer):
    def GetDomain(self, request, context):
        record = domain_lookup_pb2.DNSRecord(
            hostName='test.com', ipAddresses=['0.0.0.0'])
        time.sleep(1)
        return record


if __name__ == '__main__':
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    domain_lookup_pb2_grpc.add_DomainLookupServiceServicer_to_server(
        DomainLookupServicer(), server)
    server.add_insecure_port('localhost:{}'.format(SERVER_PORT))
    server.start()
    logging.info('Server starting on port {}'.format(SERVER_PORT))
    server.wait_for_termination()
