#! /usr/bin/env python3
"""
Digs the 1M using the cisco umbrella list.
"""
import pydig

FILENAME = 'top-1m.csv'

urls = []
max_len = 0
lens = []
with open(FILENAME, 'r') as f:
    for l in f:
        url = l.strip().split(',')[-1]
        urls.append(url)
        max_len = max(max_len, len(url))
        lens.append(len(url))

lens = sorted(lens)
print('Max Length of a DNS hostname on list: {}'.format(max_len))
print('Median Length of a DNS hostname on list: {}'.format(lens[len(lens)//2]))
