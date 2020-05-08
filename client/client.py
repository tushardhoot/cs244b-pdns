import logging

import grpc
import domain_lookup_pb2
import domain_lookup_pb2_grpc

logging.getLogger('').setLevel(logging.INFO)
SERVER_IP = '127.0.0.1'
SERVER_PORT = '8980'


class DnsClient:
    def __init__(self, dns_translator, server_ip, server_port):
        self.translator = dns_translator
        self.server_ip = server_ip
        self.server_port = server_port

        # The channel used to communicate with the DNS server.
        # lazily create
        self.channel = None

    def request_dns_lookup(self, domain_name):
        """
        Makes an RPC call to our DNS Server.

        Args:
         - domain_name: The name of the domain to lookup.

        Returns the resulting protobuf on success.
        """
        if not self.channel:
            # TODO(kbaichoo): set up a secure channel + don't hardcode
            self.channel = grpc.insecure_channel(
                '{}:{}'.format(self.server_ip, self.server_port))
        stub = domain_lookup_pb2_grpc.DomainLookupServiceStub(self.channel)
        request = domain_lookup_pb2.HostName(name=domain_name)
        return stub.GetDomain(request)


if __name__ == '__main__':
    # Launch the DNS intercept server.
    client = DnsClient(None, SERVER_IP, SERVER_PORT)
    print(client.request_dns_lookup('walmart.com'))
