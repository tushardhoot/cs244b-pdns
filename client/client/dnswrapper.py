#! /usr/bin/env python3
"""
Helper module used for interacting with DNS records.

Responsible for:
- Parsing packets and providing hostname, record_type
- Generating Responses (given ip address) for the packet
"""
import dnslib
from dnslib import DNSRecord, DNSHeader, RR, QTYPE
import logging
import binascii


logging.getLogger('').setLevel(logging.INFO)


def extract_query(pkt):
    """ Returns the hostname and record type of the record."""
    request = DNSRecord.parse(pkt)

    # Extract the hostname
    qname = request.q.qname
    hostname = str(qname)
    # Remove tailing period (artifact of the dnslib)
    if hostname[-1] == '.':
        hostname = hostname[:-1]

    # Record type
    qtype = QTYPE.get(request.q.qtype)

    return (hostname, qtype)


def generate_dns_packet(request_pkt, hostname, ip_addresses):
    """
    Generates a DNS Response populated with the provided information.

    Args:
    - request_pkt: The DNS request packet
    - hostname: Hostname being resolved
    - ip_addresses: list of ips addresses

    """
    request = DNSRecord.parse(request_pkt)
    record_type = request.q.qtype
    response = request.reply()

    # TODO(kbaichoo): add flexible ttl
    for ip_address in ip_addresses:
        # Get the class for resource data that corresponse to the request
        # and provide the IP address.
        rdata = dnslib.RDMAP.get(QTYPE.get(record_type), dnslib.RD)(ip_address)
        response.add_answer(
            RR(hostname, record_type, rdata=rdata, ttl=60))

    # Encode response
    return response.pack()


if __name__ == '__main__':
    # Example usage of this module.
    packet = binascii.unhexlify(
        b'd5ad818000010005000000000377777706676f6f676c6503636f6d0000010001c00c0005000100000005000803777777016cc010c02c0001000100000005000442f95b68c02c0001000100000005000442f95b63c02c0001000100000005000442f95b67c02c0001000100000005000442f95b93')
    print(extract_query(packet))

    generated_result = generate_dns_packet(
        packet, 'google.com', ['8.8.8.8', '1.1.1.1'])
    print(DNSRecord.parse(generated_result))
