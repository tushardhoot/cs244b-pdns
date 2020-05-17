#! /usr/bin/env python3
"""
Tests for the DNS wrapper module.
"""
import unittest
from test_server import DomainLookupServicer, SERVER_PORT
import grpc
from concurrent import futures
import domain_lookup_pb2
import domain_lookup_pb2_grpc
import client


class TestClient(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
        domain_lookup_pb2_grpc.add_DomainLookupServiceServicer_to_server(
            StrictDomainLookupServicer(), server)
        server.add_insecure_port('localhost:{}'.format(SERVER_PORT))
        server.start()
        cls._server = server

    @classmethod
    def tearDownClass(cls):
        cls._server.stop(None)  # terminate server immediately.

    def test_grpc_success(self):
        # Tests that the grpc client can connect correctly and issue an RPC
        grpc_client = client.DnsClient(None, 'localhost', SERVER_PORT)
        response = grpc_client.request_dns_lookup('walmart.com')
        self.assertIsNotNone(response)

    def test_grpc_failure(self):
        # Tests failure, via an error in the gRPC status.
        # This should test for any gRPC error even though the one raised
        # INVALID_ARGUMENT.
        grpc_client = client.DnsClient(None, 'localhost', SERVER_PORT)
        response = grpc_client.request_dns_lookup('DoesNotExist.Mapping.Foo')
        self.assertIsNone(response)


class StrictDomainLookupServicer(DomainLookupServicer):
    # Raises an error if the mapping isn't found
    # Couldn't find an easy way to mock out the service

    def GetDomain(self, request, context):
        hostname = request.hostName
        ip_addresses = []

        if hostname in DomainLookupServicer.lookup_table:
            ip_addresses = DomainLookupServicer.lookup_table[hostname]

        if not ip_addresses:
            context.set_details('Mapping not found')
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            return domain_lookup_pb2.DNSRecord()

        return domain_lookup_pb2.DNSRecord(
            hostName=hostname,
            ipAddresses=ip_addresses)


if __name__ == '__main__':
    unittest.main()
