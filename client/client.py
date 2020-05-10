#! /usr/bin/env python3
"""
Client for the P2P DomainLookupSerivce serivce.

Can use this as a standalone to make calls to the
P2P DomainLookupService's server or more likely as a library abstracting
the logic of communicating to the P2P DomainLookupService backend.
"""
import argparse
import logging
import grpc
import domain_lookup_pb2
import domain_lookup_pb2_grpc

DEFAULT_SERVER_IP = '127.0.0.1'
DEFAULT_SERVER_PORT = '8980'


class DnsClient:
    def __init__(self, dns_translator, server_ip, server_port):
        self.translator = dns_translator
        self.server_ip = server_ip
        self.server_port = server_port

        # The channel used to communicate with the DNS server.
        logging.info(
            'Connecting to backend on {}:{}'.format(
                self.server_ip, self.server_port))
        # Note this is thread safe.
        self.channel = grpc.insecure_channel(
            '{}:{}'.format(self.server_ip, self.server_port))

    def request_dns_lookup(self, domain_name):
        """
        Makes an RPC call to our DNS Server.

        Note this is a blocking call.

        Args:
         - domain_name: a string containing the name of the domain to lookup.

        Returns the resulting protobuf on success.
        """
        stub = domain_lookup_pb2_grpc.DomainLookupServiceStub(self.channel)
        request = domain_lookup_pb2.HostName(name=domain_name)
        logging.info(
            'Sending Request for domain: {}'.format(domain_name))
        return stub.GetDomain(request)


if __name__ == '__main__':
    # Turn on logging
    logging.getLogger('').setLevel(logging.INFO)

    # Parse CLI arguments.
    parser = argparse.ArgumentParser(description='Starts up the DNS client.')
    parser.add_argument('--backend_port', default=DEFAULT_SERVER_PORT,
                        type=int,
                        help='The port your DNS Server is running on.')
    parser.add_argument('--backend_ip', default=DEFAULT_SERVER_IP,
                        help='The ip your DNS Server is running on.')
    args = parser.parse_args()

    # Create the client object and test out some domains.
    client = DnsClient(None, args.backend_ip, args.backend_port)
    print(client.request_dns_lookup('walmart.com'))
    print(client.request_dns_lookup('facebook.com'))