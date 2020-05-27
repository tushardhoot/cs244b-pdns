#! /usr/bin/env python3
"""
Tests for the DNS wrapper module.
"""
# Bit of a hack to get directory structure working
import os
import sys
if os.getcwd().endswith('client'):
    sys.path.append('./client/')
else:
    sys.path.append('../client/')

from dnslib import DNSRecord
import dnswrapper
import unittest


class TestDnsWrapper(unittest.TestCase):

    def test_extract_query_pair(self):
        # Test query domain/record correctly extracted
        expected_domain = 'google.com'
        expected_qtype = 'AAAA'

        query = DNSRecord.question(expected_domain, qtype=expected_qtype)
        extracted_domain, record_type = dnswrapper.extract_query(query.pack())

        self.assertEqual(expected_domain, extracted_domain)
        self.assertEqual(expected_qtype, record_type)

    def test_generate_dns_packet_no_answer(self):
        # Test with no answer
        expected_domain = 'google.com'
        expected_qtype = 'A'

        query = DNSRecord.question(expected_domain, qtype=expected_qtype)
        query_pkt = query.pack()

        response = dnswrapper.generate_dns_packet(
            query_pkt, expected_domain, [])

        parsed_response = DNSRecord.parse(response)

        # No answers should be present
        self.assertEqual(parsed_response.header.a, 0)
        self.assertEqual(len(parsed_response.rr), 0)

    def test_generate_dns_packet_correct_answers(self):
        # Test with 'N' answers
        expected_domain = 'google.com'
        expected_qtype = 'A'
        expected_addresses = ['1.2.3.4', '5.6.7.8']

        query = DNSRecord.question(expected_domain, qtype=expected_qtype)
        query_pkt = query.pack()

        response = dnswrapper.generate_dns_packet(
            query_pkt, expected_domain, expected_addresses)

        parsed_response = DNSRecord.parse(response)

        # No answers should be present
        self.assertEqual(parsed_response.header.a, 2)
        self.assertEqual(len(parsed_response.rr), 2)

        # Check responses
        for i in range(len(expected_addresses)):
            self.assertEqual(
                str(parsed_response.rr[i].rdata), expected_addresses[i])


if __name__ == '__main__':
    unittest.main()
