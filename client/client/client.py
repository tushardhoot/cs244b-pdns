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

DEFAULT_SERVER_IP = 'localhost'
DEFAULT_SERVER_PORT = '8980'
DEFAULT_SECURE_SERVER_PORT = '7892'
DEFAULT_TIMEOUT_SECONDS = 5


class DnsClient:
    def __init__(self, server_ip, server_port,
                 timeout=DEFAULT_TIMEOUT_SECONDS, pem=None):
        self.server_ip = server_ip
        self.server_port = server_port
        self.timeout = timeout
        self.pem = pem

        # The channel used to communicate with the DNS server.
        logging.info(
            'Connecting to backend on {}:{}'.format(
                self.server_ip, self.server_port))
        server_tuple = '{}:{}'.format(self.server_ip, self.server_port)

        # The channels are thread safe.
        # Create a secure channel if the users specifies a pem.
        if self.pem:
            creds = grpc.ssl_channel_credentials(self.pem)
            self.channel = grpc.secure_channel(server_tuple, creds)
        else:
            self.channel = grpc.insecure_channel(server_tuple)

    def request_dns_lookup(self, domain_name):
        """
        Makes an RPC call to our DNS Server.

        Note this is a blocking call.

        Args:
         - domain_name: a string containing the name of the domain to lookup.

        Returns the resulting protobuf on success or None if the GRPC
        call encountered errors.
        """
        stub = domain_lookup_pb2_grpc.DomainLookupServiceStub(self.channel)
        request = domain_lookup_pb2.Message(hostName=domain_name)
        logging.info(
            'Sending Request for domain: {}'.format(domain_name))
        try:
            return stub.GetDomain(request, timeout=self.timeout)
        except grpc.RpcError as e:
            status_code = e.code()
            if status_code == grpc.StatusCode.DEADLINE_EXCEEDED:
                logging.error(
                    'request timed out for domain: {}'.format(domain_name))
            else:
                # re-raise
                logging.error('RPC Status: {} Details: {}'.format(
                    status_code.name,
                    e.details()))


if __name__ == '__main__':
    # Turn on logging
    logging.getLogger('').setLevel(logging.INFO)

    # Parse CLI arguments.
    parser = argparse.ArgumentParser(description='Starts up the DNS client.')
    parser.add_argument('--backend_port',
                        type=int,
                        help='The port your DNS Server is running on.')
    parser.add_argument('--backend_ip', default=DEFAULT_SERVER_IP,
                        help='The ip your DNS Server is running on.')
    parser.add_argument('--timeout', default=DEFAULT_TIMEOUT_SECONDS,
                        help='The timeout in seconds for DNS lookup requests.')
    parser.add_argument('--server_pem',
                        help='Path to the server pem file used to establish a '
                        'secure channel (otherwise the channel is insecure.')
    args = parser.parse_args()

    backend_port = args.backend_port

    pem = None
    if args.server_pem:
        with open(args.server_pem, 'rb') as f:
            pem = f.read()
        if not backend_port:
            backend_port = DEFAULT_SECURE_SERVER_PORT

    # If no backend_port yet set, use the default insecure port.
    if not backend_port:
        backend_port = DEFAULT_SERVER_PORT

    # Create the client object and test out some domains.
    client = DnsClient(args.backend_ip,
                       backend_port, args.timeout, pem)
    print(client.request_dns_lookup('walmart.com'))
    print(client.request_dns_lookup('facebook.com'))
