#! /usr/bin/env python3
"""
Client for the P2P DomainLookupSerivce serivce.

Can use this as a standalone to make calls to the
P2P DomainLookupService's server or more likely as a library abstracting
the logic of communicating to the P2P DomainLookupService backend.

The client will create mutual TLS if both a root_certs and private_key are
specified.
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
                 timeout=DEFAULT_TIMEOUT_SECONDS, root_certs=None, pk=None):
        self.server_ip = server_ip
        self.server_port = server_port
        self.timeout = timeout
        self.root_certs = root_certs
        self.pk = pk

        # The channel used to communicate with the DNS server.
        logging.info(
            'Connecting to backend on {}:{}'.format(
                self.server_ip, self.server_port))
        server_tuple = '{}:{}'.format(self.server_ip, self.server_port)

        # The channels are thread safe.
        # Create a secure channel if the users specifies a root_cert.
        # If they specify a PK for themselves as well it'll be mutual TLS.
        if self.root_certs or self.pk:
            creds = grpc.ssl_channel_credentials(
                root_certificates=self.root_certs, private_key=self.pk,
                certificate_chain=self.root_certs)
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
    parser.add_argument('--root_cert',
                        help='Path to the root certificates used to establish'
                        ' a secure channel (otherwise the channel is insecure.'
                        )
    parser.add_argument('--private_key',
                        help='Path to client pk used in mutual TLS')
    args = parser.parse_args()

    backend_port = args.backend_port

    root_cert = None
    if args.root_cert:
        with open(args.root_cert, 'rb') as f:
            root_cert = f.read()
        if not backend_port:
            backend_port = DEFAULT_SECURE_SERVER_PORT

    # If no backend_port yet set, use the default insecure port.
    if not backend_port:
        backend_port = DEFAULT_SERVER_PORT

    # Read private key if specified.
    pk = None
    if args.private_key:
        with open(args.private_key, 'rb') as f:
            pk = f.read()

    # Create the client object and test out some domains.
    client = DnsClient(args.backend_ip,
                       backend_port, args.timeout, root_cert, pk)
    print(client.request_dns_lookup('walmart.com'))
    print(client.request_dns_lookup('facebook.com'))
