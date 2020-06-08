#!/bin/bash

for i in {1..100}
do
	dig a $1 @$2 | grep -E -o "time: ([0-9]+)" | grep -E -o "([0-9]+)" >> $3_$4.txt
done

echo "Finished!"
