#! /usr/bin/env python3
import argparse
import logging
import grpc
import domain_lookup_pb2
import domain_lookup_pb2_grpc
import functools
import socketserver

logging.getLogger('').setLevel(logging.INFO)
DEFAULT_SERVER_IP = '127.0.0.1'
DEFAULT_SERVER_PORT = '8980'


class DnsClient:
    def __init__(self, dns_translator, server_ip, server_port):
        self.translator = dns_translator
        self.server_ip = server_ip
        self.server_port = server_port

        # The channel used to communicate with the DNS server.
        # lazily create
        self.channel = None
        # Tracks the various request
        self.request_id = 0
        # Stores mappings from request id -> futures object
        # TODO(kbaichoo): likely have this hold a bit more metadata
        # so it knows to respond to a particular DNS reuqest.
        self.outstanding_request = dict()

    def request_dns_lookup(self, domain_name):
        """
        Makes an RPC call to our DNS Server.

        Args:
         - domain_name: a string containing the name of the domain to lookup.

        Returns the resulting protobuf on success.
        """
        if not self.channel:
            # TODO(kbaichoo): set up a secure channel
            logging.info(
                'Connecting to backend on {}:{}'.format(
                    self.server_ip, self.server_port))
            self.channel = grpc.insecure_channel(
                '{}:{}'.format(self.server_ip, self.server_port))

        # Get the request ID
        request_id = self.request_id
        self.request_id += 1

        stub = domain_lookup_pb2_grpc.DomainLookupServiceStub(self.channel)
        request = domain_lookup_pb2.HostName(name=domain_name)
        cb = functools.partial(process_dns_response, self, request_id)
        future = stub.GetDomain.future(request)

        logging.info('Sending Request[ID={}] for domain: {}'.format(
            request_id, domain_name))
        # Add outstanding request to the map
        self.outstanding_request[request_id] = future
        future.add_done_callback(cb)

    def run_until_termination(self):
        """
        Set up a server for the browser to connect to,
        and run until termination.
        """
        # TODO(kbaichoo): implement this to keep running unless we see
        # an interrupt.
        import time
        time.sleep(100)


# TODO(kbaichoo): use partialmethod and have this be a method in the object.
# Currently a bit jank.
def process_dns_response(self, request_id, msg):
    """Handles the callback when the request is fulfilled."""
    logging.info('Recieved response for Request[ID={}]'.format(request_id))
    self.outstanding_request.pop(request_id)
    if msg.code() == grpc.StatusCode.OK:
        # Process the response, it was a success
        result = msg.result()
        pass

    # Error in the response.
    return None


if __name__ == '__main__':
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
    client.run_until_termination()
